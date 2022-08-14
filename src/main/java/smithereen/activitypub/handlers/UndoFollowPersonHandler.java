package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.UserStorage;

public class UndoFollowPersonHandler extends NestedActivityTypeHandler<ForeignUser, Undo, Follow, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Undo activity, Follow nested, User user) throws SQLException{
		user.ensureLocal();
		FriendshipStatus status=UserStorage.getFriendshipStatus(actor.id, user.id);
		UserStorage.unfriendUser(actor.id, user.id);
		if(status==FriendshipStatus.FRIENDS){
			context.appContext.getActivityPubWorker().sendRemoveFromFriendsCollectionActivity(user, actor);
			context.appContext.getNewsfeedController().deleteFriendsFeedEntry(user, actor.id, NewsfeedEntry.Type.ADD_FRIEND);
		}
	}
}
