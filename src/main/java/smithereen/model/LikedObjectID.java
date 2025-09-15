package smithereen.model;

import smithereen.activitypub.objects.activities.Like;

public record LikedObjectID(Like.ObjectType type, long id){
}
