package smithereen.model.notifications;

import smithereen.model.UserPermissions;
import smithereen.model.admin.UserRole;
import smithereen.util.TranslatableEnum;

// This exists separately from RealtimeNotification.Type because some finer-grained notification types
// are grouped together in the UI, like friend requests and new followers correspond to FRIEND_REQUESTS here.
public enum RealtimeNotificationSettingType implements TranslatableEnum<RealtimeNotificationSettingType>{
	MAIL,
	FRIEND_REQUESTS,
	LIKES,
	REPOSTS,
	REPLIES,
	WALL,
	MENTIONS,
	PHOTO_TAGS,
	GROUP_INVITES,

	// Admin-only
	SIGNUP_REQUESTS;

	@Override
	public String getLangKey(){
		return "settings_notifications_"+toString().toLowerCase();
	}

	public boolean isAvailable(UserPermissions permissions){
		if(this==SIGNUP_REQUESTS){
			return permissions.hasPermission(UserRole.Permission.MANAGE_INVITES);
		}
		return true;
	}
}
