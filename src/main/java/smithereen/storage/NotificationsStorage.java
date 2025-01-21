package smithereen.storage;

import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import smithereen.LruCache;
import smithereen.model.PaginatedList;
import smithereen.model.UserNotifications;
import smithereen.model.notifications.Notification;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;

public class NotificationsStorage{
	private static final LruCache<Integer, UserNotifications> userNotificationsCache=new LruCache<>(500);

	public static void putNotification(int owner, Notification.Type type, Notification.ObjectType objectType, long objectID, Notification.ObjectType relatedObjectType, long relatedObjectID, int actorID) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("notifications")
				.value("owner_id", owner)
				.value("type", type)
				.value("object_type", objectType)
				.value("object_id", objectType==null ? null : objectID)
				.value("related_object_type", relatedObjectType)
				.value("related_object_id", relatedObjectType==null ? null : relatedObjectID)
				.value("actor_id", actorID)
				.executeNoResult();

		UserNotifications un=getNotificationsFromCache(owner);
		if(un!=null)
			un.incNewNotificationsCount(1);
	}

	public static PaginatedList<Notification> getNotifications(int owner, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("notifications")
					.count()
					.where("owner_id=?", owner)
					.executeAndGetInt();
			List<Notification> notifications=new SQLQueryBuilder(conn)
					.selectFrom("notifications")
					.allColumns()
					.where("owner_id=?", owner)
					.orderBy("`time` DESC")
					.limit(count, offset)
					.executeAsStream(Notification::fromResultSet)
					.toList();
			return new PaginatedList<>(notifications, total, offset, count);
		}
	}

	public static void deleteNotificationsForObject(@NotNull Notification.ObjectType type, long objID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			conn.createStatement().execute("LOCK TABLES `notifications` WRITE");
			PreparedStatement stmt=conn.prepareStatement("SELECT DISTINCT `owner_id` FROM `notifications` WHERE (`object_type`=? AND `object_id`=?) OR (`related_object_type`=? AND `related_object_id`=?)");
			stmt.setInt(1, type.ordinal());
			stmt.setInt(3, type.ordinal());
			stmt.setLong(2, objID);
			stmt.setLong(4, objID);
			try(ResultSet res=stmt.executeQuery()){
				while(res.next()){
					userNotificationsCache.remove(res.getInt(1));
				}
			}
			stmt=conn.prepareStatement("DELETE FROM `notifications` WHERE (`object_type`=? AND `object_id`=?) OR (`related_object_type`=? AND `related_object_id`=?)");
			stmt.setInt(1, type.ordinal());
			stmt.setInt(3, type.ordinal());
			stmt.setLong(2, objID);
			stmt.setLong(4, objID);
			stmt.execute();

			conn.createStatement().execute("UNLOCK TABLES");
		}
	}

	public static void deleteNotification(@NotNull Notification.ObjectType objType, long objID, @NotNull Notification.Type type, int actorID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=conn.prepareStatement("SELECT `owner_id` FROM `notifications` WHERE `object_type`=? AND `object_id`=? AND `type`=? AND `actor_id`=?");
			stmt.setInt(1, objType.ordinal());
			stmt.setLong(2, objID);
			stmt.setInt(3, type.ordinal());
			stmt.setInt(4, actorID);
			try(ResultSet res=stmt.executeQuery()){
				if(!res.next())
					return;
				userNotificationsCache.remove(res.getInt(1));
			}
			stmt=conn.prepareStatement("DELETE FROM `notifications` WHERE `object_type`=? AND `object_id`=? AND `type`=? AND `actor_id`=?");
			stmt.setInt(1, objType.ordinal());
			stmt.setLong(2, objID);
			stmt.setInt(3, type.ordinal());
			stmt.setInt(4, actorID);
			stmt.execute();
		}
	}

	public static UserNotifications getNotificationsForUser(int userID, int lastSeenID) throws SQLException{
		UserNotifications res=userNotificationsCache.get(userID);
		if(res!=null)
			return res;
		res=new UserNotifications();
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=conn.prepareStatement("SELECT COUNT(*) FROM `friend_requests` WHERE `to_user_id`=?");
			stmt.setInt(1, userID);
			try(ResultSet r=stmt.executeQuery()){
				r.next();
				res.incNewFriendRequestCount(r.getInt(1));
			}
			stmt=conn.prepareStatement("SELECT COUNT(*) FROM `notifications` WHERE `owner_id`=? AND `id`>?");
			stmt.setInt(1, userID);
			stmt.setInt(2, lastSeenID);
			try(ResultSet r=stmt.executeQuery()){
				r.next();
				res.incNewNotificationsCount(r.getInt(1));
			}
			stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT COUNT(*), is_event FROM group_invites WHERE invitee_id=? GROUP BY is_event", userID);
			try(ResultSet r=stmt.executeQuery()){
				while(r.next()){
					if(r.getBoolean(2)) // event
						res.incNewEventInvitationsCount(r.getInt(1));
					else
						res.incNewGroupInvitationsCount(r.getInt(1));
				}
			}
			res.incUnreadMailCount(MailStorage.getUnreadMessagesCount(userID));
			res.incNewPhotoTagCount(new SQLQueryBuilder(conn)
					.selectFrom("photo_tags")
					.count()
					.where("user_id=? AND approved=0", userID)
					.executeAndGetInt());
			userNotificationsCache.put(userID, res);
			return res;
		}
	}

	public static UserNotifications getNotificationsFromCache(int userID){
		return userNotificationsCache.get(userID);
	}
}
