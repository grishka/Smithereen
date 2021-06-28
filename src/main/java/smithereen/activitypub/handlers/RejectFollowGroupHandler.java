package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.User;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.GroupStorage;
import smithereen.storage.UserStorage;

public class RejectFollowGroupHandler extends NestedActivityTypeHandler<ForeignGroup, Reject, Follow, ForeignGroup>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Reject activity, Follow nested, ForeignGroup object) throws SQLException{
		User follower=UserStorage.getUserByActivityPubID(nested.actor.link);
		if(follower==null)
			throw new ObjectNotFoundException("Follower not found");
		follower.ensureLocal();
		Group.MembershipState state=GroupStorage.getUserMembershipState(actor.id, follower.id);
		GroupStorage.leaveGroup(actor, follower.id, false);
		if(state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER){
			ActivityPubWorker.getInstance().sendRemoveFromGroupsCollectionActivity(follower, actor);
		}
	}
}
