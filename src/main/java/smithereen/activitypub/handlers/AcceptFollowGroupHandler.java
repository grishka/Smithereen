package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.exceptions.ObjectNotFoundException;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.data.ForeignGroup;
import smithereen.data.User;
import smithereen.storage.GroupStorage;
import smithereen.storage.UserStorage;

public class AcceptFollowGroupHandler extends NestedActivityTypeHandler<ForeignGroup, Accept, Follow, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Accept activity, Follow nested, User object) throws SQLException{
		User follower=UserStorage.getUserByActivityPubID(nested.actor.link);
		if(follower==null)
			throw new ObjectNotFoundException("Follower not found");
		GroupStorage.setMemberAccepted(actor.id, follower.id, true);
	}
}
