package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubActorStatus;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.model.ActorStatus;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;

public class DeleteActorStatusHandler extends ActivityTypeHandler<Actor, Delete, ActivityPubActorStatus>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Delete activity, ActivityPubActorStatus object) throws SQLException{
		if(actor instanceof ForeignUser user)
			context.appContext.getUsersController().updateStatus(user, (ActorStatus)null);
		else if(actor instanceof ForeignGroup group)
			context.appContext.getGroupsController().updateStatus(group, (ActorStatus)null);
	}
}
