package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.Server;
import smithereen.model.photos.Photo;
import smithereen.storage.GroupStorage;

public class DeletePhotoHandler extends ActivityTypeHandler<ForeignUser, Delete, ActivityPubPhoto>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Delete activity, ActivityPubPhoto object) throws SQLException{
		Photo photo=object.asNativePhoto(context.appContext);
		context.appContext.getPhotosController().deletePhoto(actor, photo);
		if(photo.ownerID<0){
			Group owner=context.appContext.getGroupsController().getGroupOrThrow(-photo.ownerID);
			if(!(owner instanceof ForeignGroup)){
				context.forwardActivity(GroupStorage.getGroupMemberInboxes(owner.id), owner, Server.Feature.PHOTO_ALBUMS);
			}
		}
	}
}
