package smithereen.model.notifications;

public record RealtimeNotification(String id, Type type, ObjectType objectType, String objectID, Integer actorID, String title, String content, String url, ImageURLs avatar, ImageURLs image){
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
	}

	public enum ObjectType{
		POST,
		PHOTO,
		PHOTO_COMMENT,
		MESSAGE
	}
}
