package smithereen.activitypub.objects;

import smithereen.model.comments.Comment;

public class LocalCommentNote extends Note{
	public final Comment nativeComment;

	public LocalCommentNote(Comment nativeComment){
		this.nativeComment=nativeComment;
	}
}
