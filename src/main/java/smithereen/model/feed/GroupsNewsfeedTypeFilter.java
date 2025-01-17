package smithereen.model.feed;

public enum GroupsNewsfeedTypeFilter{
	POSTS,
	PHOTOS;

	public String getLangKey(){
		return switch(this){
			case POSTS -> "feed_type_posts";
			case PHOTOS -> "feed_type_photos";
		};
	}
}
