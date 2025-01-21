package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityForwardingUtils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.activities.Like;
import smithereen.model.ForeignUser;
import smithereen.model.LikeableContentObject;

public class LikeObjectHandler extends ActivityTypeHandler<ForeignUser, Like, ActivityPubObject>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Like activity, ActivityPubObject _post) throws SQLException{
		LikeableContentObject obj=context.appContext.getObjectLinkResolver().resolveLocally(_post.activityPubID, LikeableContentObject.class);
		context.appContext.getUserInteractionsController().setObjectLiked(obj, true, actor, activity.activityPubID);
		ActivityForwardingUtils.forwardContentInteraction(context, obj);
	}
}
