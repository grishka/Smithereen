package smithereen.data.feed;

import java.time.Instant;

import smithereen.data.User;

public abstract class NewsfeedEntry{
	public int id;
	public Type type;
	public int objectID;
	public int authorID;
	public User author;
	public Instant time;

	@Override
	public String toString(){
		return "NewsfeedEntry{"+
				"id="+id+
				", type="+type+
				", objectID="+objectID+
				", authorID="+authorID+
				", author="+author+
				", time="+time+
				'}';
	}

	public enum Type{
		/**
		 * New post. objectID is a post
		 */
		POST,
		/**
		 * Mastodon-style repost. objectID is a post
		 */
		RETOOT,
		/**
		 * Someone added a friend. objectID is a user
		 */
		ADD_FRIEND,
		/**
		 * Someone joined a group. objectID is a group
		 */
		JOIN_GROUP,
	}
}
