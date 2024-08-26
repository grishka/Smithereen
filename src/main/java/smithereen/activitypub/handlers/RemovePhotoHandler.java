package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.model.ForeignGroup;
import smithereen.model.photos.Photo;

public class RemovePhotoHandler extends ActivityTypeHandler<ForeignGroup, Remove, ActivityPubPhoto>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Remove activity, ActivityPubPhoto object) throws SQLException{
		Photo photo=object.asNativePhoto(context.appContext);
		context.appContext.getPhotosController().deletePhoto(actor, photo);
	}
}
