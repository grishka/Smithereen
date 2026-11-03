package smithereen.model.notifications;

import smithereen.model.UserPermissions;
import smithereen.model.admin.UserRole;
import smithereen.util.TranslatableEnum;

public enum EmailNotificationType implements TranslatableEnum<EmailNotificationType>{
	FRIEND_REQUEST,
	MAIL,
	PHOTO_TAG,
	WALL_POST,
	WALL_COMMENT,
	PHOTO_COMMENT,
	COMMENT_REPLY,
	MENTION,
	GROUP_INVITE,

	// Admin-only
	SIGNUP_REQUEST;

	@Override
	public String getLangKey(){
		return "settings_notifications_"+switch(this){
			case FRIEND_REQUEST -> "friend_requests";
			case MAIL -> "mail";
			case PHOTO_TAG -> "photo_tags";
			case WALL_POST -> "wall";
			case WALL_COMMENT -> "wall_comments";
			case PHOTO_COMMENT -> "photo_comments";
			case COMMENT_REPLY -> "replies";
			case MENTION -> "mentions";
			case GROUP_INVITE -> "group_invites";
			case SIGNUP_REQUEST -> "signup_requests";
		};
	}

	public boolean isAvailable(UserPermissions permissions){
		if(this==SIGNUP_REQUEST){
			return permissions.hasPermission(UserRole.Permission.MANAGE_INVITES);
		}
		return true;
	}
}
