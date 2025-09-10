package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.exceptions.BadRequestException;
import smithereen.model.ForeignGroup;

public class DeleteGroupHandler extends ActivityTypeHandler<ForeignGroup, Delete, ForeignGroup>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Delete activity, ForeignGroup object) throws SQLException{
		if(actor.id!=object.id)
			throw new BadRequestException("Group can only delete itself");
		LOG.debug("Deleting foreign group {}", actor.activityPubID);
		context.appContext.getGroupsController().deleteForeignGroup(actor);
		LOG.debug("Successfully deleted foreign group {}", actor.activityPubID);
	}
}
