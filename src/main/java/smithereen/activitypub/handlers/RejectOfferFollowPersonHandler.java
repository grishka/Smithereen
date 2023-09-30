package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.exceptions.ObjectNotFoundException;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.DoublyNestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Offer;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.model.ForeignUser;
import smithereen.model.User;
import smithereen.storage.UserStorage;

public class RejectOfferFollowPersonHandler extends DoublyNestedActivityTypeHandler<ForeignUser, Reject, Offer, Follow, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Reject reject, Offer offer, Follow follow, User object) throws SQLException{
		if(follow.object.link==null)
			throw new IllegalArgumentException("follow.object must be a link");
		if(follow.actor.link==null)
			throw new IllegalArgumentException("follow.actor must be a link");
		if(!follow.actor.link.equals(actor.activityPubID))
			throw new IllegalArgumentException("follow.object must match reject.actor");
		User user=UserStorage.getUserByActivityPubID(follow.object.link);
		if(user==null)
			throw new ObjectNotFoundException("User not found");
		UserStorage.deleteFriendRequest(actor.id, user.id);
	}
}
