package smithereen.routes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static smithereen.Utils.*;

import smithereen.ApplicationContext;
import smithereen.model.Account;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.WebDeltaResponse;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.model.notifications.GroupedNotification;
import smithereen.model.notifications.Notification;
import smithereen.model.notifications.NotificationWrapper;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

public class NotificationsRoutes{
	public static Object notifications(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("notifications", req);
		int maxID=parseIntOrDefault(req.queryParams("maxID"), Integer.MAX_VALUE);
		PaginatedList<NotificationWrapper> notifications=ctx.getNotificationsController().getNotifications(self, offset(req), 50, maxID);
		model.pageTitle(lang(req).get("notifications"));
		if(notifications.list.isEmpty()){
			model.paginate(notifications);
		}else{
			if(maxID==Integer.MAX_VALUE){
				maxID=notifications.list.getFirst().getLatestNotification().id;
			}
			model.paginate(notifications, "/my/notifications?maxID="+maxID+"&offset=", "/my/notifications");
		}
		HashSet<Integer> needUsers=new HashSet<>(), needPosts=new HashSet<>();
		HashSet<Long> needPhotos=new HashSet<>(), needComments=new HashSet<>(), needTopics=new HashSet<>();

		for(NotificationWrapper nw:notifications.list){
			switch(nw){
				case Notification n -> needUsers.add(n.actorID);
				case GroupedNotification gn -> {
					for(Notification n:gn.notifications){
						needUsers.add(n.actorID);
					}
				}
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

		Map<Long, Comment> rawComments=ctx.getCommentsController().getCommentsIgnoringPrivacy(needComments);
		HashSet<Long> needExtraComments=new HashSet<>();
		for(Comment c:rawComments.values()){
			switch(c.parentObjectID.type()){
				case PHOTO -> needPhotos.add(c.parentObjectID.id());
			}
			if(c.getReplyLevel()>0 && !rawComments.containsKey(c.replyKey.getLast()))
				needExtraComments.add(c.replyKey.getLast());
		}

		if(!needExtraComments.isEmpty()){
			rawComments=new HashMap<>(rawComments);
			rawComments.putAll(ctx.getCommentsController().getCommentsIgnoringPrivacy(needExtraComments));
		}

		Map<Integer, PostViewModel> posts=ctx.getWallController().getPosts(needPosts)
				.values()
				.stream()
				.map(PostViewModel::new)
				.collect(Collectors.toMap(p->p.post.id, Function.identity()));
		Map<Long, CommentViewModel> comments=rawComments.values()
				.stream()
				.map(CommentViewModel::new)
				.collect(Collectors.toMap(c->c.post.id, Function.identity()));

		ctx.getWallController().populateReposts(self.user, posts.values(), 2);
		PostViewModel.collectActorIDs(posts.values(), needUsers, null);

		Map<Integer, User> users=ctx.getUsersController().getUsers(needUsers);
		Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
		Map<Long, BoardTopic> topics=ctx.getBoardController().getTopicsIgnoringPrivacy(needTopics);

		model.with("users", users)
				.with("posts", posts)
				.with("photos", photos)
				.with("comments", comments)
				.with("topics", topics)
				.with("lastSeenID", self.prefs.lastSeenNotificationID);

		if(!isMobile(req)){
			model.with("postInteractions", ctx.getWallController().getUserInteractions(posts.values().stream().toList(), self.user));
			model.with("commentInteractions", ctx.getUserInteractionsController().getUserInteractions(comments.values().stream().map(cvm->cvm.post).toList(), self.user));
			model.with("photoInteractions", ctx.getUserInteractionsController().getUserInteractions(photos.values().stream().toList(), self.user));

			HashSet<List<Integer>> needOwnReplies=new HashSet<>();
			HashSet<List<Long>> needOwnCommentReplies=new HashSet<>();
			for(NotificationWrapper nw:notifications.list){
				Notification n=nw.getLatestNotification();
				if(n.type==Notification.Type.REPLY || n.type==Notification.Type.MENTION || n.type==Notification.Type.POST_OWN_WALL){
					if(n.objectType==Notification.ObjectType.POST){
						PostViewModel post=posts.get((int)n.objectID);
						if(post!=null){
							needOwnReplies.add(post.getReplyKeyForInteractions());
						}
					}else if(n.objectType==Notification.ObjectType.COMMENT){
						CommentViewModel comment=comments.get(n.objectID);
						if(comment!=null){
							needOwnCommentReplies.add(comment.post.getReplyKeyForReplies());
						}
					}
				}
			}
			model.with("ownWallReplies", ctx.getWallController().getUserReplies(self.user, needOwnReplies));
			model.with("ownCommentReplies", ctx.getCommentsController().getUserReplies(self.user, needOwnCommentReplies));
		}

		if(!notifications.list.isEmpty()){
			int last=notifications.list.getFirst().getLatestNotification().id;
			ctx.getNotificationsController().setNotificationsSeen(self, last);
		}

		if(isAjax(req)){
			String paginationID=req.queryParams("pagination");
			if(StringUtils.isNotEmpty(paginationID)){
				WebDeltaResponse r=new WebDeltaResponse(resp)
						.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, notifications.list.isEmpty() ? "" : model.renderBlock("notificationsInner"));
				if(notifications.list.isEmpty()){
					r.remove("ajaxPagination_"+paginationID);
				}else{
					r.setAttribute("ajaxPaginationLink_"+paginationID, "href", req.pathInfo()+"?offset="+(notifications.offset+notifications.perPage)+"&maxID="+maxID);
				}
				return r;
			}
		}

		return model;
	}

	public static Object ajaxReadLastNotifications(Request req, Response resp, Account self, ApplicationContext ctx){
		// Given a notification ID, move the "last seen" marker forward, but only if it's the only counted unread notification.
		// Called when an instant notification is clicked. Used to reset the notifications counter so the user won't have to
		// click "my feedback" menu item just to reset it manually *after* already having seen and clicked the instant notification.
		if(ctx.getNotificationsController().getUserCounters(self).getNewNotificationsCount()==1){
			int id=safeParseInt(req.queryParams("id"));
			if(self.prefs.lastSeenNotificationID<id){
				ctx.getNotificationsController().setNotificationsSeen(self, id);
			}
		}
		return "";
	}
}
