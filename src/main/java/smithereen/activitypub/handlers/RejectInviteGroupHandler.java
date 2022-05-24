package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Invite;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.User;
import smithereen.exceptions.BadRequestException;
import smithereen.storage.GroupStorage;

public class RejectInviteGroupHandler extends NestedActivityTypeHandler<ForeignUser, Reject, Invite, Group>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Reject activity, Invite invite, Group group) throws SQLException{
		group.ensureLocal();
		if(invite.to==null || invite.to.size()!=1 || invite.to.get(0).link==null)
			throw new BadRequestException("Invite.to must have exactly 1 element and it must be a user ID");
		if(!actor.activityPubID.equals(invite.to.get(0).link))
			throw new BadRequestException("Reject.actor must match Invite.to[0]");
		GroupStorage.deleteInvitation(actor.id, group.id, group.isEvent());
	}
}
