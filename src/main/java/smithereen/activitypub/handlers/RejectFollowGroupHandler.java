package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.model.ForeignGroup;
import smithereen.model.Group;
import smithereen.model.User;
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
		GroupStorage.leaveGroup(actor, follower.id, false, state!=Group.MembershipState.REQUESTED);
		if(state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER){
			context.appContext.getActivityPubWorker().sendRemoveFromGroupsCollectionActivity(follower, actor);
		}
	}
}
