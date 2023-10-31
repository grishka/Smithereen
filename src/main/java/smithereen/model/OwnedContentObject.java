package smithereen.model;

/**
 * A piece of content, e.g. a post, that has an owner ID (userID or -groupID) and an author ID.
 */
public interface OwnedContentObject{
	int getOwnerID();
	int getAuthorID();
}
