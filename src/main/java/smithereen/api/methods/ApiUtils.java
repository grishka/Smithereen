package smithereen.api.methods;

import java.util.BitSet;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiUser;
import smithereen.controllers.FriendsController;
import smithereen.model.User;
import smithereen.model.UserPresence;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.friends.FriendshipStatus;
import smithereen.model.photos.Photo;

class ApiUtils{
	public static List<ApiUser> getUsers(Set<Integer> ids, ApplicationContext ctx, ApiCallContext actx){
		EnumSet<ApiUser.Field> fields=actx.optCommaSeparatedStringSet("fields")
				.stream()
				.map(ApiUser.Field::valueOfApi)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(()->EnumSet.noneOf(ApiUser.Field.class)));
		Map<Integer, User> users=ctx.getUsersController().getUsers(ids);
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

			if(fields.contains(ApiUser.Field.IS_FAVORITE)){
				bookmarkedIDs=ctx.getBookmarksController().filterUserIDsByBookmarked(actx.self.user, ids);
			}else{
				bookmarkedIDs=null;
			}

			if(fields.contains(ApiUser.Field.LISTS)){
				friendLists=ctx.getFriendsController().getFriendListsForUsers(actx.self.user, actx.self.user, ids);
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

		List<ApiUser> result=users.values().stream().map(u->new ApiUser(actx, u, fields, extraUsers, onlines, blockingIDs, blockedIDs, allowedPrivacySettings, mutualCounts, friendStatuses, bookmarkedIDs, friendLists, mutedIDs, profilePhotos)).toList();
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
}
