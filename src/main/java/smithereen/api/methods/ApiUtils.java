package smithereen.api.methods;

import java.util.BitSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiGroup;
import smithereen.api.model.ApiUser;
import smithereen.controllers.FriendsController;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.Group;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.UserPresence;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.board.BoardTopicsSortOrder;
import smithereen.model.friends.FriendshipStatus;
import smithereen.model.groups.GroupFeatureState;
import smithereen.model.photos.Photo;
import smithereen.util.XTEA;

class ApiUtils{
	public static List<ApiUser> getUsers(Collection<Integer> ids, ApplicationContext ctx, ApiCallContext actx){
		List<Integer> idList=switch(ids){
			case List<Integer> l -> l;
			default -> ids.stream().toList();
		};
		return getUsers(ctx.getUsersController().getUsersAsList(idList).stream().filter(Objects::nonNull).toList(), ctx, actx);
	}

	public static List<ApiUser> getUsers(List<User> userList, ApplicationContext ctx, ApiCallContext actx){
		EnumSet<ApiUser.Field> fields=actx.optCommaSeparatedStringSet("fields")
				.stream()
				.map(ApiUser.Field::valueOfApi)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(()->EnumSet.noneOf(ApiUser.Field.class)));
		Map<Integer, User> users=userList.stream().collect(Collectors.toMap(u->u.id, Function.identity(), (a, b)->b));
		Set<Integer> ids=users.keySet();
		Map<Integer, User> extraUsers;

		if(fields.contains(ApiUser.Field.RELATION)){
			Set<Integer> needExtraUsers=users.values().stream()
					.map(u->u.relationship!=null && u.relationship.canHavePartner() ? u.relationshipPartnerID : 0)
					.filter(id->id!=0)
					.collect(Collectors.toCollection(HashSet::new));
			extraUsers=new HashMap<>();
			for(int id:needExtraUsers){
				User u=users.get(id);
				if(u!=null)
					extraUsers.put(id, u);
			}
			needExtraUsers.removeAll(extraUsers.keySet());
			if(!needExtraUsers.isEmpty())
				extraUsers.putAll(ctx.getUsersController().getUsers(needExtraUsers));
		}else{
			extraUsers=Map.of();
		}

		Map<Integer, UserPresence> onlines;
		if(fields.contains(ApiUser.Field.ONLINE) || fields.contains(ApiUser.Field.LAST_SEEN)){
			if(fields.contains(ApiUser.Field.LAST_SEEN))
				onlines=ctx.getUsersController().getUserPresences(ids);
			else
				onlines=ctx.getUsersController().getUserPresencesOnlineOnly(ids);
		}else{
			onlines=Map.of();
		}

		Set<Integer> blockedIDs, blockingIDs;
		Map<Integer, Set<UserPrivacySettingKey>> allowedPrivacySettings;
		Map<Integer, Integer> mutualCounts;
		Map<Integer, FriendshipStatus> friendStatuses;
		Set<Integer> bookmarkedIDs, mutedIDs;
		Map<Integer, BitSet> friendLists;
		Map<Integer, Photo> profilePhotos;
		if(actx.self!=null){
			if(fields.contains(ApiUser.Field.BLOCKED))
				blockingIDs=ctx.getUsersController().getBlockingUsers(actx.self.user, ids);
			else
				blockingIDs=null;

			if(fields.contains(ApiUser.Field.BLOCKED_BY_ME))
				blockedIDs=ctx.getUsersController().getBlockedUsers(actx.self.user, ids);
			else
				blockedIDs=null;

			if(fields.contains(ApiUser.Field.CAN_POST) || fields.contains(ApiUser.Field.CAN_SEE_ALL_POSTS) || fields.contains(ApiUser.Field.CAN_WRITE_PRIVATE_MESSAGE) || fields.contains(ApiUser.Field.WALL_DEFAULT)){
				allowedPrivacySettings=new HashMap<>();
				EnumSet<UserPrivacySettingKey> keysToCheck=EnumSet.noneOf(UserPrivacySettingKey.class);
				if(fields.contains(ApiUser.Field.CAN_POST))
					keysToCheck.add(UserPrivacySettingKey.WALL_POSTING);
				if(fields.contains(ApiUser.Field.CAN_SEE_ALL_POSTS) || fields.contains(ApiUser.Field.WALL_DEFAULT))
					keysToCheck.add(UserPrivacySettingKey.WALL_OTHERS_POSTS);
				if(fields.contains(ApiUser.Field.CAN_WRITE_PRIVATE_MESSAGE))
					keysToCheck.add(UserPrivacySettingKey.PRIVATE_MESSAGES);

				for(User user:users.values()){
					EnumSet<UserPrivacySettingKey> keys=EnumSet.noneOf(UserPrivacySettingKey.class);
					for(UserPrivacySettingKey key:keysToCheck){
						if((key==UserPrivacySettingKey.WALL_POSTING || key==UserPrivacySettingKey.WALL_OTHERS_POSTS) && !user.hasWall())
							continue;
						if(ctx.getPrivacyController().checkUserPrivacy(actx.self.user, user, key))
							keys.add(key);
					}
					allowedPrivacySettings.put(user.id, keys);
				}
			}else{
				allowedPrivacySettings=null;
			}

			if(fields.contains(ApiUser.Field.MUTUAL_COUNT)){
				mutualCounts=ctx.getFriendsController().getMutualFriendsCounts(actx.self.user, ids);
			}else{
				mutualCounts=null;
			}

			if(fields.contains(ApiUser.Field.FRIEND_STATUS) || fields.contains(ApiUser.Field.IS_FRIEND)){
				friendStatuses=ctx.getFriendsController().getSimpleFriendshipStatuses(actx.self.user, ids);
			}else{
				friendStatuses=null;
			}

			if(!actx.hasPermission(ClientAppPermission.LIKES_READ))
				fields.remove(ApiUser.Field.IS_FAVORITE);
			if(fields.contains(ApiUser.Field.IS_FAVORITE)){
				bookmarkedIDs=ctx.getBookmarksController().filterUserIDsByBookmarked(actx.self.user, ids);
			}else{
				bookmarkedIDs=null;
			}

			if(fields.contains(ApiUser.Field.LISTS)){
				friendLists=ctx.getFriendsController().getFriendListsForUsers(actx.hasPermission(ClientAppPermission.FRIENDS_READ) ? actx.self.user : null, actx.self.user, ids);
			}else{
				friendLists=null;
			}

			if(fields.contains(ApiUser.Field.IS_HIDDEN_FROM_FEED)){
				mutedIDs=ctx.getFriendsController().getMutedUserIDs(actx.self.user, ids);
			}else{
				mutedIDs=null;
			}
		}else{
			fields.removeAll(ApiUser.FIELDS_THAT_REQUIRE_ACCOUNT);
			blockedIDs=blockingIDs=null;
			allowedPrivacySettings=null;
			mutualCounts=null;
			friendStatuses=null;
			bookmarkedIDs=null;
			friendLists=null;
			mutedIDs=null;
		}

		if(fields.contains(ApiUser.Field.PHOTO_ID) || fields.contains(ApiUser.Field.CROP_PHOTO)){
			profilePhotos=ctx.getPhotosController().getUserProfilePhotos(users.values());
		}else{
			profilePhotos=null;
		}

		List<ApiUser> result=userList.stream()
				.map(u->new ApiUser(actx, u, fields, extraUsers, onlines, blockingIDs, blockedIDs, allowedPrivacySettings, mutualCounts, friendStatuses, bookmarkedIDs, friendLists, mutedIDs, profilePhotos))
				.toList();
		if(fields.contains(ApiUser.Field.COUNTERS) && result.size()==1){
			ApiUser au=result.getFirst();
			User user=users.get(au.id);
			User self=actx.self==null ? null : actx.self.user;
			au.counters=new ApiUser.Counters(
					ctx.getPhotosController().getAllAlbums(user, self, false, false).size(),
					ctx.getPhotosController().getAllPhotosCount(user, self),
					user.getFriendsCount(),
					ctx.getGroupsController().getUserGroups(user, self, 0, 1).total,
					ctx.getFriendsController().getFriends(user, 0, 1, FriendsController.SortOrder.ID_ASCENDING, true, 0).total,
					self==null || self.id==user.id ? 0 : ctx.getFriendsController().getMutualFriendsCount(self, user),
					ctx.getPrivacyController().checkUserPrivacy(self, user, UserPrivacySettingKey.PHOTO_TAG_LIST) ? ctx.getPhotosController().getUserTaggedPhotosIgnoringPrivacy(user, 0, 1).total : 0,
					user.getFollowersCount(),
					user.getFollowingCount()
			);
		}
		if(fields.contains(ApiUser.Field.TIMEZONE) && actx.self!=null){
			for(ApiUser u:result){
				if(u.id==actx.self.user.id){
					u.timezone=actx.self.prefs.timeZone.toString();
					break;
				}
			}
		}
		return result;
	}

	public static User getUserOrSelf(ApplicationContext ctx, ApiCallContext actx, String paramName){
		if(actx.hasParam(paramName)){
			try{
				return ctx.getUsersController().getUserOrThrow(actx.requireParamIntPositive(paramName));
			}catch(ObjectNotFoundException x){
				throw actx.error(ApiErrorType.NOT_FOUND, "user with this ID does not exist");
			}
		}else if(actx.self!=null){
			return actx.self.user;
		}else{
			throw actx.paramError(paramName+" is required when this method is called without a token");
		}
	}

	public static List<ApiGroup> getGroups(Collection<Integer> ids, ApplicationContext ctx, ApiCallContext actx){
		List<Integer> idList=switch(ids){
			case List<Integer> l -> l;
			default -> ids.stream().toList();
		};
		return getGroups(ctx.getGroupsController().getGroupsByIdAsList(idList).stream().filter(Objects::nonNull).toList(), ctx, actx);
	}

	public static List<ApiGroup> getGroups(List<Group> groupList, ApplicationContext ctx, ApiCallContext actx){
		EnumSet<ApiGroup.Field> fields=actx.optCommaSeparatedStringSet("fields")
				.stream()
				.map(ApiGroup.Field::valueOfApi)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(()->EnumSet.noneOf(ApiGroup.Field.class)));
		Set<Integer> ids=groupList.stream().map(g->g.id).collect(Collectors.toSet());

		Map<Integer, Group.AdminLevel> adminLevels;
		Map<Integer, Group.MembershipState> memberStates;
		Set<Integer> canPost;
		Set<Integer> canCreateTopic;
		Set<Integer> favoritedGroups;
		Map<Integer, Photo> profilePhotos;

		if(fields.contains(ApiGroup.Field.PHOTO_ID) || fields.contains(ApiGroup.Field.CROP_PHOTO))
			profilePhotos=ctx.getPhotosController().getGroupProfilePhotos(groupList);
		else
			profilePhotos=null;

		if(actx.self!=null){
			if(fields.contains(ApiGroup.Field.ADMIN_LEVEL) || fields.contains(ApiGroup.Field.IS_ADMIN) || fields.contains(ApiGroup.Field.CAN_POST) || fields.contains(ApiGroup.Field.CAN_CREATE_TOPIC)){
				adminLevels=ctx.getGroupsController().getMemberAdminLevels(groupList, actx.self.user);
			}else{
				adminLevels=null;
			}

			boolean thereArePrivateGroups=false;
			for(Group g:groupList){
				if(g.accessType==Group.AccessType.PRIVATE){
					thereArePrivateGroups=true;
					break;
				}
			}

			if(fields.contains(ApiGroup.Field.MEMBER_STATUS) || fields.contains(ApiGroup.Field.IS_MEMBER) || fields.contains(ApiGroup.Field.CAN_POST) || fields.contains(ApiGroup.Field.CAN_CREATE_TOPIC) || thereArePrivateGroups){
				memberStates=ctx.getGroupsController().getUserMembershipStates(groupList, actx.self.user);
			}else{
				memberStates=null;
			}

			if(!actx.hasPermission(ClientAppPermission.LIKES_READ))
				fields.remove(ApiGroup.Field.IS_FAVORITE);
			if(fields.contains(ApiGroup.Field.IS_FAVORITE)){
				favoritedGroups=ctx.getBookmarksController().filterGroupIDsByBookmarked(actx.self.user, ids);
			}else{
				favoritedGroups=null;
			}

			if(fields.contains(ApiGroup.Field.CAN_POST)){
				canPost=new HashSet<>();
				for(Group g:groupList){
					Group.MembershipState state=memberStates.get(g.id);
					if(g.accessType==Group.AccessType.OPEN || state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER){
						if(g.wallState==GroupFeatureState.ENABLED_OPEN || (g.wallState==GroupFeatureState.ENABLED_RESTRICTED && adminLevels.get(g.id).isAtLeast(Group.AdminLevel.MODERATOR)))
							canPost.add(g.id);
					}
				}
			}else{
				canPost=null;
			}

			if(fields.contains(ApiGroup.Field.CAN_CREATE_TOPIC)){
				canCreateTopic=new HashSet<>();
				for(Group g:groupList){
					Group.MembershipState state=memberStates.get(g.id);
					if(g.accessType==Group.AccessType.OPEN || state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER){
						if(g.boardState==GroupFeatureState.ENABLED_OPEN || (g.boardState==GroupFeatureState.ENABLED_RESTRICTED && adminLevels.get(g.id).isAtLeast(Group.AdminLevel.MODERATOR)))
							canCreateTopic.add(g.id);
					}
				}
			}else{
				canCreateTopic=null;
			}
		}else{
			fields.removeAll(ApiGroup.FIELDS_THAT_REQUIRE_ACCOUNT);
			adminLevels=null;
			memberStates=null;
			canPost=canCreateTopic=favoritedGroups=null;
		}

		List<ApiGroup> result=groupList.stream()
				.map(g->new ApiGroup(actx, g, fields, adminLevels, memberStates, canPost, canCreateTopic, favoritedGroups, profilePhotos))
				.toList();

		if(result.size()==1){
			ApiGroup ag=result.getFirst();
			Group g=groupList.getFirst();
			if(fields.contains(ApiGroup.Field.COUNTERS)){
				ag.counters=new ApiGroup.Counters(
						ctx.getPhotosController().getAllPhotosCount(g, actx.self==null ? null : actx.self.user),
						ctx.getPhotosController().getAllAlbums(g, actx.self==null ? null : actx.self.user, false, false).size(),
						ctx.getBoardController().getTopicsIgnoringPrivacy(g, 0, 1, BoardTopicsSortOrder.UPDATED_DESC).total
				);
			}
			if(fields.contains(ApiGroup.Field.MANAGEMENT)){
				ag.management=ctx.getGroupsController().getAdmins(g)
						.stream()
						.map(ga->new ApiGroup.Manager(ga.userID, ga.title))
						.toList();
			}
			if(fields.contains(ApiGroup.Field.LINKS)){
				ag.links=ctx.getGroupsController().getLinks(g)
						.stream()
						.map(l->{
							SizedImage img=l.getImage();
							String objID=null;
							String objType=switch(l.object.type()){
								case USER -> {
									objID=l.object.id()+"";
									yield "user";
								}
								case GROUP -> {
									objID=l.object.id()+"";
									yield "group";
								}
								case POST -> {
									objID=l.object.id()+"";
									yield "post";
								}
								case PHOTO ->{
									objID=XTEA.encodeObjectID(l.object.id(), ObfuscatedObjectIDType.PHOTO);
									yield "photo";
								}
								case PHOTO_ALBUM ->{
									objID=XTEA.encodeObjectID(l.object.id(), ObfuscatedObjectIDType.PHOTO_ALBUM);
									yield "photo_album";
								}
								case BOARD_TOPIC ->{
									objID=XTEA.encodeObjectID(l.object.id(), ObfuscatedObjectIDType.BOARD_TOPIC);
									yield "topic";
								}
								default -> null;
							};
							return new ApiGroup.Link(l.id, l.url.toString(), l.title, l.getDescription(),
									img==null ? null : img.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_SMALL, actx.imageFormat).toString(),
									img==null ? null : img.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_MEDIUM, actx.imageFormat).toString(),
									img==null ? null : img.getUriForSizeAndFormat(SizedImage.Type.AVA_SQUARE_LARGE, actx.imageFormat).toString(),
									objType, objID);
						})
						.toList();
			}
		}

		return result;
	}
}
