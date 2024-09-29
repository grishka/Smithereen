package smithereen.activitypub.objects;

import smithereen.model.comments.Comment;

public class LocalCommentNoteTombstone extends NoteTombstone{
	public final Comment nativeComment;

	public LocalCommentNoteTombstone(Comment nativeComment){
		this.nativeComment=nativeComment;
	}
}
