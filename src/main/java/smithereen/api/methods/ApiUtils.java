package smithereen.api.methods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiComment;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiGroup;
import smithereen.api.model.ApiUser;
import smithereen.api.model.ApiWallPost;
import smithereen.controllers.FriendsController;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.CommentViewType;
import smithereen.model.Group;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.Poll;
import smithereen.model.PostSource;
import smithereen.model.SizedImage;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.UserPresence;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.attachments.Attachment;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.board.BoardTopicsSortOrder;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.comments.CommentableObjectType;
import smithereen.model.friends.FriendshipStatus;
import smithereen.model.groups.GroupFeatureState;
import smithereen.model.media.MediaFileUploadPurpose;
import smithereen.model.media.MediaFileUploadTokens;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.text.FormattedTextFormat;
import smithereen.util.CryptoUtils;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;
import spark.utils.StringUtils;

public class ApiUtils{
	public static final byte[] UPLOAD_KEY=CryptoUtils.randomBytes(16);
	public static final HashMap<Integer, Long> uploadUrlIDs=new HashMap<>();
	public static final AtomicInteger lastUploadUrlID=new AtomicInteger();

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
					ctx.getPrivacyController().checkUserPrivacy(self, user, UserPrivacySettingKey.PHOTO_TAG_LIST) ? ctx.getPhotosController().getUserTaggedPhotosIgnoringPrivacy(user, 0, 1, false).total : 0,
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

	public static Actor getOwnerOrSelf(ApplicationContext ctx, ApiCallContext actx, String paramName){
		if(actx.hasParam(paramName)){
			int oid=actx.requireParamIntNonZero(paramName);
			try{
				return oid>0 ? ctx.getUsersController().getUserOrThrow(oid) : ctx.getGroupsController().getGroupOrThrow(-oid);
			}catch(ObjectNotFoundException x){
				throw actx.error(ApiErrorType.NOT_FOUND, (oid>0 ? "user" : "group")+" with this ID does not exist");
			}
		}else if(actx.self!=null){
			return actx.self.user;
		}else{
			throw actx.paramError(paramName+" is required when this method is called without a token");
		}
	}

	public static User getUser(ApplicationContext ctx, ApiCallContext actx, String paramName){
		try{
			return ctx.getUsersController().getUserOrThrow(actx.requireParamIntPositive(paramName));
		}catch(ObjectNotFoundException x){
			throw actx.error(ApiErrorType.NOT_FOUND, "user with this ID does not exist");
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
							String objType;
							String title=l.title;
							if(l.object==null){
								objType=null;
							}else{
								objType=switch(l.object.type()){
									case USER -> {
										objID=l.object.id()+"";
										try{
											title=ctx.getUsersController().getUserOrThrow((int)l.object.id()).getFullName();
										}catch(ObjectNotFoundException x){
											title="DELETED";
										}
										yield "user";
									}
									case GROUP -> {
										objID=l.object.id()+"";
										try{
											title=ctx.getGroupsController().getGroupOrThrow((int)l.object.id()).name;
										}catch(ObjectNotFoundException x){
											title="DELETED";
										}
										yield "group";
									}
									case POST -> {
										objID=l.object.id()+"";
										yield "post";
									}
									case PHOTO -> {
										objID=XTEA.encodeObjectID(l.object.id(), ObfuscatedObjectIDType.PHOTO);
										yield "photo";
									}
									case PHOTO_ALBUM -> {
										objID=XTEA.encodeObjectID(l.object.id(), ObfuscatedObjectIDType.PHOTO_ALBUM);
										yield "photo_album";
									}
									case BOARD_TOPIC -> {
										objID=XTEA.encodeObjectID(l.object.id(), ObfuscatedObjectIDType.BOARD_TOPIC);
										yield "topic";
									}
									default -> null;
								};
							}
							return new ApiGroup.Link(l.id, l.url.toString(), title, l.getDescription(),
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

	public static List<ApiWallPost> getPosts(List<PostViewModel> posts, ApplicationContext ctx, ApiCallContext actx, boolean needLikes, boolean needReposts, boolean enforcePermission){
		User self=(!enforcePermission && actx.self!=null) || actx.hasPermission(ClientAppPermission.WALL_READ) ? actx.self.user : null;
		if(needReposts){
			int repostDepth=actx.optParamIntPositive("repost_history_depth", 2);
			ctx.getWallController().populateReposts(self, posts, repostDepth);
		}

		Map<Integer, UserInteractions> interactions;
		if(needLikes)
			interactions=ctx.getWallController().getUserInteractions(posts, self);
		else
			interactions=Map.of();

		Map<Integer, List<Integer>> pinnedIDs=new HashMap<>();
		Set<Long> needPhotos=new HashSet<>();
		for(PostViewModel p:posts){
			PostViewModel post=p;
			do{
				if(post.post.ownerID>0 && !pinnedIDs.containsKey(post.post.ownerID))
					pinnedIDs.put(post.post.ownerID, ctx.getWallController().getPinnedPostIDs(post.post.ownerID));
				List<Attachment> attachments=post.post.getProcessedAttachments();
				for(Attachment att:attachments){
					if(att instanceof PhotoAttachment pa && pa.photoID!=0){
						needPhotos.add(pa.photoID);
					}
				}
				post=post.repost==null ? null : post.repost.post();
			}while(post!=null);
		}
		Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
		return posts.stream().map(p->new ApiWallPost(p, actx, interactions, pinnedIDs, photos)).toList();
	}

	public static List<ApiComment> getComments(List<CommentViewModel> comments, ApplicationContext ctx, ApiCallContext actx, boolean needLikes){
		User self=actx.self!=null ? actx.self.user : null;

		Map<Long, UserInteractions> interactions;
		if(needLikes)
			interactions=ctx.getUserInteractionsController().getUserInteractions(comments.stream().map(cvm->cvm.post).toList(), self);
		else
			interactions=Map.of();

		Set<Long> needPhotos=new HashSet<>();
		for(CommentViewModel c:comments){
			List<Attachment> attachments=c.post.getProcessedAttachments();
			for(Attachment att:attachments){
				if(att instanceof PhotoAttachment pa && pa.photoID!=0){
					needPhotos.add(pa.photoID);
				}
			}
		}
		Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
		return comments.stream().map(c->new ApiComment(c, actx, interactions, photos)).toList();
	}

	public static Object getObjectComments(ApplicationContext ctx, ApiCallContext actx, CommentableContentObject parent){
		CommentViewType viewType=switch(actx.optParamString("view_type")){
			case "threaded" -> CommentViewType.THREADED;
			case "two_level" -> CommentViewType.TWO_LEVEL;
			case "flat" -> CommentViewType.FLAT;
			case null, default -> actx.self==null ? CommentViewType.FLAT : actx.self.prefs.commentViewType;
		};
		int offset=actx.getOffset();
		int count=actx.getCount(20, 100);
		int secondaryCount=Math.min(100, actx.optParamIntPositive("secondary_count", 20));
		String commentID=actx.optParamString("comment_id");
		boolean needLikes=actx.booleanParam("need_likes");

		List<Long> replyKey;
		if(commentID!=null){
			Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(commentID, ObfuscatedObjectIDType.COMMENT));
			if(!comment.parentObjectID.equals(parent.getCommentParentID()))
				throw actx.error(ApiErrorType.NOT_FOUND);
			if(viewType==CommentViewType.TWO_LEVEL)
				viewType=CommentViewType.FLAT;
			replyKey=comment.getReplyKeyForReplies();
		}else{
			replyKey=List.of();
		}

		PaginatedList<CommentViewModel> comments=ctx.getCommentsController().getComments(parent, replyKey, offset, count, secondaryCount, viewType);

		record CommentsResponse(int count, List<ApiComment> items, String viewType, List<ApiUser> profiles, List<ApiGroup> groups){}

		List<ApiComment> apiComments=getComments(comments.list, ctx, actx, needLikes);
		if(actx.booleanParam("extended")){
			HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
			CommentViewModel.collectUserIDs(comments.list, needUsers);
			return new CommentsResponse(comments.total, apiComments, viewType.name().toLowerCase(), ApiUtils.getUsers(needUsers, ctx, actx), ApiUtils.getGroups(needGroups, ctx, actx));
		}
		return new CommentsResponse(comments.total, apiComments, viewType.name().toLowerCase(), null, null);
	}

	public static Object createComment(ApplicationContext ctx, ApiCallContext actx, CommentableContentObject parent){
		Comment replyTo;
		if(actx.hasParam("reply_to_comment")){
			Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(actx.requireParamString("reply_to_comment"), ObfuscatedObjectIDType.COMMENT));
			if(!comment.parentObjectID.equals(parent.getCommentParentID()))
				throw new ObjectNotFoundException();
			replyTo=comment;
		}else{
			replyTo=null;
		}
		return createOrUpdateComment(ctx, actx, parent, replyTo, null);
	}

	public static Object editComment(ApplicationContext ctx, ApiCallContext actx, Comment comment){
		if(!actx.permissions.canEditPost(comment))
			throw actx.error(ApiErrorType.ACCESS_DENIED, "no access to edit this comment");
		return createOrUpdateComment(ctx, actx, null, null, comment);
	}

	private static Object createOrUpdateComment(ApplicationContext ctx, ApiCallContext actx, CommentableContentObject parent, Comment replyTo, Comment edit){
		String message=actx.optParamString("message");
		String cw=actx.optParamString("content_warning");
		FormattedTextFormat textFormat=getTextFormat(actx);

		InputAttachments attachments=parseAttachments(ctx, actx, true, false);
		if(StringUtils.isEmpty(message) && attachments.ids.isEmpty())
			throw actx.paramError("both message and attachments are undefined");

		if(edit!=null){
			ctx.getCommentsController().editComment(actx.self.user, edit, message, textFormat, cw, attachments.ids, attachments.altTexts);
			return edit.getIDString();
		}

		String guid=actx.optParamString("guid");
		if(StringUtils.isNotEmpty(guid)){
			guid=actx.token.getEncodedID()+"|"+guid;
		}
		return ctx.getCommentsController().createComment(actx.self.user, parent, replyTo, message, textFormat, cw, attachments.ids, attachments.altTexts, guid).getIDString();
	}

	public static Object getCommentEditSource(ApplicationContext ctx, ApiCallContext actx, CommentableObjectType expectedType){
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(actx.requireParamString("comment_id"), ObfuscatedObjectIDType.COMMENT));
		if(comment.parentObjectID.type()!=expectedType)
			throw new ObjectNotFoundException();
		if(!actx.permissions.canEditPost(comment))
			throw actx.error(ApiErrorType.ACCESS_DENIED, "no access to edit this comment");
		PostSource source=ctx.getCommentsController().getCommentSource(comment);
		List<Map<String, Object>> attachments=new ArrayList<>();
		for(Attachment att:comment.getProcessedAttachments()){
			if(att instanceof PhotoAttachment pa){
				if(pa.photoID!=0){
					attachments.add(Map.of(
							"type", "photo",
							"photo_id", XTEA.encodeObjectID(pa.photoID, ObfuscatedObjectIDType.PHOTO)
					));
				}else if(pa.image instanceof LocalImage li && li.fileRecord!=null){
					Map<String, Object> sa=new HashMap<>();
					sa.put("type", "image");
					sa.put("file_id", XTEA.encodeObjectID(li.fileID, ObfuscatedObjectIDType.MEDIA_FILE));
					sa.put("file_hash", MediaFileUploadTokens.getToken(li.fileRecord.id(), MediaFileUploadPurpose.ATTACHMENT, actx.self.user.id));
					sa.put("text", li.name);
					attachments.add(sa);
				}
			}
		}

		record SourceResponse(String text, String format, List<Map<String, Object>> attachments){}
		return new SourceResponse(source.text(), source.format().name().toLowerCase(), attachments);
	}

	public static @NotNull InputAttachments parseAttachments(ApplicationContext ctx, ApiCallContext actx, boolean isReply, boolean allowPoll){
		JsonArray attachments=actx.optParamJsonArray("attachments");
		Poll poll=null;
		List<String> attachmentIDs;
		Map<String, String> attachmentAltTexts;
		if(attachments!=null){
			attachmentIDs=new ArrayList<>();
			attachmentAltTexts=new HashMap<>();
			int i=0;
			for(JsonElement el:attachments){
				if(!(el instanceof JsonObject obj))
					throw actx.paramError("attachments["+i+"] is not an object");
				if(!(obj.get("type") instanceof JsonPrimitive jType && jType.isString()))
					throw actx.paramError("attachments["+i+"].type is not a string");
				String type=jType.getAsString();
				switch(type){
					case "image" -> {
						if(!(obj.get("file_id") instanceof JsonPrimitive jId))
							throw actx.paramError("attachments["+i+"].file_id is undefined");
						if(!(obj.get("file_hash") instanceof JsonPrimitive jHash))
							throw actx.paramError("attachments["+i+"].file_hash is undefined");

						String id=jId.getAsString()+":"+jHash.getAsString();
						attachmentIDs.add(id);

						if(obj.get("text") instanceof JsonPrimitive jText && jText.isString())
							attachmentAltTexts.put(id, jText.getAsString());
					}
					case "photo" -> {
						if(!(obj.get("photo_id") instanceof JsonPrimitive jId))
							throw actx.paramError("attachments["+i+"].photo_id is undefined");
						String photoID=jId.getAsString();
						attachmentIDs.add("photo:"+photoID);
					}
					case "poll" -> {
						if(allowPoll){
							if(!(obj.get("poll_id") instanceof JsonPrimitive jId))
								throw actx.paramError("attachments["+i+"].poll_id is undefined");
							if(poll!=null)
								throw actx.paramError("can't attach more than one poll");
							int id=jId.getAsInt();
							poll=ctx.getWallController().getPollByID(id);
						}else{
							throw actx.paramError("attachments["+i+"].type must be one of image, photo");
						}
					}
					default -> throw actx.paramError("attachments["+i+"].type must be one of image, photo"+(isReply ? "" : ", poll"));
				}
				i++;
				if(i==(isReply ? 2 : 10))
					break;
			}
		}else{
			attachmentIDs=List.of();
			attachmentAltTexts=Map.of();
		}
		return new InputAttachments(attachmentIDs, attachmentAltTexts, poll);
	}

	public static String getUploadURL(ApiCallContext actx, String path, Map<String, Object> extraParams){
		String data=new JsonObjectBuilder()
				.add("id", actx.self.id)
				.add("ct", System.currentTimeMillis()/1000L)
				.add("s", ApiUtils.lastUploadUrlID.incrementAndGet())
				.addAll(extraParams)
				.build()
				.toString();
		return UriBuilder.local()
				.path("api", path)
				.queryParam("d", Base64.getUrlEncoder().withoutPadding().encodeToString(CryptoUtils.aesGcmEncrypt(data.getBytes(StandardCharsets.UTF_8), ApiUtils.UPLOAD_KEY)))
				.build()
				.toString();
	}

	public static void removeOldUploadUrlIDs(){
		synchronized(uploadUrlIDs){
			long now=System.currentTimeMillis();
			uploadUrlIDs.values().removeIf(v->now-v>60_000);
		}
	}

	public static @NotNull FormattedTextFormat getTextFormat(@NotNull ApiCallContext actx){
		return switch(actx.optParamString("text_format")){
			case "markdown" -> FormattedTextFormat.MARKDOWN;
			case "html" -> FormattedTextFormat.HTML;
			case "plain" -> FormattedTextFormat.PLAIN;
			case null -> actx.self.prefs.textFormat;
			default -> throw actx.paramError("text_format must be one of markdown, html, plain");
		};
	}

	public record InputAttachments(@NotNull List<String> ids, @NotNull Map<String, String> altTexts, @Nullable Poll poll){
	}
}
