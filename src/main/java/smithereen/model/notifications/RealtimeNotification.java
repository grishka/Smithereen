package smithereen.model.notifications;

import java.util.Map;

public record RealtimeNotification(String id, Type type, ObjectType objectType, String objectID, Integer actorID, String title, String content, String url, ImageURLs avatar, ImageURLs image, Map<String, String> linkExtraAttrs){
	public record ImageURLs(String jpeg1x, String webp1x, String jpeg2x, String webp2x){}
	public enum Type{
		REPLY,
		LIKE,
		MENTION,
		REPOST,
		WALL_POST,
		INVITE_SIGNUP,
		FOLLOW,
		FRIEND_REQUEST,
		FRIEND_REQUEST_ACCEPTED,
		MAIL_MESSAGE,
		GROUP_INVITE,
		EVENT_INVITE,
		GROUP_REQUEST_ACCEPTED,
		PHOTO_TAG;

		public RealtimeNotificationSettingType getSettingType(){
			return switch(this){
				case REPLY -> RealtimeNotificationSettingType.REPLIES;
				case LIKE -> RealtimeNotificationSettingType.LIKES;
				case MENTION -> RealtimeNotificationSettingType.MENTIONS;
				case REPOST -> RealtimeNotificationSettingType.REPOSTS;
				case WALL_POST -> RealtimeNotificationSettingType.WALL;
				case INVITE_SIGNUP, FRIEND_REQUEST_ACCEPTED, FRIEND_REQUEST, FOLLOW -> RealtimeNotificationSettingType.FRIEND_REQUESTS;
				case MAIL_MESSAGE -> RealtimeNotificationSettingType.MAIL;
				case GROUP_INVITE, GROUP_REQUEST_ACCEPTED, EVENT_INVITE -> RealtimeNotificationSettingType.GROUP_INVITES;
				case PHOTO_TAG -> RealtimeNotificationSettingType.PHOTO_TAGS;
			};
		}
	}

	public enum ObjectType{
		POST,
		PHOTO,
		PHOTO_COMMENT,
		MESSAGE,
		BOARD_COMMENT
	}
}
