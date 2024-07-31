package smithereen.model;

import smithereen.activitypub.objects.activities.Like;
import smithereen.model.notifications.Notification;

public interface LikeableContentObject{
	Like.ObjectType getLikeObjectType();
	Notification.ObjectType getObjectTypeForLikeNotifications();
}
