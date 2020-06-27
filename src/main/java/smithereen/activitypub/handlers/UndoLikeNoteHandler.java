package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.notifications.Notification;
import smithereen.storage.LikeStorage;
import smithereen.storage.NotificationsStorage;

public class UndoLikeNoteHandler extends NestedActivityTypeHandler<ForeignUser, Undo, Like, Post>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Undo activity, Like nested, Post post) throws SQLException{
		LikeStorage.setPostLiked(actor.id, post.id, false);
		NotificationsStorage.deleteNotification(Notification.ObjectType.POST, post.id, Notification.Type.LIKE, actor.id);
	}
}
