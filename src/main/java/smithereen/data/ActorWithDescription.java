package smithereen.data;

import smithereen.activitypub.objects.Actor;

public record ActorWithDescription(Actor actor, String description){
}
