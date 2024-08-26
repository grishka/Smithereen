package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.photos.PhotoAlbum;

public class DeletePhotoAlbumHandler extends ActivityTypeHandler<Actor, Delete, ActivityPubPhotoAlbum>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Delete activity, ActivityPubPhotoAlbum object) throws SQLException{
		PhotoAlbum nativeAlbum=object.asNativePhotoAlbum(context.appContext);
		if(nativeAlbum.id==0){
			LOG.debug("Ignoring Delete{PhotoAlbum} for album {} because it is not in local DB", object.activityPubID);
			return;
		}
		if(actor instanceof ForeignUser user)
			context.appContext.getPhotosController().deleteAlbum(user, nativeAlbum);
		else if(actor instanceof ForeignGroup group)
			context.appContext.getPhotosController().deleteAlbum(group, nativeAlbum);
	}
}
