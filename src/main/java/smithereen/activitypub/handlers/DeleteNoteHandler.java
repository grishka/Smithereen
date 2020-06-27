package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.notifications.Notification;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;

public class DeleteNoteHandler extends ActivityTypeHandler<ForeignUser, Delete, Post>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Delete activity, Post post) throws SQLException{
		if(post.canBeManagedBy(actor)){
			PostStorage.deletePost(post.id);
			NotificationsStorage.deleteNotificationsForObject(Notification.ObjectType.POST, post.id);
		}else{
			throw new IllegalArgumentException("No access to delete this post");
		}
	}
}
