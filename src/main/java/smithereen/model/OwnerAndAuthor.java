package smithereen.model;

import smithereen.activitypub.objects.Actor;

public record OwnerAndAuthor(Actor owner, User author){
}
