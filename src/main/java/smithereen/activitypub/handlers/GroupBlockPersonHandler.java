package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Block;
import smithereen.data.ForeignGroup;
import smithereen.data.User;
import smithereen.storage.GroupStorage;

public class GroupBlockPersonHandler extends ActivityTypeHandler<ForeignGroup, Block, User>{

	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Block activity, User object) throws SQLException{
		object.ensureLocal();
		GroupStorage.blockUser(actor.id, object.id);
		context.appContext.getActivityPubWorker().sendRemoveFromGroupsCollectionActivity(object, actor);
	}
}
