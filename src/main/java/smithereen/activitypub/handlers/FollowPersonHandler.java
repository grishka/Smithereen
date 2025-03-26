package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.Utils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.model.ForeignUser;
import smithereen.model.FriendshipStatus;
import smithereen.model.User;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.notifications.Notification;
import smithereen.exceptions.BadRequestException;
import smithereen.storage.UserStorage;

public class FollowPersonHandler extends ActivityTypeHandler<ForeignUser, Follow, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Follow activity, User user) throws SQLException{
		if(user instanceof ForeignUser)
			throw new BadRequestException("Follow is only supported for local users");
		Utils.ensureUserNotBlocked(actor, user);
		FriendshipStatus status=UserStorage.getFriendshipStatus(actor.id, user.id);
		if(status==FriendshipStatus.FRIENDS || status==FriendshipStatus.REQUEST_SENT || status==FriendshipStatus.FOLLOWING){
			LOG.debug("{} already follows {}, resending Accept{Follow} for {}", actor.activityPubID, user.activityPubID, activity.activityPubID);
			context.appContext.getActivityPubWorker().sendAcceptFollowActivity(actor, user, activity);
			return;
		}
		UserStorage.followUser(actor.id, user.id, true, false, true);
		UserStorage.deleteFriendRequest(actor.id, user.id);

		context.appContext.getActivityPubWorker().sendAcceptFollowActivity(actor, user, activity);

		context.appContext.getNotificationsController().createNotification(user, status==FriendshipStatus.REQUEST_RECVD ? Notification.Type.FRIEND_REQ_ACCEPT : Notification.Type.FOLLOW, null, null, actor);

		if(status==FriendshipStatus.REQUEST_RECVD || status==FriendshipStatus.FOLLOWED_BY){
			context.appContext.getActivityPubWorker().sendAddToFriendsCollectionActivity(user, actor);
			context.appContext.getNewsfeedController().putFriendsFeedEntry(user, actor.id, NewsfeedEntry.Type.ADD_FRIEND);
		}
	}
}
