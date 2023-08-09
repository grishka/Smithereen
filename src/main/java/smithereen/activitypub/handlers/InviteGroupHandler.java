package smithereen.activitypub.handlers;

import java.sql.SQLException;
import java.util.Objects;

import smithereen.Utils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Invite;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.User;
import smithereen.data.UserNotifications;
import smithereen.data.UserPrivacySettingKey;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.storage.GroupStorage;
import smithereen.storage.NotificationsStorage;

public class InviteGroupHandler extends ActivityTypeHandler<ForeignUser, Invite, Group>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Invite invite, Group object) throws SQLException{
		Utils.ensureUserNotBlocked(actor, object);
		if(!(object instanceof ForeignGroup)){
			Group.MembershipState inviterState=context.appContext.getGroupsController().getUserMembershipState(object, actor);
			if(inviterState!=Group.MembershipState.MEMBER && inviterState!=Group.MembershipState.TENTATIVE_MEMBER)
				throw new BadRequestException("Inviter must be a member of this group");
		}
		if(invite.to==null || invite.to.size()!=1 || invite.to.get(0).link==null)
			throw new BadRequestException("Invite.to must have exactly 1 element and it must be a user ID");
		User user=context.appContext.getObjectLinkResolver().resolve(invite.to.get(0).link, User.class, true, true, false);
		Utils.ensureUserNotBlocked(actor, user);
		context.appContext.getPrivacyController().enforceUserPrivacy(actor, user, UserPrivacySettingKey.GROUP_INVITE);
		if(object.id==0)
			context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(object);
		context.appContext.getGroupsController().runLocked(()->{
			Group.MembershipState state=context.appContext.getGroupsController().getUserMembershipState(object, user);
			if(state!=Group.MembershipState.NONE)
				throw new BadRequestException("Can only invite users who aren't members of this group and don't have a pending invitation to it");
			try{
				GroupStorage.putInvitation(object.id, actor.id, user.id, object.isEvent(), Objects.toString(invite.activityPubID, null));
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
		});

		if(!(user instanceof ForeignUser)){
			UserNotifications notifications=NotificationsStorage.getNotificationsFromCache(user.id);
			if(notifications!=null){
				if(object.isEvent())
					notifications.incNewEventInvitationsCount(1);
				else
					notifications.incNewGroupInvitationsCount(1);
			}
		}
	}
}
