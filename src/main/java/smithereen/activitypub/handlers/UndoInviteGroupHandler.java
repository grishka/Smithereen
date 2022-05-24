package smithereen.activitypub.handlers;

import java.sql.SQLException;
import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Invite;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.data.ForeignGroup;
import smithereen.data.User;
import smithereen.exceptions.BadRequestException;
import smithereen.storage.GroupStorage;

public class UndoInviteGroupHandler extends NestedActivityTypeHandler<ForeignGroup, Undo, Invite, ForeignGroup>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup group, Undo undo, Invite invite, ForeignGroup object) throws SQLException{
		if(!Objects.equals(group.activityPubID, object.activityPubID))
			throw new BadRequestException("Groups can only undo invites to themselves");

		if(invite.to==null || invite.to.size()!=1 || invite.to.get(0).link==null)
			throw new BadRequestException("Invite.to must have exactly 1 element and it must be a user ID");
		User user=context.appContext.getObjectLinkResolver().resolve(invite.to.get(0).link, User.class, false, false, false);
		user.ensureLocal();

		GroupStorage.deleteInvitation(user.id, group.id, group.isEvent());
	}
}
