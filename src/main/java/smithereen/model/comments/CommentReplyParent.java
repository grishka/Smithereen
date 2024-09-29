package smithereen.model.comments;

public sealed interface CommentReplyParent permits CommentableContentObject, Comment{
	// This only exists to provide a common type for comments themselves and their parent objects
}
