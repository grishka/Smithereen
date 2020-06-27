package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.BadRequestException;
import smithereen.Config;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.User;
import smithereen.data.notifications.Notification;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.UserStorage;

public class FollowPersonHandler extends ActivityTypeHandler<ForeignUser, Follow, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Follow activity, User user) throws SQLException{
		if(user instanceof ForeignUser)
			throw new BadRequestException("Follow is only supported for local users");
		FriendshipStatus status=UserStorage.getFriendshipStatus(actor.id, user.id);
		if(status==FriendshipStatus.FRIENDS || status==FriendshipStatus.REQUEST_SENT || status==FriendshipStatus.FOLLOWING){
			throw new BadRequestException("Already following");
		}
		UserStorage.followUser(actor.id, user.id, true);
		UserStorage.deleteFriendRequest(actor.id, user.id);

		ActivityPubWorker.getInstance().sendAcceptFollowActivity(actor, user, activity);

		Notification n=new Notification();
		n.type=status==FriendshipStatus.REQUEST_RECVD ? Notification.Type.FRIEND_REQ_ACCEPT : Notification.Type.FOLLOW;
		n.actorID=actor.id;
		NotificationsStorage.putNotification(user.id, n);
	}
}
