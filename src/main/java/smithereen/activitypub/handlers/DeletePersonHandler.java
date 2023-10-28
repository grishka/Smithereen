package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.exceptions.BadRequestException;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.model.ForeignUser;

public class DeletePersonHandler extends ActivityTypeHandler<ForeignUser, Delete, ForeignUser>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Delete activity, ForeignUser object) throws SQLException{
		if(actor.id!=object.id)
			throw new BadRequestException("User can only delete themselves");
		LOG.debug("Deleting foreign user {}", actor.activityPubID);
		context.appContext.getUsersController().deleteForeignUser(actor);
		LOG.debug("Successfully deleted foreign user {}", actor.activityPubID);
	}
}
