package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.Config;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Add;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.data.FederationState;
import smithereen.data.Post;
import smithereen.storage.PostStorage;

public class RejectAddNoteHandler extends NestedActivityTypeHandler<Actor, Reject, Add, NoteOrQuestion>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Reject activity, Add nested, NoteOrQuestion object) throws SQLException{
		if(!Config.isLocal(object.activityPubID)){
			LOG.warn("Reject{Add{Note}} post {} is not local", object.activityPubID);
			return;
		}
		Post post=context.appContext.getWallController().getPostOrThrow(object.activityPubID);
		Actor postOwner=context.appContext.getWallController().getPostAuthorAndOwner(post).owner();
		if(!actor.activityPubID.equals(postOwner.activityPubID)){
			LOG.warn("Reject{Add{Note}} post {} owner {} does not match actor {}", object.activityPubID, postOwner.activityPubID, actor.activityPubID);
			return;
		}
		PostStorage.setPostFederationState(post.id, FederationState.REJECTED);
	}
}
