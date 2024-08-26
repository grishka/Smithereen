package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.model.ForeignUser;
import smithereen.model.Post;
import smithereen.model.notifications.Notification;
import smithereen.storage.LikeStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;

public class UndoLikeNoteHandler extends NestedActivityTypeHandler<ForeignUser, Undo, Like, NoteOrQuestion>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Undo activity, Like nested, NoteOrQuestion _post) throws SQLException{
		Post post=context.appContext.getWallController().getPostOrThrow(_post.activityPubID);
		int id=LikeStorage.setObjectLiked(actor.id, post.id, Like.ObjectType.POST, false, null);
		NotificationsStorage.deleteNotification(Notification.ObjectType.POST, post.id, Notification.Type.LIKE, actor.id);
		if(id==0)
			return;
		if(context.ldSignatureOwner!=null)
			context.forwardActivity(PostStorage.getInboxesForPostInteractionForwarding(post), context.appContext.getUsersController().getUserOrThrow(post.authorID));
	}
}
