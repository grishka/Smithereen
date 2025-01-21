package smithereen.model.comments;

import java.net.URI;

import smithereen.ApplicationContext;
import smithereen.model.OwnedContentObject;
import smithereen.model.photos.Photo;

public sealed interface CommentableContentObject extends OwnedContentObject, CommentReplyParent permits Photo{
	CommentParentObjectID getCommentParentID();
	String getURL();
	URI getActivityPubID();
	URI getCommentCollectionID(ApplicationContext context);
}
