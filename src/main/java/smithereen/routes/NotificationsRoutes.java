package smithereen.routes;

import java.util.HashSet;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.model.Account;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.notifications.Notification;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;

import static smithereen.Utils.*;

public class NotificationsRoutes{
	public static Object notifications(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("notifications", req);
		PaginatedList<Notification> notifications=ctx.getNotificationsController().getNotifications(self.user, offset(req), 50);
		model.pageTitle(lang(req).get("notifications")).paginate(notifications);
		HashSet<Integer> needUsers=new HashSet<>(), needPosts=new HashSet<>();

		for(Notification n:notifications.list){
			needUsers.add(n.actorID);
			if(n.objectType==Notification.ObjectType.POST){
				needPosts.add(n.objectID);
			}
			if(n.relatedObjectType==Notification.ObjectType.POST){
				needPosts.add(n.relatedObjectID);
			}
		}

		Map<Integer, User> users=ctx.getUsersController().getUsers(needUsers);
		Map<Integer, Post> posts=ctx.getWallController().getPosts(needPosts);

		model.with("users", users).with("posts", posts);

		if(!notifications.list.isEmpty()){
			int last=notifications.list.get(0).id;
			ctx.getNotificationsController().setNotificationsSeen(self, last);
		}

		return model;
	}
}
