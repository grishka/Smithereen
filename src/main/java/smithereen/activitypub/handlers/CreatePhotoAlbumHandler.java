package smithereen.activitypub.handlers;

import java.sql.SQLException;
import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Create;
import smithereen.exceptions.BadRequestException;
import smithereen.model.photos.PhotoAlbum;

public class CreatePhotoAlbumHandler extends ActivityTypeHandler<Actor, Create, ActivityPubPhotoAlbum>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Create activity, ActivityPubPhotoAlbum object) throws SQLException{
		if(!Objects.equals(object.attributedTo, actor.activityPubID))
			throw new BadRequestException("PhotoAlbum.attributedTo must match Actor.id");
		PhotoAlbum nativeAlbum=object.asNativePhotoAlbum(context.appContext);
		context.appContext.getPhotosController().putOrUpdateForeignAlbum(nativeAlbum);
	}
}
