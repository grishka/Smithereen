package smithereen.routes;

import java.util.HashSet;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.model.Account;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.notifications.Notification;
import smithereen.model.photos.Photo;
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
		HashSet<Long> needPhotos=new HashSet<>();

		for(Notification n:notifications.list){
			needUsers.add(n.actorID);
			switch(n.objectType){
				case null -> {}
				case POST -> needPosts.add((int) n.objectID);
				case PHOTO -> needPhotos.add(n.objectID);
			}
			switch(n.relatedObjectType){
				case null -> {}
				case POST -> needPosts.add((int) n.relatedObjectID);
				case PHOTO -> needPhotos.add(n.relatedObjectID);
			}
		}

		Map<Integer, User> users=ctx.getUsersController().getUsers(needUsers);
		Map<Integer, Post> posts=ctx.getWallController().getPosts(needPosts);
		Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);

		model.with("users", users)
				.with("posts", posts)
				.with("photos", photos);

		if(!notifications.list.isEmpty()){
			int last=notifications.list.getFirst().id;
			ctx.getNotificationsController().setNotificationsSeen(self, last);
		}

		return model;
	}
}
