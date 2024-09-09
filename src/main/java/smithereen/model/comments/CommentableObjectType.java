package smithereen.model.comments;

import smithereen.model.feed.CommentsNewsfeedObjectType;

public enum CommentableObjectType{
	PHOTO;

	public CommentsNewsfeedObjectType newsfeedType(){
		return switch(this){
			case PHOTO -> CommentsNewsfeedObjectType.PHOTO;
		};
	}

	public int getMaxAttachments(){
		return switch(this){
			case PHOTO -> 2;
		};
	}
}
