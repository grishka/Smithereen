package smithereen.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import smithereen.LruCache;
import smithereen.data.UserNotifications;
import smithereen.data.notifications.Notification;

public class NotificationsStorage{
	private static LruCache<Integer, UserNotifications> userNotificationsCache=new LruCache<>(500);

	public static void putNotification(int owner, @NotNull Notification n) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("INSERT INTO `notifications` (`owner_id`, `type`, `object_id`, `object_type`, `related_object_id`, `related_object_type`, `actor_id`) VALUES (?, ?, ?, ?, ?, ?, ?)");
		stmt.setInt(1, owner);
		stmt.setInt(2, n.type.ordinal());
		if(n.objectID!=0){
			stmt.setInt(3, n.objectID);
			stmt.setInt(4, n.objectType.ordinal());
		}else{
			stmt.setNull(3, Types.INTEGER);
			stmt.setNull(4, Types.INTEGER);
		}
		if(n.relatedObjectID!=0){
			stmt.setInt(5, n.relatedObjectID);
			stmt.setInt(6, n.relatedObjectType.ordinal());
		}else{
			stmt.setNull(5, Types.INTEGER);
			stmt.setNull(6, Types.INTEGER);
		}
		stmt.setInt(7, n.actorID);
		stmt.execute();
		UserNotifications un=getNotificationsFromCache(owner);
		if(un!=null)
			un.incNewNotificationsCount(1);
	}

	public static List<Notification> getNotifications(int owner, int offset, @Nullable int[] total) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt;
		if(total!=null){
			stmt=conn.prepareStatement("SELECT COUNT(*) FROM `notifications` WHERE `owner_id`=?");
			stmt.setInt(1, owner);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				total[0]=res.getInt(1);
			}
		}
		stmt=conn.prepareStatement("SELECT * FROM `notifications` WHERE `owner_id`=? ORDER BY `time` DESC LIMIT ?,50");
		stmt.setInt(1, owner);
		stmt.setInt(2, offset);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				ArrayList<Notification> notifications=new ArrayList<>();
				do{
					notifications.add(Notification.fromResultSet(res));
				}while(res.next());
				return notifications;
			}
		}
		return Collections.EMPTY_LIST;
	}

	public static void deleteNotificationsForObject(@NotNull Notification.ObjectType type, int objID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		conn.createStatement().execute("LOCK TABLES `notifications` WRITE");
		PreparedStatement stmt=conn.prepareStatement("SELECT DISTINCT `owner_id` FROM `notifications` WHERE (`object_type`=? AND `object_id`=?) OR (`related_object_type`=? AND `related_object_id`=?)");
		stmt.setInt(1, type.ordinal());
		stmt.setInt(3, type.ordinal());
		stmt.setInt(2, objID);
		stmt.setInt(4, objID);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				synchronized(NotificationsStorage.class){
					do{
						userNotificationsCache.remove(res.getInt(1));
					}while(res.next());
				}
			}
		}
		stmt=conn.prepareStatement("DELETE FROM `notifications` WHERE (`object_type`=? AND `object_id`=?) OR (`related_object_type`=? AND `related_object_id`=?)");
		stmt.setInt(1, type.ordinal());
		stmt.setInt(3, type.ordinal());
		stmt.setInt(2, objID);
		stmt.setInt(4, objID);

		conn.createStatement().execute("UNLOCK TABLES");
	}

	public static synchronized UserNotifications getNotificationsForUser(int userID, int lastSeenID) throws SQLException{
		UserNotifications res=userNotificationsCache.get(userID);
		if(res!=null)
			return res;
		res=new UserNotifications();
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT COUNT(*) FROM `friend_requests` WHERE `to_user_id`=?");
		stmt.setInt(1, userID);
		try(ResultSet r=stmt.executeQuery()){
			r.first();
			res.incNewFriendRequestCount(r.getInt(1));
		}
		stmt=conn.prepareStatement("SELECT COUNT(*) FROM `notifications` WHERE `owner_id`=? AND `id`>?");
		stmt.setInt(1, userID);
		stmt.setInt(2, lastSeenID);
		try(ResultSet r=stmt.executeQuery()){
			r.first();
			res.incNewNotificationsCount(r.getInt(1));
		}
		userNotificationsCache.put(userID, res);
		return res;
	}

	public static synchronized UserNotifications getNotificationsFromCache(int userID){
		return userNotificationsCache.get(userID);
	}
}
