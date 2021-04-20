package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.Utils;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Offer;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.User;
import smithereen.storage.UserStorage;

public class OfferFollowPersonHandler extends NestedActivityTypeHandler<ForeignUser, Offer, Follow, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Offer activity, Follow nested, User object) throws SQLException{
		User user=UserStorage.getUserByActivityPubID(nested.actor.link);
		if(user==null || user instanceof ForeignUser)
			throw new ObjectNotFoundException("User not found");
		Utils.ensureUserNotBlocked(actor, user);

		FriendshipStatus status=UserStorage.getFriendshipStatus(actor.id, user.id);
		if(status==FriendshipStatus.NONE || status==FriendshipStatus.FOLLOWING){
			UserStorage.putFriendRequest(actor.id, user.id, activity.content, true);
		}else if(status==FriendshipStatus.FRIENDS){
			throw new BadRequestException("Already friends");
		}else if(status==FriendshipStatus.REQUEST_RECVD){
			throw new BadRequestException("Incoming friend request already received");
		}else{ // REQ_SENT
			throw new BadRequestException("Friend request already sent");
		}
	}
}
