package smithereen.activitypub.handlers;

import com.google.gson.JsonObject;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Add;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.ForeignUser;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.photos.PhotoTag;

public class AddPhotoHandler extends ActivityTypeHandler<Actor, Add, ActivityPubPhoto>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Add activity, ActivityPubPhoto object){
		URI albumID;
		if(activity.target==null)
			throw new BadRequestException("target is required");
		else if(activity.target.link!=null)
			albumID=activity.target.link;
		else if(activity.target.object instanceof ActivityPubPhotoAlbum pa)
			albumID=pa.activityPubID;
		else if(activity.target.object instanceof ActivityPubCollection collection){
			if(!Objects.equals(actor.activityPubID, collection.attributedTo))
				throw new BadRequestException("target.attributedTo must match actor ID");
			if(actor instanceof ForeignUser user && user.getTaggedPhotosURL()!=null && Objects.equals(user.getTaggedPhotosURL(), collection.activityPubID)){
				handleApprovePhotoTag(context.appContext, user, object);
			}else{
				throw new BadRequestException("Unknown/unsupported collection ID in target");
			}
			return;
		}else
			throw new BadRequestException("target must be a link or an abbreviated PhotoAlbum object");
		if(albumID==null)
			throw new BadRequestException("target must be a link or an abbreviated PhotoAlbum object");

		PhotoAlbum album=context.appContext.getObjectLinkResolver().resolveLocally(albumID, PhotoAlbum.class);
		if(album.ownerID!=actor.getOwnerID())
			throw new UserActionNotAllowedException("This actor does not own this album");
		if(album.ownerID>0 && !Objects.equals(object.attributedTo, actor.activityPubID))
			throw new BadRequestException("User-owned albums can only contain photos attributedTo their owners");

		Photo photo=object.asNativePhoto(context.appContext);
		context.appContext.getPhotosController().putOrUpdateForeignPhoto(photo, object);
	}

	private void handleApprovePhotoTag(ApplicationContext ctx, ForeignUser user, ActivityPubPhoto object){
		Photo photo=object.asNativePhoto(ctx);
		boolean wasNew=photo.id==0;
		if(photo.id==0){
			ctx.getPhotosController().putOrUpdateForeignPhoto(photo, object);
		}
		if(!tryApprovePhotoTag(ctx, user, object, photo) && wasNew){
			// Maybe our local version was stale and didn't have this tag. Let's force-reload it and try again.
			photo=ctx.getObjectLinkResolver().resolveNative(photo.getActivityPubID(), Photo.class, true, true, true, (JsonObject) null, true);
			if(tryApprovePhotoTag(ctx, user, object, photo))
				return;
		}
		throw new BadRequestException("This user is not tagged in this photo");
	}

	private boolean tryApprovePhotoTag(ApplicationContext ctx, ForeignUser user, ActivityPubPhoto object, Photo photo){
		List<PhotoTag> tags=ctx.getPhotosController().getTagsForPhoto(photo.id);
		for(PhotoTag tag:tags){
			if(tag.userID()==user.id){
				if(!tag.approved()){
					ctx.getPhotosController().approvePhotoTag(user, photo, tag.id());
				}
				return true;
			}
		}
		return false;
	}
}
