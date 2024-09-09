package smithereen.model;

import smithereen.activitypub.objects.activities.Like;
import smithereen.model.notifications.Notification;
import smithereen.model.photos.Photo;

public sealed interface LikeableContentObject extends OwnedContentObject permits Photo, Post{
	Like.ObjectType getLikeObjectType();
	Notification.ObjectType getObjectTypeForLikeNotifications();
}
