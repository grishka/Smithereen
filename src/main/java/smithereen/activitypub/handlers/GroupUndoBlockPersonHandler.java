package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Block;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.model.ForeignGroup;
import smithereen.model.User;
import smithereen.storage.GroupStorage;

public class GroupUndoBlockPersonHandler extends NestedActivityTypeHandler<ForeignGroup, Undo, Block, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Undo activity, Block nested, User object) throws SQLException{
		GroupStorage.unblockUser(actor.id, object.id);
	}
}
