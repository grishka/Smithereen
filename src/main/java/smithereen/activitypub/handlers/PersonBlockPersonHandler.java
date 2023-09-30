package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Block;
import smithereen.model.ForeignUser;
import smithereen.model.FriendshipStatus;
import smithereen.model.User;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.storage.UserStorage;

public class PersonBlockPersonHandler extends ActivityTypeHandler<ForeignUser, Block, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Block activity, User object) throws SQLException{
		object.ensureLocal();
		FriendshipStatus status=UserStorage.getFriendshipStatus(object.id, actor.id);
		UserStorage.blockUser(actor.id, object.id);
		if(status==FriendshipStatus.FRIENDS){
			context.appContext.getActivityPubWorker().sendRemoveFromFriendsCollectionActivity(object, actor);
			context.appContext.getNewsfeedController().deleteFriendsFeedEntry(object, actor.id, NewsfeedEntry.Type.ADD_FRIEND);
		}
	}
}
