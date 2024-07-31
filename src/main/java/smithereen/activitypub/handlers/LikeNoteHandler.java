package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.Utils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Like;
import smithereen.model.ForeignUser;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.Post;
import smithereen.model.notifications.Notification;
import smithereen.storage.LikeStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;

public class LikeNoteHandler extends ActivityTypeHandler<ForeignUser, Like, NoteOrQuestion>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Like activity, NoteOrQuestion _post) throws SQLException{
		Post post=context.appContext.getWallController().getPostOrThrow(_post.activityPubID);
		OwnerAndAuthor oaa=context.appContext.getWallController().getContentAuthorAndOwner(post);

		Utils.ensureUserNotBlocked(actor, oaa.author());
		Utils.ensureUserNotBlocked(actor, oaa.owner());

		int id=LikeStorage.putLike(actor.id, post.id, Like.ObjectType.POST, activity.activityPubID);
		if(id==0)
			return;
		if(!(oaa.author() instanceof ForeignUser)){
			Notification n=new Notification();
			n.type=Notification.Type.LIKE;
			n.actorID=actor.id;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			NotificationsStorage.putNotification(post.authorID, n);
			if(context.ldSignatureOwner!=null)
				context.forwardActivity(PostStorage.getInboxesForPostInteractionForwarding(post), oaa.author());
		}
	}
}
