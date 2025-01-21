package smithereen.activitypub.handlers;

import java.util.List;
import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.exceptions.BadRequestException;
import smithereen.model.ForeignUser;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoTag;

public class RejectPhotoHandler extends ActivityTypeHandler<ForeignUser, Reject, ActivityPubPhoto>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Reject activity, ActivityPubPhoto object){
		Photo photo=object.asNativePhoto(context.appContext);
		if(activity.target==null || !(activity.target.object instanceof ActivityPubCollection collection))
			throw new BadRequestException("target is required");
		if(Objects.equals(collection.activityPubID, actor.getTaggedPhotosURL())){
			List<PhotoTag> tags=context.appContext.getPhotosController().getTagsForPhoto(photo.id);
			for(PhotoTag tag:tags){
				if(tag.userID()==actor.id){
					context.appContext.getPhotosController().deletePhotoTag(actor, photo, tag.id());
					break;
				}
			}
		}else{
			throw new BadRequestException("Unknown/unsupported collection ID in target");
		}
	}
}
