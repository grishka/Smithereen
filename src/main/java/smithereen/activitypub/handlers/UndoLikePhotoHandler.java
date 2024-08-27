package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityForwardingUtils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.model.ForeignUser;
import smithereen.model.photos.Photo;

public class UndoLikePhotoHandler extends NestedActivityTypeHandler<ForeignUser, Undo, Like, ActivityPubPhoto>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Undo activity, Like nested, ActivityPubPhoto object) throws SQLException{
		Photo photo=object.asNativePhoto(context.appContext);
		context.appContext.getUserInteractionsController().setObjectLiked(photo, false, actor, nested.activityPubID);
		ActivityForwardingUtils.forwardPhotoRelatedActivity(context, photo);
	}
}
