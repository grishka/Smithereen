package smithereen.model.comments;

import smithereen.model.feed.CommentsNewsfeedObjectType;

public enum CommentableObjectType{
	PHOTO,
	BOARD_TOPIC;

	public CommentsNewsfeedObjectType newsfeedType(){
		return switch(this){
			case PHOTO -> CommentsNewsfeedObjectType.PHOTO;
			case BOARD_TOPIC -> CommentsNewsfeedObjectType.BOARD_TOPIC;
		};
	}

	public int getMaxAttachments(){
		return switch(this){
			case PHOTO -> 2;
			case BOARD_TOPIC -> 10;
		};
	}
}
