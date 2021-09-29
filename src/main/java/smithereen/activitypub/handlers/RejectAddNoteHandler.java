package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Add;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.data.FederationState;
import smithereen.data.Post;
import smithereen.storage.PostStorage;

public class RejectAddNoteHandler extends NestedActivityTypeHandler<Actor, Reject, Add, Post>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Reject activity, Add nested, Post object) throws SQLException{
		if(!object.local){
			LOG.warn("Reject{Add{Note}} post {} is not local", object.activityPubID);
			return;
		}
		if(!actor.activityPubID.equals(object.owner)){
			LOG.warn("Reject{Add{Note}} post {} owner {} does not match actor {}", object.activityPubID, object.owner.activityPubID, actor.activityPubID);
			return;
		}
		PostStorage.setPostFederationState(object.id, FederationState.REJECTED);
	}
}
