package smithereen.api.methods;

import java.util.Arrays;

import smithereen.ApplicationContext;
import smithereen.api.ApiCallContext;
import smithereen.model.UserPresence;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.notifications.UserNotifications;

public class AccountMethods{
	public static Object getCounters(ApplicationContext ctx, ApiCallContext actx){
		UserNotifications notifications=ctx.getNotificationsController().getUserCounters(actx.self);
		record Counters(int friends, int notifications, int groups, int events, int photos){}
		return new Counters(
				notifications.getNewFriendRequestCount(),
				notifications.getNewNotificationsCount(),
				notifications.getNewGroupInvitationsCount(),
				notifications.getNewEventInvitationsCount(),
				notifications.getNewPhotoTagCount()
		);
	}

	public static Object setOnline(ApplicationContext ctx, ApiCallContext actx){
		ctx.getUsersController().setOnline(actx.self.user, actx.booleanParam("mobile") ? UserPresence.PresenceType.MOBILE_API : UserPresence.PresenceType.API,
				Arrays.hashCode(actx.token.id()), actx.token.appID());
		return true;
	}

	public static Object setOffline(ApplicationContext ctx, ApiCallContext actx){
		ctx.getUsersController().setOffline(actx.self.user, Arrays.hashCode(actx.token.id()));
		return true;
	}

	public static Object getAppPermissions(ApplicationContext ctx, ApiCallContext actx){
		return actx.token.permissions().stream().map(ClientAppPermission::getScopeValue).toList();
	}
}
