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

	public boolean isNonPost(){
		return type!=Type.POST && type!=Type.RETOOT;
	}

	public boolean canBeGrouped(){
		return type==Type.ADD_FRIEND || type==Type.JOIN_GROUP || type==Type.JOIN_EVENT;
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
		/**
		 * Someone joined an event. objectID is a group
		 */
		JOIN_EVENT,
		CREATE_GROUP,
		CREATE_EVENT,
		/**
		 * Multiple entries of the same type, within the same day, grouped together
		 */
		GROUPED,
	}
}
