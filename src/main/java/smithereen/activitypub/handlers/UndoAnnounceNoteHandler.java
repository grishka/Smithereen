package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.notifications.Notification;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.NotificationsStorage;

public class UndoAnnounceNoteHandler extends NestedActivityTypeHandler<ForeignUser, Undo, Announce, Post>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Undo activity, Announce nested, Post post) throws SQLException{
		NewsfeedStorage.deleteRetoot(actor.id, post.id);
		NotificationsStorage.deleteNotification(Notification.ObjectType.POST, post.id, Notification.Type.RETOOT, actor.id);
	}
}
