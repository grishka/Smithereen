package smithereen.model.comments;

import java.net.URI;

import smithereen.model.OwnedContentObject;
import smithereen.model.photos.Photo;

public sealed interface CommentableContentObject extends OwnedContentObject permits Photo{
	CommentParentObjectID getCommentParentID();
	String getURL();
}
