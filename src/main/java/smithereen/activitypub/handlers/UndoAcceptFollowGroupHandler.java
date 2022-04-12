package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.DoublyNestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.data.ForeignGroup;
import smithereen.data.Group;
import smithereen.data.User;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.GroupStorage;
import smithereen.storage.UserStorage;

public class UndoAcceptFollowGroupHandler extends DoublyNestedActivityTypeHandler<ForeignGroup, Undo, Accept, Follow, Group>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Undo undo, Accept accept, Follow follow, Group object) throws SQLException{
		User follower=UserStorage.getUserByActivityPubID(follow.actor.link);
		if(follower==null)
			throw new ObjectNotFoundException("Follower not found");
		follower.ensureLocal();
		Group.MembershipState state=GroupStorage.getUserMembershipState(actor.id, follower.id);
		GroupStorage.setMemberAccepted(actor.id, follower.id, false);
		if(state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER){
			context.appContext.getActivityPubWorker().sendRemoveFromGroupsCollectionActivity(follower, actor);
		}
	}
}
