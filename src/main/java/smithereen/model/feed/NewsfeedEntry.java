package smithereen.model.feed;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import smithereen.storage.DatabaseUtils;

public class NewsfeedEntry{
	public int id;
	public Type type;
	public long objectID;
	public int authorID;
	public Instant time;
	public Map<String, Object> extraData=Map.of();

	public static NewsfeedEntry fromResultSet(ResultSet res) throws SQLException{
		NewsfeedEntry e=new NewsfeedEntry();
		e.id=res.getInt("id");
		e.type=Type.values()[res.getInt("type")];
		e.objectID=res.getLong("object_id");
		e.authorID=res.getInt("author_id");
		e.time=DatabaseUtils.getInstant(res, "time");
		return e;
	}

	@Override
	public String toString(){
		return "NewsfeedEntry{"+
				"id="+id+
				", type="+type+
				", objectID="+objectID+
				", authorID="+authorID+
				", time="+time+
				'}';
	}

	public boolean isNonPost(){
		return type!=Type.POST && type!=Type.RETOOT;
	}

	public boolean canBeGrouped(){
		return type==Type.ADD_FRIEND || type==Type.JOIN_GROUP || type==Type.JOIN_EVENT || type==Type.ADD_PHOTO || type==Type.PHOTO_TAG;
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
		/**
		 * Someone added a photo to an album
		 */
		ADD_PHOTO,
		/**
		 * Photo for the comments newsfeed
		 */
		PHOTO,
		/**
		 * Someone was tagged in a photo
		 */
		PHOTO_TAG,
	}
}
