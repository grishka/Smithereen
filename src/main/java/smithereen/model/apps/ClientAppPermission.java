package smithereen.model.apps;

public enum ClientAppPermission{
	PASSWORD_GRANT_USED,
	FRIENDS_READ,
	FRIENDS_WRITE,
	PHOTOS_READ,
	PHOTOS_WRITE,
	ACCOUNT_WRITE,
	WALL_READ,
	WALL_WRITE,
	GROUPS_READ,
	GROUPS_WRITE,
	MESSAGES_READ,
	MESSAGES_WRITE,
	LIKES_READ,
	LIKES_WRITE,
	NEWSFEED,
	NOTIFICATIONS,
	OFFLINE;

	public String getScopeValue(){
		return switch(this){
			case PASSWORD_GRANT_USED -> "";
			case FRIENDS_READ -> "friends:read";
			case FRIENDS_WRITE -> "friends:write";
			case PHOTOS_READ -> "photos:read";
			case PHOTOS_WRITE -> "photos:write";
			case ACCOUNT_WRITE -> "account";
			case WALL_READ -> "wall:read";
			case WALL_WRITE -> "wall:write";
			case GROUPS_READ -> "groups:read";
			case GROUPS_WRITE -> "groups:write";
			case MESSAGES_READ -> "messages:read";
			case MESSAGES_WRITE -> "messages:write";
			case LIKES_READ -> "likes:read";
			case LIKES_WRITE -> "likes:write";
			case NEWSFEED -> "newsfeed";
			case NOTIFICATIONS -> "notifications";
			case OFFLINE -> "offline";
		};
	}
}
