package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Like;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.notifications.Notification;
import smithereen.storage.LikeStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;

public class LikeNoteHandler extends ActivityTypeHandler<ForeignUser, Like, Post>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Like activity, Post post) throws SQLException{
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
