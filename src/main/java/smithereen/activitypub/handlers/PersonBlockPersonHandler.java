package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Block;
import smithereen.data.ForeignUser;
import smithereen.data.User;
import smithereen.storage.UserStorage;

public class PersonBlockPersonHandler extends ActivityTypeHandler<ForeignUser, Block, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Block activity, User object) throws SQLException{
		UserStorage.blockUser(actor.id, object.id);
	}
}
