package smithereen.api.methods;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiBoardTopic;
import smithereen.api.model.ApiComment;
import smithereen.api.model.ApiGroup;
import smithereen.api.model.ApiPhoto;
import smithereen.api.model.ApiUser;
import smithereen.api.model.ApiWallPost;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableObjectType;
import smithereen.model.notifications.GroupedNotification;
import smithereen.model.notifications.Notification;
import smithereen.model.notifications.NotificationWrapper;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.util.XTEA;

public class NotificationsMethods{
	private static final int MAX_USERS_PER_GROUPED=10;

	public static Object get(ApplicationContext ctx, ApiCallContext actx){
		PaginatedList<NotificationWrapper> notifications=ctx.getNotificationsController().getNotifications(actx.self, actx.getOffset(), actx.getCount(50, 100), actx.optParamIntPositive("max_id", Integer.MAX_VALUE));

		record UserIDs(int count, @NotNull List<Integer> items){
		}
		record Feedback(
				@NotNull String type,
				@Nullable Integer wallPostId,
				@Nullable Integer wallCommentId,
				@Nullable String photoCommentId,
				@Nullable String boardCommentId
		){
		}
		record NotificationObject(
				@NotNull String type,
				@Nullable Integer wallPostId,
				@Nullable Integer wallCommentId,
				@Nullable String commentId,
				@Nullable String photoId
		){
		}
		record Parent(
				@NotNull String type,
				@Nullable Integer wallPostId,
				@Nullable String photoId,
				@Nullable String boardTopicId
		){
		}
		record ApiNotification(
				int id,
				@NotNull String type,
				@Nullable UserIDs userIds,
				@Nullable Integer userId,
				long date,
				@Nullable Feedback feedback,
				@Nullable NotificationObject object,
				@Nullable Parent parent
		){
		}
		record NotificationsResponse(
				@NotNull List<ApiNotification> items,
				@NotNull List<ApiWallPost> wallPosts,
				@NotNull List<ApiWallPost> wallComments,
				@NotNull List<ApiComment> photoComments,
				@NotNull List<ApiComment> boardComments,
				@NotNull List<ApiPhoto> photos,
				@NotNull List<ApiBoardTopic> boardTopics,
				@NotNull List<ApiUser> profiles,
				@NotNull List<ApiGroup> groups,
				int lastViewed
		){
		}

		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>(), needPosts=new HashSet<>();
		HashSet<Long> needPhotos=new HashSet<>(), needComments=new HashSet<>(), needTopics=new HashSet<>();
		ArrayList<ApiNotification> apiNotifications=new ArrayList<>();
		for(NotificationWrapper nw:notifications.list){
			switch(nw){
				case Notification n -> needUsers.add(n.actorID);
				case GroupedNotification gn -> gn.notifications.stream().limit(MAX_USERS_PER_GROUPED).map(n->n.actorID).forEach(needUsers::add);
			}

			Notification n=nw.getLatestNotification();
			switch(n.objectType){
				case null -> {}
				case POST -> needPosts.add((int) n.objectID);
				case PHOTO -> needPhotos.add(n.objectID);
				case COMMENT -> needComments.add(n.objectID);
				case BOARD_TOPIC -> needTopics.add(n.objectID);
			}
			switch(n.relatedObjectType){
				case null -> {}
				case POST -> needPosts.add((int) n.relatedObjectID);
				case PHOTO -> needPhotos.add(n.relatedObjectID);
				case COMMENT -> needComments.add(n.relatedObjectID);
				case BOARD_TOPIC -> needTopics.add(n.relatedObjectID);
			}
		}

		Map<Long, Comment> comments=ctx.getCommentsController().getCommentsIgnoringPrivacy(needComments);
		HashSet<Long> needExtraComments=new HashSet<>();
		for(Comment c:comments.values()){
			switch(c.parentObjectID.type()){
				case PHOTO -> needPhotos.add(c.parentObjectID.id());
				case BOARD_TOPIC -> needTopics.add(c.parentObjectID.id());
			}
			if(c.getReplyLevel()>0 && !comments.containsKey(c.replyKey.getLast()))
				needExtraComments.add(c.replyKey.getLast());
		}

		if(!needExtraComments.isEmpty()){
			comments=new HashMap<>(comments);
			comments.putAll(ctx.getCommentsController().getCommentsIgnoringPrivacy(needExtraComments));
		}

		Map<Integer, Post> posts=ctx.getWallController().getPosts(needPosts);
		HashSet<Integer> needExtraPosts=new HashSet<>();
		for(Post p:posts.values()){
			if(p.getReplyLevel()>0 && !posts.containsKey(p.replyKey.getLast()))
				needExtraPosts.add(p.replyKey.getLast());
		}
		if(!needExtraPosts.isEmpty()){
			posts=new HashMap<>(posts);
			posts.putAll(ctx.getWallController().getPosts(needExtraPosts));
		}

		Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
		Map<Long, BoardTopic> topics=ctx.getBoardController().getTopicsIgnoringPrivacy(needTopics);

		for(Post p:posts.values()){
			needUsers.add(p.authorID);
			if(p.ownerID>0)
				needUsers.add(p.ownerID);
			else
				needGroups.add(-p.ownerID);
		}

		for(Comment c:comments.values()){
			needUsers.add(c.authorID);
			if(c.ownerID>0)
				needUsers.add(c.ownerID);
			else
				needGroups.add(-c.ownerID);
		}

		for(Photo p:photos.values()){
			needUsers.add(p.authorID);
			if(p.ownerID>0)
				needUsers.add(p.ownerID);
			else
				needGroups.add(-p.ownerID);
		}

		for(BoardTopic t:topics.values()){
			needGroups.add(t.groupID);
			needUsers.add(t.authorID);
		}

		for(NotificationWrapper n:notifications.list){
			Notification latest=n.getLatestNotification();
			UserIDs userIDs;
			Integer userID;
			if(latest.type.canBeGrouped()){
				userIDs=switch(n){
					case GroupedNotification gn -> new UserIDs(gn.notifications.size(), gn.notifications.stream().limit(MAX_USERS_PER_GROUPED).map(nn->nn.actorID).toList());
					case Notification nn -> new UserIDs(1, List.of(nn.actorID));
				};
				userID=null;
			}else{
				userIDs=null;
				userID=latest.actorID;
			}
			String type=switch(latest.type){
				case REPLY -> {
					if(latest.objectType==Notification.ObjectType.COMMENT){
						Comment comment=comments.get(latest.objectID);
						yield comment!=null && comment.getReplyLevel()>0 ? "reply" : "comment";
					}else{
						Post post=posts.get((int)latest.objectID);
						yield post!=null && post.getReplyLevel()>1 ? "reply" : "comment";
					}
				}
				case LIKE -> "like";
				case MENTION -> "mention";
				case RETOOT, REPOST -> "repost";
				case POST_OWN_WALL -> "wall_post";
				case INVITE_SIGNUP -> "invite_signup";
				case FOLLOW -> "follow";
				case FRIEND_REQ_ACCEPT -> "friend_accept";
			};

			Feedback feedback;
			if(latest.type==Notification.Type.REPLY || latest.type==Notification.Type.MENTION || latest.type==Notification.Type.POST_OWN_WALL){
				if(latest.objectType==Notification.ObjectType.COMMENT){
					feedback=switch(comments.get(latest.objectID).parentObjectID.type()){
						case PHOTO -> new Feedback("photo_comment", null, null, XTEA.encodeObjectID(latest.objectID, ObfuscatedObjectIDType.COMMENT), null);
						case BOARD_TOPIC -> new Feedback("board_comment", null, null, null, XTEA.encodeObjectID(latest.objectID, ObfuscatedObjectIDType.COMMENT));
					};
				}else if(latest.objectType==Notification.ObjectType.POST){
					Post post=posts.get((int)latest.objectID);
					feedback=new Feedback(post.getReplyLevel()>0 ? "wall_comment" : "wall_post", post.getReplyLevel()>0 ? null : post.id, post.getReplyLevel()>0 ? post.id : null, null, null);
				}else{
					throw new IllegalStateException();
				}
			}else{
				feedback=null;
			}

			NotificationObject obj;
			Parent parent;
			if(latest.type==Notification.Type.REPLY){
				if(latest.objectType==Notification.ObjectType.COMMENT){
					Comment comment=comments.get(latest.objectID);
					if(comment.getReplyLevel()>0){
						parent=switch(comment.parentObjectID.type()){
							case PHOTO -> new Parent("photo", null, XTEA.encodeObjectID(comment.parentObjectID.id(), ObfuscatedObjectIDType.PHOTO), null);
							case BOARD_TOPIC -> new Parent("board_topic", null, null, XTEA.encodeObjectID(comment.parentObjectID.id(), ObfuscatedObjectIDType.BOARD_TOPIC));
						};
						obj=new NotificationObject(switch(comment.parentObjectID.type()){
							case PHOTO -> "photo_comment";
							case BOARD_TOPIC -> "board_comment";
						}, null, null, XTEA.encodeObjectID(comment.replyKey.getLast(), ObfuscatedObjectIDType.COMMENT), null);
					}else{
						parent=null;
						obj=switch(comment.parentObjectID.type()){
							case PHOTO -> new NotificationObject("photo", null, null, null, XTEA.encodeObjectID(comment.parentObjectID.id(), ObfuscatedObjectIDType.PHOTO));
							case BOARD_TOPIC -> throw new IllegalStateException();
						};
					}
				}else if(latest.objectType==Notification.ObjectType.POST){
					Post post=posts.get((int)latest.objectID);
					if(post.getReplyLevel()>1){
						parent=new Parent("wall_post", post.replyKey.getFirst(), null, null);
						obj=new NotificationObject("wall_comment", null, post.replyKey.getLast(), null, null);
					}else{
						parent=null;
						obj=new NotificationObject("wall_post", post.replyKey.getFirst(), null, null, null);
					}
				}else{
					throw new IllegalStateException();
				}
			}else if(latest.type==Notification.Type.LIKE || latest.type==Notification.Type.REPOST || latest.type==Notification.Type.RETOOT){
				obj=switch(latest.objectType){
					case POST -> {
						Post post=posts.get((int) latest.objectID);
						if(post.getReplyLevel()>0){
							parent=new Parent("wall_post", post.replyKey.getFirst(), null, null);
							yield new NotificationObject("wall_comment", null, post.id, null, null);
						}else{
							parent=null;
							yield new NotificationObject("wall_post", post.id, null, null, null);
						}
					}
					case COMMENT -> {
						Comment comment=comments.get(latest.objectID);
						parent=switch(comment.parentObjectID.type()){
							case PHOTO -> new Parent("photo", null, XTEA.encodeObjectID(comment.parentObjectID.id(), ObfuscatedObjectIDType.PHOTO), null);
							case BOARD_TOPIC -> new Parent("board_topic", null, null, XTEA.encodeObjectID(comment.parentObjectID.id(), ObfuscatedObjectIDType.BOARD_TOPIC));
						};
						yield new NotificationObject(switch(comment.parentObjectID.type()){
							case PHOTO -> "photo_comment";
							case BOARD_TOPIC -> "board_comment";
						}, null, null, XTEA.encodeObjectID(comment.id, ObfuscatedObjectIDType.COMMENT), null);
					}
					case PHOTO -> {
						parent=null;
						yield new NotificationObject("photo", null, null, null, XTEA.encodeObjectID(latest.objectID, ObfuscatedObjectIDType.PHOTO));
					}
					case BOARD_TOPIC -> throw new IllegalStateException();
				};
			}else{
				parent=null;
				obj=null;
			}
			apiNotifications.add(new ApiNotification(latest.id, type, userIDs, userID, latest.time.getEpochSecond(), feedback, obj, parent));
		}

		List<PostViewModel> topLevelPosts=posts.values().stream().filter(p->p.getReplyLevel()==0).map(PostViewModel::new).toList();
		List<PostViewModel> wallComments=posts.values().stream().filter(p->p.getReplyLevel()>0).map(PostViewModel::new).toList();

		List<CommentViewModel> photoComments=comments.values().stream().filter(c->c.parentObjectID.type()==CommentableObjectType.PHOTO).map(CommentViewModel::new).toList();
		List<CommentViewModel> boardComments=comments.values().stream().filter(c->c.parentObjectID.type()==CommentableObjectType.BOARD_TOPIC).map(CommentViewModel::new).toList();

		return new NotificationsResponse(
				apiNotifications,
				ApiUtils.getPosts(topLevelPosts, ctx, actx, false, false, false),
				ApiUtils.getPosts(wallComments, ctx, actx, false, false, false),
				ApiUtils.getComments(photoComments, ctx, actx, false),
				ApiUtils.getComments(boardComments, ctx, actx, false),
				ApiUtils.getPhotos(ctx, actx, new ArrayList<>(photos.values())),
				topics.values().stream().map(ApiBoardTopic::new).toList(),
				ApiUtils.getUsers(needUsers, ctx, actx),
				ApiUtils.getGroups(needGroups, ctx, actx),
				actx.self.prefs.lastSeenNotificationID
		);
	}

	public static Object markAsViewed(ApplicationContext ctx, ApiCallContext actx){
		List<NotificationWrapper> notifications=ctx.getNotificationsController().getNotifications(actx.self, 0, 1, Integer.MAX_VALUE).list;
		if(!notifications.isEmpty() && notifications.getFirst().getLatestNotification().id<actx.self.prefs.lastSeenNotificationID){
			ctx.getNotificationsController().setNotificationsSeen(actx.self, notifications.getFirst().getLatestNotification().id);
			return true;
		}
		return false;
	}
}
