package smithereen.data.notifications;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class Notification{
	public int id;
	public Type type;
	public ObjectType objectType;
	public int objectID;
	public ObjectType relatedObjectType;
	public int relatedObjectID;
	public int actorID;
	public Timestamp time;

	public static Notification fromResultSet(ResultSet res) throws SQLException{
		Notification n=new Notification();
		n.id=res.getInt("id");
		n.type=Type.values()[res.getInt("type")];
		n.objectID=res.getInt("object_id");
		if(!res.wasNull())
			n.objectType=ObjectType.values()[res.getInt("object_type")];
		n.relatedObjectID=res.getInt("related_object_id");
		if(!res.wasNull())
			n.relatedObjectType=ObjectType.values()[res.getInt("related_object_type")];
		n.actorID=res.getInt("actor_id");
		n.time=res.getTimestamp("time");
		return n;
	}


	@Override
	public String toString(){
		return "Notification{"+
				"id="+id+
				", type="+type+
				", objectType="+objectType+
				", objectID="+objectID+
				", relatedObjectType="+relatedObjectType+
				", relatedObjectID="+relatedObjectID+
				", actorID="+actorID+
				", time="+time+
				'}';
	}

	public enum Type{
		/**
		 * %username% replied to you
		 */
		REPLY,
		/**
		 * %username% liked your %object%
		 */
		LIKE,
		/**
		 * %username% mentioned you in their %object%
		 */
		MENTION,
		/**
		 * %username% reposted (Mastodon style) your post
		 */
		RETOOT,
		/**
		 * %username% posted on your wall
		 */
		POST_OWN_WALL
	}

	public enum ObjectType{
		POST
	}
}
