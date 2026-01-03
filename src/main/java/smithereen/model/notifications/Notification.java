package smithereen.model.notifications;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Objects;

import smithereen.storage.DatabaseUtils;

public final class Notification implements NotificationWrapper{
	public int id;

	@NotNull
	public Type type;

	@Nullable
	public ObjectType objectType;

	public long objectID;

	@Nullable
	public ObjectType relatedObjectType;

	public long relatedObjectID;
	public int actorID;

	@NotNull
	public Instant time;

	private Notification(@NotNull Type type, @NotNull Instant time){
		this.type=type;
		this.time=time;
	}

	public static Notification fromResultSet(ResultSet res) throws SQLException{
		Notification n=new Notification(
				Type.values()[res.getInt("type")],
				Objects.requireNonNull(DatabaseUtils.getInstant(res, "time"))
		);
		n.id=res.getInt("id");
		n.objectID=res.getLong("object_id");
		if(!res.wasNull())
			n.objectType=ObjectType.values()[res.getInt("object_type")];
		n.relatedObjectID=res.getLong("related_object_id");
		if(!res.wasNull())
			n.relatedObjectType=ObjectType.values()[res.getInt("related_object_type")];
		n.actorID=res.getInt("actor_id");
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

	@Override
	@NotNull
	public Notification getLatestNotification(){
		return this;
	}

	public enum Type{
		/**
		 * %username% replied to you
		 * object is the comment
		 * relatedObject is the top-level post or comment parent
		 */
		REPLY,
		/**
		 * %username% liked your %object%
		 * object is what was liked
		 * relatedObject is the top-level post or comment parent, if comment was liked
		 */
		LIKE,
		/**
		 * %username% mentioned you in their %object%
		 * object is post/comment mentioning the user
		 * relatedObject is the top-level post or comment parent, if mention is in a comment
		 */
		MENTION,
		/**
		 * %username% reposted (Mastodon style) your post
		 * object is wall post/comment that was reposted
		 * relatedObject is the top-level post if object is a comment
		 */
		RETOOT,
		/**
		 * %username% posted on your wall
		 * object is the wall post
		 */
		POST_OWN_WALL,
		/**
		 * %username% signed up by your invitation
		 */
		INVITE_SIGNUP,
		/**
		 * %username% followed you
		 */
		FOLLOW,
		/**
		 * %username% accepted your friend request
		 */
		FRIEND_REQ_ACCEPT,
		/**
		 * %username% reposted (quoted) your %object%
		 * object is wall post/comment that was reposted
		 * relatedObject is the top-level post if object is a comment
		 */
		REPOST;

		public boolean canBeGrouped(){
			return this==LIKE || this==RETOOT || this==INVITE_SIGNUP || this==FOLLOW || this==FRIEND_REQ_ACCEPT || this==REPOST;
		}
	}

	public enum ObjectType{
		POST,
		PHOTO,
		COMMENT,
		BOARD_TOPIC
	}
}
