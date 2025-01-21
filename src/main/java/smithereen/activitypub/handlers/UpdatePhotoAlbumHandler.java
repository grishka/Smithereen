package smithereen.activitypub.handlers;

import java.sql.SQLException;
import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Update;
import smithereen.exceptions.BadRequestException;
import smithereen.model.photos.PhotoAlbum;

public class UpdatePhotoAlbumHandler extends ActivityTypeHandler<Actor, Update, ActivityPubPhotoAlbum>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Update activity, ActivityPubPhotoAlbum object) throws SQLException{
		if(!Objects.equals(object.attributedTo, actor.activityPubID))
			throw new BadRequestException("PhotoAlbum.attributedTo must match Actor.id");
		PhotoAlbum nativeAlbum=object.asNativePhotoAlbum(context.appContext);
		boolean isNew=nativeAlbum.id==0;
		context.appContext.getPhotosController().putOrUpdateForeignAlbum(nativeAlbum);
		if(isNew){
			// We've received an Update for an album we've not seen before. This most possibly means its owner changed the view privacy setting
			// so at least some of our users can see it now. It makes sense, then, that there's already some photos in this album,
			// and we should reload its content.
			context.appContext.getActivityPubWorker().fetchPhotoAlbumContents(object, nativeAlbum);
		}
	}
}
