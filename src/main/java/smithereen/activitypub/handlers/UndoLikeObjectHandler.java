package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityForwardingUtils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.model.ForeignUser;
import smithereen.model.LikeableContentObject;

public class UndoLikeObjectHandler extends NestedActivityTypeHandler<ForeignUser, Undo, Like, ActivityPubObject>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Undo activity, Like nested, ActivityPubObject _post) throws SQLException{
		LikeableContentObject obj=context.appContext.getObjectLinkResolver().resolveLocally(_post.activityPubID, LikeableContentObject.class);
		context.appContext.getUserInteractionsController().setObjectLiked(obj, false, actor, nested.activityPubID);
		ActivityForwardingUtils.forwardContentInteraction(context, obj);
	}
}
