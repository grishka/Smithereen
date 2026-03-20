package smithereen.model;

import org.jetbrains.annotations.Nullable;

import smithereen.activitypub.objects.Actor;

public record OwnerAndAuthor(@Nullable Actor owner, @Nullable User author){
}
