package smithereen.model.notifications;

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
	GROUP_INVITE;

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
		};
	}
}
