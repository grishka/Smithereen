package smithereen.data;

import smithereen.activitypub.objects.Actor;

public record OwnerAndAuthor(Actor owner, User author){
}
