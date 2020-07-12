package smithereen.routes;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import smithereen.Utils;
import smithereen.data.Account;
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
		int offset=Utils.parseIntOrDefault(req.queryParams("offset"), 0);
		RenderedTemplateResponse model=new RenderedTemplateResponse("notifications");
		int[] total={0};
		List<Notification> notifications=NotificationsStorage.getNotifications(self.user.id, offset, total);
		model.with("title", lang(req).get("notifications")).with("notifications", notifications).with("offset", offset).with("total", total[0]);
		ArrayList<Integer> needUsers=new ArrayList<>(), needPosts=new ArrayList<>();

		for(Notification n:notifications){
			needUsers.add(n.actorID);
			if(n.objectType==Notification.ObjectType.POST){
				needPosts.add(n.objectID);
			}
			if(n.relatedObjectType==Notification.ObjectType.POST){
				needPosts.add(n.relatedObjectID);
			}
		}

		HashMap<Integer, User> users=new HashMap<>();
		HashMap<Integer, Post> posts=new HashMap<>();
		// TODO get all users & posts in one database query
		for(Integer uid:needUsers){
			if(users.containsKey(uid))
				continue;
			users.put(uid, UserStorage.getById(uid));
		}
		for(Integer pid:needPosts){
			if(posts.containsKey(pid))
				continue;
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

		return model.renderToString(req);
	}
}
