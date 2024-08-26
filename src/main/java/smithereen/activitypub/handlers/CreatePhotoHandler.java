package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
import smithereen.activitypub.objects.activities.Create;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;

public class CreatePhotoHandler extends ActivityTypeHandler<ForeignUser, Create, ActivityPubPhoto>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Create activity, ActivityPubPhoto object) throws SQLException{
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
		if(album.ownerID>0)
			throw new BadRequestException("Create{Photo} is only for adding photos to group-owned albums");

		Group group=context.appContext.getGroupsController().getLocalGroupOrThrow(-album.ownerID);
		context.appContext.getPrivacyController().enforceUserAccessToGroupContent(actor, group);
		if(album.flags.contains(PhotoAlbum.Flag.GROUP_RESTRICT_UPLOADS))
			throw new UserActionNotAllowedException("Uploads to this album are restricted to group staff");
		Photo photo=object.asNativePhoto(context.appContext);
		boolean wasNew=photo.id==0;
		context.appContext.getPhotosController().putOrUpdateForeignPhoto(photo);
		if(wasNew){
			context.appContext.getActivityPubWorker().sendAddPhotoToAlbum(group, photo, album);
		}
	}
}
