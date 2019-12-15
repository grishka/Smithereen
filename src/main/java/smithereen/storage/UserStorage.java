package smithereen.storage;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import smithereen.Config;
import smithereen.LruCache;
import smithereen.activitypub.ContextCollector;
import smithereen.data.ForeignUser;
import smithereen.data.FriendRequest;
import smithereen.data.FriendshipStatus;
import smithereen.data.Invitation;
import smithereen.data.User;
import smithereen.data.UserNotifications;

public class UserStorage{
	private static LruCache<Integer, User> cache=new LruCache<>(500);
	private static LruCache<String, User> cacheByUsername=new LruCache<>(500);
	private static LruCache<URI, ForeignUser> cacheByActivityPubID=new LruCache<>(500);
	private static LruCache<Integer, UserNotifications> userNotificationsCache=new LruCache<>(500);

	public static synchronized User getById(int id) throws SQLException{
		User user=cache.get(id);
		if(user!=null)
			return user;
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT * FROM `users` WHERE `id`=?");
		stmt.setInt(1, id);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				user=User.fromResultSet(res);
				cache.put(id, user);
				cacheByUsername.put(user.getFullUsername(), user);
				return user;
			}
		}
		return null;
	}

	public static synchronized User getByUsername(@NotNull String username) throws SQLException{
		username=username.toLowerCase();
		User user=cacheByUsername.get(username);
		if(user!=null)
			return user;
		String realUsername;
		String domain="";
		if(username.contains("@")){
			String[] parts=username.split("@");
			realUsername=parts[0];
			domain=parts[1];
		}else{
			realUsername=username;
		}
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT * FROM `users` WHERE `username`=? AND `domain`=?");
		stmt.setString(1, realUsername);
		stmt.setString(2, domain);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				if(domain.length()>0)
					user=ForeignUser.fromResultSet(res);
				else
					user=User.fromResultSet(res);
				cacheByUsername.put(username, user);
				cache.put(user.id, user);
				return user;
			}
		}
		return null;
	}

	public static FriendshipStatus getFriendshipStatus(int selfUserID, int targetUserID) throws SQLException{
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT `follower_id`,`followee_id`,`mutual` FROM `followings` WHERE (`follower_id`=? AND `followee_id`=?) OR (`follower_id`=? AND `followee_id`=?) LIMIT 1");
		stmt.setInt(1, selfUserID);
		stmt.setInt(2, targetUserID);
		stmt.setInt(3, targetUserID);
		stmt.setInt(4, selfUserID);
		FriendshipStatus status;
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				boolean mutual=res.getBoolean(3);
				if(mutual)
					return FriendshipStatus.FRIENDS;
				int follower=res.getInt(1);
				int followee=res.getInt(2);
				if(follower==selfUserID && followee==targetUserID)
					status=FriendshipStatus.FOLLOWING;
				else
					status=FriendshipStatus.FOLLOWED_BY;
			}else{
				return FriendshipStatus.NONE;
			}
		}

		stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT count(*) FROM `friend_requests` WHERE `from_user_id`=? AND `to_user_id`=?");
		if(status==FriendshipStatus.FOLLOWING){
			stmt.setInt(1, selfUserID);
			stmt.setInt(2, targetUserID);
		}else{
			stmt.setInt(2, selfUserID);
			stmt.setInt(1, targetUserID);
		}
		try(ResultSet res=stmt.executeQuery()){
			res.first();
			int count=res.getInt(1);
			if(count==1){
				if(status==FriendshipStatus.FOLLOWING)
					return FriendshipStatus.REQUEST_SENT;
				else
					return FriendshipStatus.REQUEST_RECVD;
			}
		}
		return status;
	}

	public static void putFriendRequest(int selfUserID, int targetUserID, String message) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		conn.createStatement().execute("START TRANSACTION");
		try{
			PreparedStatement stmt=conn.prepareStatement("INSERT INTO `friend_requests` (`from_user_id`, `to_user_id`, `message`) VALUES (?, ?, ?)");
			stmt.setInt(1, selfUserID);
			stmt.setInt(2, targetUserID);
			stmt.setString(3, message);
			stmt.execute();
			stmt=conn.prepareStatement("SELECT COUNT(*) FROM `followings` WHERE `follower_id`=? AND `followee_id`=?");
			stmt.setInt(1, selfUserID);
			stmt.setInt(2, targetUserID);
			try(ResultSet res=stmt.executeQuery()){
				if(!res.first() || res.getInt(1)==0){
					stmt=conn.prepareStatement("INSERT INTO `followings` (`follower_id`, `followee_id`) VALUES (?, ?)");
					stmt.setInt(1, selfUserID);
					stmt.setInt(2, targetUserID);
					stmt.execute();
				}
			}
			synchronized(UserStorage.class){
				UserNotifications res=userNotificationsCache.get(targetUserID);
				if(res!=null)
					res.incNewFriendRequestCount(1);
			}
			conn.createStatement().execute("COMMIT");
		}catch(SQLException x){
			conn.createStatement().execute("ROLLBACK");
			throw new SQLException(x);
		}
	}

	public static synchronized UserNotifications getNotificationsForUser(int userID) throws SQLException{
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
		userNotificationsCache.put(userID, res);
		return res;
	}

	public static List<User> getFriendListForUser(int userID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT `users`.* FROM `followings` INNER JOIN `users` ON `users`.`id`=`followings`.`followee_id` WHERE `follower_id`=? AND `mutual`=1");
		stmt.setInt(1, userID);
		ArrayList<User> friends=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					User user;
					if(res.getString("domain").length()>0)
						user=ForeignUser.fromResultSet(res);
					else
						user=User.fromResultSet(res);
					cache.put(user.id, user);
					cacheByUsername.put(user.getFullUsername(), user);
					friends.add(user);
				}while(res.next());
			}
		}
		return friends;
	}

	public static List<User> getRandomFriendsForProfile(int userID, int[] outTotal) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		if(outTotal!=null && outTotal.length>=1){
			PreparedStatement stmt=conn.prepareStatement("SELECT COUNT(*) FROM `followings` WHERE `follower_id`=? AND `mutual`=1");
			stmt.setInt(1, userID);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				outTotal[0]=res.getInt(1);
			}
		}
		PreparedStatement stmt=conn.prepareStatement("SELECT `users`.* FROM `followings` INNER JOIN `users` ON `users`.`id`=`followings`.`followee_id` WHERE `follower_id`=? AND `mutual`=1 ORDER BY RAND() LIMIT 6");
		stmt.setInt(1, userID);
		ArrayList<User> friends=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					User user;
					if(res.getString("domain").length()>0)
						user=ForeignUser.fromResultSet(res);
					else
						user=User.fromResultSet(res);
					cache.put(user.id, user);
					cacheByUsername.put(user.getFullUsername(), user);
					friends.add(user);
				}while(res.next());
			}
		}
		return friends;
	}

	public static List<User> getNonMutualFollowers(int userID, boolean followers) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		String fld1=followers ? "follower_id" : "followee_id";
		String fld2=followers ? "followee_id" : "follower_id";
		PreparedStatement stmt=conn.prepareStatement("SELECT `users`.* FROM `followings` INNER JOIN `users` ON `users`.`id`=`followings`.`"+fld1+"` WHERE `"+fld2+"`=? AND `mutual`=0");
		stmt.setInt(1, userID);
		ArrayList<User> friends=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					User user;
					user=User.fromResultSet(res);
					cache.put(user.id, user);
					cacheByUsername.put(user.getFullUsername(), user);
					friends.add(user);
				}while(res.next());
			}
		}
		return friends;
	}

	public static List<FriendRequest> getIncomingFriendRequestsForUser(int userID, int offset, int count) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT `friend_requests`.`message`, `users`.* FROM `friend_requests` INNER JOIN `users` ON `friend_requests`.`from_user_id`=`users`.`id` WHERE `to_user_id`=? LIMIT ?,?");
		stmt.setInt(1, userID);
		stmt.setInt(2, offset);
		stmt.setInt(3, count);
		ArrayList<FriendRequest> reqs=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					FriendRequest req=FriendRequest.fromResultSet(res);
					cache.put(req.from.id, req.from);
					cacheByUsername.put(req.from.getFullUsername(), req.from);
					reqs.add(req);
				}while(res.next());
			}
		}
		return reqs;
	}

	public static void acceptFriendRequest(int userID, int targetUserID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		conn.createStatement().execute("START TRANSACTION");
		try{
			PreparedStatement stmt=conn.prepareStatement("DELETE FROM `friend_requests` WHERE `from_user_id`=? AND `to_user_id`=?");
			stmt.setInt(1, targetUserID);
			stmt.setInt(2, userID);
			if(stmt.executeUpdate()!=1){
				System.out.println("fail 1");
				conn.createStatement().execute("ROLLBACK");
				return;
			}
			stmt=conn.prepareStatement("INSERT INTO `followings` (`follower_id`, `followee_id`, `mutual`) VALUES(?, ?, 1)");
			stmt.setInt(1, userID);
			stmt.setInt(2, targetUserID);
			stmt.execute();
			stmt=conn.prepareStatement("UPDATE `followings` SET `mutual`=1 WHERE `follower_id`=? AND `followee_id`=?");
			stmt.setInt(1, targetUserID);
			stmt.setInt(2, userID);
			if(stmt.executeUpdate()!=1){
				System.out.println("fail 2");
				conn.createStatement().execute("ROLLBACK");
				return;
			}
			conn.createStatement().execute("COMMIT");
			UserNotifications n=userNotificationsCache.get(userID);
			if(n!=null)
				n.incNewFriendRequestCount(-1);
		}catch(SQLException x){
			conn.createStatement().execute("ROLLBACK");
			throw new SQLException(x);
		}
	}

	public static void deleteFriendRequest(int userID, int targetUserID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("DELETE FROM `friend_requests` WHERE `from_user_id`=? AND `to_user_id`=?");
		stmt.setInt(1, targetUserID);
		stmt.setInt(2, userID);
		stmt.execute();
		UserNotifications n=userNotificationsCache.get(userID);
		if(n!=null)
			n.incNewFriendRequestCount(-1);
	}

	public static void unfriendUser(int userID, int targetUserID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		conn.createStatement().execute("START TRANSACTION");
		try{
			PreparedStatement stmt=conn.prepareStatement("DELETE FROM `followings` WHERE `follower_id`=? AND `followee_id`=?");
			stmt.setInt(1, userID);
			stmt.setInt(2, targetUserID);
			stmt.execute();
			stmt=conn.prepareStatement("UPDATE `followings` SET `mutual`=0 WHERE `follower_id`=? AND `followee_id`=?");
			stmt.setInt(1, targetUserID);
			stmt.setInt(2, userID);
			stmt.execute();
			conn.createStatement().execute("COMMIT");
		}catch(SQLException x){
			conn.createStatement().execute("ROLLBACK");
			throw new SQLException(x);
		}
	}

	public static void followUser(int userID, int targetUserID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		conn.createStatement().execute("START TRANSACTION");
		try{
			boolean mutual=false;
			PreparedStatement stmt=conn.prepareStatement("SELECT count(*) FROM `followings` WHERE `follower_id`=? AND `followee_id`=?");
			stmt.setInt(1, targetUserID);
			stmt.setInt(2, userID);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				mutual=res.getInt(1)==1;
			}
			stmt.setInt(1, userID);
			stmt.setInt(2, targetUserID);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				if(res.getInt(1)==1)
					throw new SQLException("Already following");
			}

			stmt=conn.prepareStatement("INSERT INTO `followings` (`follower_id`,`followee_id`,`mutual`) VALUES (?,?,?)");
			stmt.setInt(1, userID);
			stmt.setInt(2, targetUserID);
			stmt.setBoolean(3, mutual);
			stmt.execute();

			if(mutual){
				stmt=conn.prepareStatement("UPDATE `followings` SET `mutual`=1 WHERE `follower_id`=? AND `followee_id`=?");
				stmt.setInt(1, targetUserID);
				stmt.setInt(2, userID);
				stmt.execute();
			}

			conn.createStatement().execute("COMMIT");
		}catch(SQLException x){
			conn.createStatement().execute("ROLLBACK");
			throw new SQLException(x);
		}
	}

	public static List<Invitation> getInvites(int userID, boolean onlyValid) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT * FROM `signup_invitations` WHERE `owner_id`=?"+(onlyValid ? " AND `signups_remaining`>0" : "")+" ORDER BY `created` DESC");
		stmt.setInt(1, userID);
		ArrayList<Invitation> invitations=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					invitations.add(Invitation.fromResultSet(res));
				}while(res.next());
			}
		}
		return invitations;
	}

	public static void putInvite(int userID, byte[] code, int signups) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("INSERT INTO `signup_invitations` (`owner_id`, `code`, `signups_remaining`) VALUES (?, ?, ?)");
		stmt.setInt(1, userID);
		stmt.setBytes(2, code);
		stmt.setInt(3, signups);
		stmt.execute();
	}

	public static void changeName(int userID, String first, String last) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("UPDATE `users` SET `fname`=?, `lname`=? WHERE `id`=?");
		stmt.setString(1, first);
		stmt.setString(2, last);
		stmt.setInt(3, userID);
		stmt.execute();
		User user=getById(userID);
		user.firstName=first;
		user.lastName=last;
	}

	public static int getLocalUserCount() throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		try(ResultSet res=conn.createStatement().executeQuery("SELECT count(*) FROM `users` WHERE `domain`=''")){
			res.first();
			return res.getInt(1);
		}
	}

	public static void updateProfilePicture(int userID, String serializedPic) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("UPDATE `users` SET `avatar`=? WHERE `id`=?");
		stmt.setString(1, serializedPic);
		stmt.setInt(2, userID);
		stmt.execute();
	}

	public static synchronized int putOrUpdateForeignUser(ForeignUser user) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT `id` FROM `users` WHERE `ap_id`=?");
		stmt.setString(1, Objects.toString(user.activityPubID, null));
		int existingUserID=0;
		try(ResultSet res=stmt.executeQuery()){
			if(res.first())
				existingUserID=res.getInt(1);
		}
		if(existingUserID!=0){
			stmt=conn.prepareStatement("UPDATE `users` SET `fname`=?,`lname`=?,`bdate`=?,`username`=?,`domain`=?,`public_key`=?,`ap_url`=?,`ap_inbox`=?,`ap_outbox`=?,`ap_shared_inbox`=?,`ap_id`=?,`ap_followers`=?,`ap_following`=?," +
					"`about`=?,`gender`=?,`avatar`=?,`last_updated`=CURRENT_TIMESTAMP() WHERE `id`=?");
			stmt.setInt(17, existingUserID);
		}else{
			stmt=conn.prepareStatement("INSERT INTO `users` (`fname`,`lname`,`bdate`,`username`,`domain`,`public_key`,`ap_url`,`ap_inbox`,`ap_outbox`,`ap_shared_inbox`,`ap_id`,`ap_followers`,`ap_following`,`about`,`gender`,`avatar`,`last_updated`)" +
					" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP())", PreparedStatement.RETURN_GENERATED_KEYS);
		}

		stmt.setString(1, user.firstName);
		stmt.setString(2, user.lastName);
		stmt.setDate(3, user.birthDate);
		stmt.setString(4, user.username);
		stmt.setString(5, user.domain);
		stmt.setBytes(6, user.publicKey.getEncoded());
		stmt.setString(7, Objects.toString(user.url, null));
		stmt.setString(8, Objects.toString(user.inbox, null));
		stmt.setString(9, Objects.toString(user.outbox, null));
		stmt.setString(10, Objects.toString(user.sharedInbox, null));
		stmt.setString(11, Objects.toString(user.activityPubID, null));
		stmt.setString(12, Objects.toString(user.followers, null));
		stmt.setString(13, Objects.toString(user.following, null));
		stmt.setString(14, user.summary);
		stmt.setInt(15, user.gender==null ? 0 : user.gender.ordinal());
		stmt.setString(16, user.icon!=null ? user.icon.get(0).asActivityPubObject(new JSONObject(), new ContextCollector()).toString() : null);

		stmt.executeUpdate();
		if(existingUserID==0){
			try(ResultSet res=stmt.getGeneratedKeys()){
				res.first();
				existingUserID=res.getInt(1);
			}
		}
		user.id=existingUserID;
		cache.put(user.id, user);
		cacheByUsername.put(user.getFullUsername(), user);
		cacheByActivityPubID.put(user.activityPubID, user);

		return existingUserID;
	}

	public static User getUserByActivityPubID(URI apID) throws SQLException{
		if(Config.isLocal(apID)){
			String username=apID.getPath().substring(1);
			return getByUsername(username);
		}
		return getForeignUserByActivityPubID(apID);
	}

	public static synchronized ForeignUser getForeignUserByActivityPubID(URI apID) throws SQLException{
		ForeignUser user=cacheByActivityPubID.get(apID);
		if(user!=null)
			return user;
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT * FROM `users` WHERE `ap_id`=?");
		stmt.setString(1, apID.toString());
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				user=ForeignUser.fromResultSet(res);
				cacheByActivityPubID.put(apID, user);
				cache.put(user.id, user);
				return user;
			}
		}
		return null;
	}

	public static List<URI> getFollowerInboxes(int userID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT DISTINCT `ap_shared_inbox` FROM `users` WHERE `id` IN (SELECT `follower_id` FROM `followings` WHERE `followee_id`=?)");
		stmt.setInt(1, userID);
		ArrayList<URI> list=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					String url=res.getString(1);
					if(url==null)
						continue;
					try{
						list.add(new URI(url));
					}catch(URISyntaxException ignore){}
				}while(res.next());
			}
		}
		return list;
	}

	public static List<URI> getUserFollowerURIs(int userID, boolean followers, int offset, int count, int[] total) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		String fld1=followers ? "follower_id" : "followee_id";
		String fld2=followers ? "followee_id" : "follower_id";
		PreparedStatement stmt;
		if(total!=null){
			stmt=conn.prepareStatement("SELECT count(*) FROM `followings` WHERE `"+fld2+"`=?");
			stmt.setInt(1, userID);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				total[0]=res.getInt(1);
			}
		}
		if(count>0){
			stmt=conn.prepareStatement("SELECT `ap_id`,`username` FROM `followings` INNER JOIN `users` ON `users`.`id`=`"+fld1+"` WHERE `"+fld2+"`=? LIMIT ? OFFSET ?");
			stmt.setInt(1, userID);
			stmt.setInt(2, count);
			stmt.setInt(3, offset);
			ArrayList<URI> list=new ArrayList<>();
			try(ResultSet res=stmt.executeQuery()){
				if(res.first()){
					do{
						String _u=res.getString(1);
						if(_u==null){
							list.add(Config.localURI(res.getString(2)));
						}else{
							list.add(URI.create(_u));
						}
					}while(res.next());
				}
			}
			return list;
		}
		return Collections.EMPTY_LIST;
	}
}
