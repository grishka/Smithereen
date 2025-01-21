package smithereen.model.feed;

public enum FriendsNewsfeedTypeFilter{
	POSTS,
	PHOTOS,
	FRIENDS,
	GROUPS,
	EVENTS,
	PHOTO_TAGS,
	PERSONAL_INFO;

	public String getLangKey(){
		return switch(this){
			case POSTS -> "feed_type_posts";
			case PHOTOS -> "feed_type_photos";
			case FRIENDS -> "feed_type_friends";
			case GROUPS -> "feed_type_groups";
			case EVENTS -> "feed_type_events";
			case PHOTO_TAGS -> "feed_type_tags";
			case PERSONAL_INFO -> "feed_type_personal";
		};
	}
}
