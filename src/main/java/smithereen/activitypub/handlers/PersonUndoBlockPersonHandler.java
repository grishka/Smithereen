package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Block;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.model.ForeignUser;
import smithereen.model.User;
import smithereen.storage.UserStorage;

public class PersonUndoBlockPersonHandler extends NestedActivityTypeHandler<ForeignUser, Undo, Block, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Undo activity, Block nested, User object) throws SQLException{
		UserStorage.unblockUser(actor.id, object.id);
	}
}
