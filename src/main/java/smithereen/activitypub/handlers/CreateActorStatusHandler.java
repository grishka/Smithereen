package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubActorStatus;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Create;
import smithereen.exceptions.BadRequestException;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;

public class CreateActorStatusHandler extends ActivityTypeHandler<Actor, Create, ActivityPubActorStatus>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Create activity, ActivityPubActorStatus object) throws SQLException{
		if(!actor.activityPubID.equals(object.attributedTo))
			throw new BadRequestException("status.attributedTo must match actor ID");

		if(actor instanceof ForeignUser user)
			context.appContext.getUsersController().updateStatus(user, object.asNativeStatus());
		else if(actor instanceof ForeignGroup group)
			context.appContext.getGroupsController().updateStatus(group, object.asNativeStatus());
	}
}
