package smithereen.model.feed;

public enum GroupsNewsfeedTypeFilter{
	POSTS,
	TOPICS,
	PHOTOS;

	public String getLangKey(){
		return switch(this){
			case POSTS -> "feed_type_posts";
			case TOPICS -> "feed_type_board_topics";
			case PHOTOS -> "feed_type_photos";
		};
	}
}
