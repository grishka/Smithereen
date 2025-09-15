package smithereen.model.groups;

import smithereen.activitypub.objects.LocalImage;
import smithereen.controllers.ObjectLinkResolver;

public record GroupLinkParseResult(ObjectLinkResolver.ObjectTypeAndID apObject, String title, String imageURL, LocalImage image){
}
