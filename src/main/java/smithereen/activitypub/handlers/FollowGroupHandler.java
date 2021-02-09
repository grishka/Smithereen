package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.BadRequestException;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.storage.GroupStorage;

public class FollowGroupHandler extends ActivityTypeHandler<ForeignUser, Follow, Group>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Follow activity, Group group) throws SQLException{
		if(group instanceof ForeignGroup)
			throw new BadRequestException("Follow is only supported for local groups");

		Group.MembershipState state=GroupStorage.getUserMembershipState(group.id, actor.id);
		if(state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER){
			// send an Accept{Follow} once again because the other server apparently didn't get it the first time
			// why would it resend a Follow otherwise?
			ActivityPubWorker.getInstance().sendAcceptFollowActivity(actor, group, activity);
			return;
		}
		GroupStorage.joinGroup(group, actor.id, false, true);
		ActivityPubWorker.getInstance().sendAcceptFollowActivity(actor, group, activity);
	}
}
