package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.activities.Like;
import smithereen.model.ForeignUser;
import smithereen.model.photos.Photo;

public class LikePhotoHandler extends ActivityTypeHandler<ForeignUser, Like, ActivityPubPhoto>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Like activity, ActivityPubPhoto object) throws SQLException{
		Photo photo=object.asNativePhoto(context.appContext);
		context.appContext.getUserInteractionsController().setObjectLiked(photo, true, actor, activity.activityPubID);
	}
}
