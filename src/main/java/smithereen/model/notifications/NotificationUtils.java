package smithereen.model.notifications;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

import smithereen.model.ForeignUser;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.UserStorage;

public class NotificationUtils{
	public static void putNotificationsForPost(@NotNull Post post, @Nullable Post parent) throws SQLException{
		boolean isReply=post.getReplyLevel()>0;
		if(isReply && parent==null)
			throw new IllegalArgumentException("Post is a reply but parent is null");
		else if(!isReply && parent!=null)
			throw new IllegalArgumentException("Post is not a reply but parent is not null");

		// For a reply to a local post, notify the parent post author about the reply
		if(isReply && parent.isLocal() /*&& Config.isLocal(parent.attributedTo)*/ && parent.authorID!=post.authorID){
			Notification n=new Notification();
			n.type=Notification.Type.REPLY;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			n.relatedObjectID=parent.id;
			n.relatedObjectType=Notification.ObjectType.POST;
			n.actorID=post.authorID;
			NotificationsStorage.putNotification(parent.authorID, n);
		}

		// Notify every mentioned local user, except the parent post author, if any
		for(User user:UserStorage.getById(post.mentionedUserIDs).values()){
			if(user instanceof ForeignUser)
				continue;
			if(isReply && user.id==parent.authorID)
				continue;
			if(user.id==post.authorID)
				continue;
			if(UserStorage.isUserBlocked(user.id, post.authorID))
				continue;
			Notification n=new Notification();
			n.type=Notification.Type.MENTION;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			n.actorID=post.authorID;
			NotificationsStorage.putNotification(user.id, n);
		}

		// Finally, if it's a wall post on a local user's wall, notify them
		if(post.getReplyLevel()==0 && post.ownerID!=post.authorID && post.ownerID>0 && !(UserStorage.getById(post.ownerID) instanceof ForeignUser)){
			Notification n=new Notification();
			n.type=Notification.Type.POST_OWN_WALL;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			n.actorID=post.authorID;
			NotificationsStorage.putNotification(post.ownerID, n);
		}
	}
}
