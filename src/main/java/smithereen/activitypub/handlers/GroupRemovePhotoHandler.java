package smithereen.activitypub.handlers;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.model.ForeignGroup;
import smithereen.model.photos.Photo;

public class GroupRemovePhotoHandler extends ActivityTypeHandler<ForeignGroup, Remove, ActivityPubPhoto>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Remove activity, ActivityPubPhoto object){
		Photo photo=object.asNativePhoto(context.appContext);
		context.appContext.getPhotosController().deletePhoto(actor, photo);
	}
}
