package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.UserStorage;

public class AcceptFollowPersonHandler extends NestedActivityTypeHandler<ForeignUser, Accept, Follow, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Accept activity, Follow nested, User object) throws SQLException{
		User follower=UserStorage.getUserByActivityPubID(nested.actor.link);
		if(follower==null)
			throw new ObjectNotFoundException("Follower not found");
		follower.ensureLocal();
		UserStorage.setFollowAccepted(follower.id, actor.id, true);
		FriendshipStatus status=UserStorage.getFriendshipStatus(follower.id, actor.id);
		if(status==FriendshipStatus.FRIENDS){
			context.appContext.getActivityPubWorker().sendAddToFriendsCollectionActivity(follower, actor);
			NewsfeedStorage.putEntry(follower.id, actor.id, NewsfeedEntry.Type.ADD_FRIEND, null);
		}
		if(UserStorage.getLocalFollowersCount(actor.id)==1){
			context.appContext.getActivityPubWorker().fetchActorRelationshipCollections(actor);
			context.appContext.getActivityPubWorker().fetchActorContentCollections(actor);
		}
	}
}
