package smithereen.routes;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import smithereen.Utils;
import smithereen.data.Account;
import smithereen.data.PaginatedList;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.data.notifications.Notification;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;

import static smithereen.Utils.*;

public class NotificationsRoutes{
	public static Object notifications(Request req, Response resp, Account self) throws SQLException{
		int offset=offset(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("notifications", req);
		int[] total={0};
		List<Notification> notifications=NotificationsStorage.getNotifications(self.user.id, offset, 50, total);
		model.pageTitle(lang(req).get("notifications")).paginate(new PaginatedList<Notification>(notifications, total[0], offset, 50));
		HashSet<Integer> needUsers=new HashSet<>(), needPosts=new HashSet<>();

		for(Notification n:notifications){
			needUsers.add(n.actorID);
			if(n.objectType==Notification.ObjectType.POST){
				needPosts.add(n.objectID);
			}
			if(n.relatedObjectType==Notification.ObjectType.POST){
				needPosts.add(n.relatedObjectID);
			}
		}

		Map<Integer, User> users=UserStorage.getById(needUsers);
		Map<Integer, Post> posts=new HashMap<>();
		for(Integer pid:needPosts){
			posts.put(pid, PostStorage.getPostByID(pid, false));
		}

		model.with("users", users).with("posts", posts);

		if(!notifications.isEmpty()){
			int last=notifications.get(0).id;
			if(last>self.prefs.lastSeenNotificationID){
				self.prefs.lastSeenNotificationID=last;
				SessionStorage.updatePreferences(self.id, self.prefs);
			}
		}
		NotificationsStorage.getNotificationsForUser(self.user.id, self.prefs.lastSeenNotificationID).setNotificationsViewed();

		return model;
	}
}
