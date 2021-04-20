package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.Utils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Like;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.data.notifications.Notification;
import smithereen.storage.LikeStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;

public class LikeNoteHandler extends ActivityTypeHandler<ForeignUser, Like, Post>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Like activity, Post post) throws SQLException{
		Utils.ensureUserNotBlocked(actor, post.user);
		if(post.owner instanceof User)
			Utils.ensureUserNotBlocked(actor, (User) post.owner);
		if(post.owner instanceof Group)
			Utils.ensureUserNotBlocked(actor, (Group) post.owner);
		int id=LikeStorage.setPostLiked(actor.id, post.id, true);
		if(id==0)
			return;
		if(!(post.user instanceof ForeignUser)){
			Notification n=new Notification();
			n.type=Notification.Type.LIKE;
			n.actorID=actor.id;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			NotificationsStorage.putNotification(post.user.id, n);
			if(context.ldSignatureOwner!=null)
				context.forwardActivity(PostStorage.getInboxesForPostInteractionForwarding(post), post.user);
		}
	}
}
