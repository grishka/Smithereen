package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;
import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Add;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;

public class AddPhotoHandler extends ActivityTypeHandler<Actor, Add, ActivityPubPhoto>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Add activity, ActivityPubPhoto object) throws SQLException{
		URI albumID;
		if(activity.target==null)
			throw new BadRequestException("target is required");
		else if(activity.target.link!=null)
			albumID=activity.target.link;
		else if(activity.target.object instanceof ActivityPubPhotoAlbum pa)
			albumID=pa.activityPubID;
		else
			throw new BadRequestException("target must be a link or an abbreviated PhotoAlbum object");
		if(albumID==null)
			throw new BadRequestException("target must be a link or an abbreviated PhotoAlbum object");

		PhotoAlbum album=context.appContext.getObjectLinkResolver().resolveLocally(albumID, PhotoAlbum.class);
		if(album.ownerID!=actor.getOwnerID())
			throw new UserActionNotAllowedException("This actor does not own this album");
		if(album.ownerID>0 && !Objects.equals(object.attributedTo, actor.activityPubID))
			throw new BadRequestException("User-owned albums can only contain photos attributedTo their owners");

		Photo photo=object.asNativePhoto(context.appContext);
		context.appContext.getPhotosController().putOrUpdateForeignPhoto(photo);
	}
}
