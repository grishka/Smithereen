package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.Utils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Join;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.storage.GroupStorage;

public class FollowGroupHandler extends ActivityTypeHandler<ForeignUser, Follow, Group>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Follow activity, Group group) throws SQLException{
		if(group instanceof ForeignGroup)
			throw new BadRequestException("Follow is only supported for local groups");
		Utils.ensureUserNotBlocked(actor, group);

		boolean tentative=group.isEvent() && activity instanceof Join j && j.tentative;

		Group.MembershipState state=GroupStorage.getUserMembershipState(group.id, actor.id);
		if(state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER){
			// send an Accept{Follow} once again because the other server apparently didn't get it the first time
			// why would it resend a Follow otherwise?
			context.appContext.getActivityPubWorker().sendAcceptFollowActivity(actor, group, activity);
			// update the event decision locally if it changed
			if((tentative && state==Group.MembershipState.MEMBER) || (!tentative && state==Group.MembershipState.TENTATIVE_MEMBER))
				GroupStorage.updateUserEventDecision(group, actor.id, tentative);
			return;
		}

		boolean accept=true;
		if(group.accessType==Group.AccessType.CLOSED){
			accept=state==Group.MembershipState.INVITED;
		}else if(group.accessType==Group.AccessType.PRIVATE && state!=Group.MembershipState.INVITED){
			throw new UserActionNotAllowedException("Can't join a private group without an invitation");
		}

		GroupStorage.joinGroup(group, actor.id, tentative, accept);
		if(accept)
			context.appContext.getActivityPubWorker().sendAcceptFollowActivity(actor, group, activity);
	}
}
