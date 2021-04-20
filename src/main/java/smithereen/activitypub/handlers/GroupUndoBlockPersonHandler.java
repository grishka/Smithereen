package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Block;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.User;
import smithereen.storage.GroupStorage;
import smithereen.storage.UserStorage;

public class GroupUndoBlockPersonHandler extends NestedActivityTypeHandler<ForeignGroup, Undo, Block, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Undo activity, Block nested, User object) throws SQLException{
		GroupStorage.unblockUser(actor.id, object.id);
	}
}
