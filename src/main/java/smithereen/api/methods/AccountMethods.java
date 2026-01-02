package smithereen.api.methods;

import java.util.Arrays;
import java.util.List;

import smithereen.ApplicationContext;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiPaginatedList;
import smithereen.model.PaginatedList;
import smithereen.model.User;
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

	public static Object getBannedUsers(ApplicationContext ctx, ApiCallContext actx){
		PaginatedList<User> users=ctx.getPrivacyController().getBlockedUsers(actx.self.user, actx.getOffset(), actx.getCount(100, 1000));
		return new ApiPaginatedList<>(users.total, ApiUtils.getUsers(users.list, ctx, actx));
	}

	public static Object banUser(ApplicationContext ctx, ApiCallContext actx){
		User user=ctx.getUsersController().getUserOrThrow(actx.requireParamIntPositive("user_id"));
		if(user.id==actx.self.user.id)
			throw actx.paramError("can't block self");
		ctx.getFriendsController().blockUser(actx.self.user, user);
		return true;
	}

	public static Object unbanUser(ApplicationContext ctx, ApiCallContext actx){
		User user=ctx.getUsersController().getUserOrThrow(actx.requireParamIntPositive("user_id"));
		ctx.getFriendsController().unblockUser(actx.self.user, user);
		return true;
	}

	public static Object getBannedDomains(ApplicationContext ctx, ApiCallContext actx){
		List<String> domains=ctx.getPrivacyController().getBlockedDomains(actx.self.user);
		int offset=actx.getOffset();
		int count=actx.getCount(100, 1000);
		if(offset>=domains.size())
			return new ApiPaginatedList<>(0, List.of());
		return new ApiPaginatedList<>(domains.size(), domains.subList(offset, Math.min(offset+count, domains.size())));
	}

	public static Object banDomain(ApplicationContext ctx, ApiCallContext actx){
		String domain=actx.requireParamString("domain");
		if(!domain.matches("^([a-zA-Z0-9-]+\\.)+[a-zA-Z0-9-]{2,}$"))
			throw actx.paramError("invalid domain");
		ctx.getPrivacyController().blockDomain(actx.self.user, domain);
		return true;
	}

	public static Object unbanDomain(ApplicationContext ctx, ApiCallContext actx){
		ctx.getPrivacyController().unblockDomain(actx.self.user, actx.requireParamString("domain"));
		return true;
	}

	public static Object revokeToken(ApplicationContext ctx, ApiCallContext actx){
		ctx.getAppsController().revokeAccessToken(actx.token.id());
		return true;
	}
}
