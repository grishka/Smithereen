package smithereen.data.notifications;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

import smithereen.Config;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.storage.NotificationsStorage;

public class NotificationUtils{
	public static void putNotificationsForPost(@NotNull Post post, @Nullable Post parent) throws SQLException{
		boolean isReply=post.getReplyLevel()>0;
		if(isReply && parent==null)
			throw new IllegalArgumentException("Post is a reply but parent is null");
		else if(!isReply && parent!=null)
			throw new IllegalArgumentException("Post is not a reply but parent is not null");

		// For a reply to a local post, notify the parent post author about the reply
		if(isReply && Config.isLocal(parent.url) && Config.isLocal(parent.attributedTo) && !parent.user.equals(post.user)){
			Notification n=new Notification();
			n.type=Notification.Type.REPLY;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			n.relatedObjectID=parent.id;
			n.relatedObjectType=Notification.ObjectType.POST;
			n.actorID=post.user.id;
			NotificationsStorage.putNotification(parent.user.id, n);
		}

		// Notify every mentioned local user, except the parent post author, if any
		for(User user:post.mentionedUsers){
			if(user instanceof ForeignUser)
				continue;
			if(isReply && user.equals(parent.user))
				continue;
			if(user.equals(post.user))
				continue;
			Notification n=new Notification();
			n.type=Notification.Type.MENTION;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			n.actorID=post.user.id;
			NotificationsStorage.putNotification(user.id, n);
		}

		// Finally, if it's a wall post on a local user's wall, notify them
		if(!post.owner.equals(post.user) && !(post.owner instanceof ForeignUser) && post.owner instanceof User){
			Notification n=new Notification();
			n.type=Notification.Type.POST_OWN_WALL;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			n.actorID=post.user.id;
			NotificationsStorage.putNotification(((User) post.owner).id, n);
		}
	}
}
