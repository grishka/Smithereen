package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.activities.Update;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.photos.Photo;
import smithereen.storage.GroupStorage;

public class UpdatePhotoHandler extends ActivityTypeHandler<ForeignUser, Update, ActivityPubPhoto>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Update activity, ActivityPubPhoto object) throws SQLException{
		Photo photo=object.asNativePhoto(context.appContext);
		context.appContext.getPhotosController().enforcePhotoManagementPermission(actor, photo);
		if(photo.id!=0){
			try{
				Photo existing=context.appContext.getPhotosController().getPhotoIgnoringPrivacy(photo.id);
				if(existing.albumID!=photo.albumID)
					throw new BadRequestException("Use Move{Photo} to move photos between albums");
			}catch(ObjectNotFoundException ignore){}
		}
		context.appContext.getPhotosController().putOrUpdateForeignPhoto(photo);
		if(photo.ownerID<0){
			Group owner=context.appContext.getGroupsController().getGroupOrThrow(-photo.ownerID);
			if(!(owner instanceof ForeignGroup)){
				context.forwardActivity(GroupStorage.getGroupMemberInboxes(owner.id), owner);
			}
		}
	}
}
