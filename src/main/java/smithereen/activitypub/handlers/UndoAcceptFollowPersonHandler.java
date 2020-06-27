package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.ObjectNotFoundException;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.DoublyNestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.data.ForeignUser;
import smithereen.data.User;
import smithereen.storage.UserStorage;

public class UndoAcceptFollowPersonHandler extends DoublyNestedActivityTypeHandler<ForeignUser, Undo, Accept, Follow, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Undo undo, Accept accept, Follow follow, User object) throws SQLException{
		User follower=UserStorage.getUserByActivityPubID(follow.actor.link);
		if(follower==null)
			throw new ObjectNotFoundException("Follower not found");
		UserStorage.setFollowAccepted(follower.id, actor.id, false);
	}
}
