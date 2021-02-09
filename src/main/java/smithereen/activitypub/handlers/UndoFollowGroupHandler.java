package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.BadRequestException;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.storage.GroupStorage;

public class UndoFollowGroupHandler extends NestedActivityTypeHandler<ForeignUser, Undo, Follow, Group>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Undo activity, Follow nested, Group group) throws SQLException{
		if(group instanceof ForeignGroup)
			throw new BadRequestException("Local group required here");
		Group.MembershipState state=GroupStorage.getUserMembershipState(group.id, actor.id);
		if(state!=Group.MembershipState.MEMBER && state!=Group.MembershipState.TENTATIVE_MEMBER){
			return;
		}
		GroupStorage.leaveGroup(group, actor.id, state==Group.MembershipState.TENTATIVE_MEMBER);
	}
}
