package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.data.ForeignUser;
import smithereen.data.User;
import smithereen.storage.UserStorage;

public class UndoFollowPersonHandler extends NestedActivityTypeHandler<ForeignUser, Undo, Follow, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Undo activity, Follow nested, User user) throws SQLException{
		UserStorage.unfriendUser(actor.id, user.id);
		System.out.println(actor.getFullUsername()+" remotely unfollowed "+user.getFullUsername());
	}
}
