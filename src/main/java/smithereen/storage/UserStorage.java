package smithereen.storage;

import org.jetbrains.annotations.NotNull;
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
import smithereen.Utils;
import smithereen.activitypub.ContextCollector;
import smithereen.data.Account;
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
	private static LruCache<Integer, Account> accountCache=new LruCache<>(500);

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

	public static ForeignUser getByOutbox(URI outbox) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT * FROM `users` WHERE `ap_outbox`=?");
		stmt.setString(1, outbox.toString());
		try(ResultSet res=stmt.executeQuery()){
			if(res.first())
				return ForeignUser.fromResultSet(res);
		}
		return null;
	}

	public static FriendshipStatus getFriendshipStatus(int selfUserID, int targetUserID) throws SQLException{
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT `follower_id`,`followee_id`,`mutual`,`accepted` FROM `followings` WHERE (`follower_id`=? AND `followee_id`=?) OR (`follower_id`=? AND `followee_id`=?) LIMIT 1");
		stmt.setInt(1, selfUserID);
		stmt.setInt(2, targetUserID);
		stmt.setInt(3, targetUserID);
		stmt.setInt(4, selfUserID);
		FriendshipStatus status;
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				boolean mutual=res.getBoolean(3);
				boolean accepted=res.getBoolean(4);
				if(mutual)
					return FriendshipStatus.FRIENDS;
				int follower=res.getInt(1);
				int followee=res.getInt(2);
				if(follower==selfUserID && followee==targetUserID)
					status=accepted ? FriendshipStatus.FOLLOWING : FriendshipStatus.FOLLOW_REQUESTED;
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

	public static void putFriendRequest(int selfUserID, int targetUserID, String message, boolean followAccepted) throws SQLException{
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
					stmt=conn.prepareStatement("INSERT INTO `followings` (`follower_id`, `followee_id`, `accepted`) VALUES (?, ?, ?)");
					stmt.setInt(1, selfUserID);
					stmt.setInt(2, targetUserID);
					stmt.setBoolean(3, followAccepted);
					stmt.execute();
				}
			}
			synchronized(NotificationsStorage.class){
				UserNotifications res=NotificationsStorage.getNotificationsFromCache(targetUserID);
				if(res!=null)
					res.incNewFriendRequestCount(1);
			}
			conn.createStatement().execute("COMMIT");
		}catch(SQLException x){
			conn.createStatement().execute("ROLLBACK");
			throw new SQLException(x);
		}
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

	public static List<User> getNonMutualFollowers(int userID, boolean followers, boolean accepted) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		String fld1=followers ? "follower_id" : "followee_id";
		String fld2=followers ? "followee_id" : "follower_id";
		PreparedStatement stmt=conn.prepareStatement("SELECT `users`.* FROM `followings` INNER JOIN `users` ON `users`.`id`=`followings`.`"+fld1+"` WHERE `"+fld2+"`=? AND `mutual`=0 AND `accepted`=?");
		stmt.setInt(1, userID);
		stmt.setBoolean(2, accepted);
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

	public static void acceptFriendRequest(int userID, int targetUserID, boolean followAccepted) throws SQLException{
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
			stmt=conn.prepareStatement("INSERT INTO `followings` (`follower_id`, `followee_id`, `mutual`, `accepted`) VALUES(?, ?, 1, ?)");
			stmt.setInt(1, userID);
			stmt.setInt(2, targetUserID);
			stmt.setBoolean(3, followAccepted);
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
			synchronized(NotificationsStorage.class){
				UserNotifications n=NotificationsStorage.getNotificationsFromCache(userID);
				if(n!=null)
					n.incNewFriendRequestCount(-1);
			}
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
		int rows=stmt.executeUpdate();
		synchronized(NotificationsStorage.class){
			UserNotifications n=NotificationsStorage.getNotificationsFromCache(userID);
			if(n!=null)
				n.incNewFriendRequestCount(-rows);
		}
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

	public static void followUser(int userID, int targetUserID, boolean accepted) throws SQLException{
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

			stmt=conn.prepareStatement("INSERT INTO `followings` (`follower_id`,`followee_id`,`mutual`,`accepted`) VALUES (?,?,?,?)");
			stmt.setInt(1, userID);
			stmt.setInt(2, targetUserID);
			stmt.setBoolean(3, mutual);
			stmt.setBoolean(4, accepted);
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

	public static void changeBasicInfo(int userID, String firstName, String lastName, String middleName, String maidenName, User.Gender gender, java.sql.Date bdate) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("UPDATE `users` SET `fname`=?, `lname`=?, `gender`=?, `bdate`=?, middle_name=?, maiden_name=? WHERE `id`=?");
		stmt.setString(1, firstName);
		stmt.setString(2, lastName);
		stmt.setInt(3, gender.ordinal());
		stmt.setDate(4, bdate);
		stmt.setString(5, middleName);
		stmt.setString(6, maidenName);
		stmt.setInt(7, userID);
		stmt.execute();
		synchronized(UserStorage.class){
			User user=cache.get(userID);
			if(user!=null){
				cache.remove(userID);
				cacheByActivityPubID.remove(user.activityPubID);
				cacheByUsername.remove(user.getFullUsername());
			}
		}
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
		synchronized(UserStorage.class){
			User user=cache.get(userID);
			if(user!=null){
				cache.remove(userID);
				cacheByActivityPubID.remove(user.activityPubID);
				cacheByUsername.remove(user.getFullUsername());
			}
		}
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
					"`about`=?,`gender`=?,`avatar`=?,`profile_fields`=?,`flags`=?,middle_name=?,maiden_name=?,`last_updated`=CURRENT_TIMESTAMP() WHERE `id`=?");
			stmt.setInt(21, existingUserID);
		}else{
			stmt=conn.prepareStatement("INSERT INTO `users` (`fname`,`lname`,`bdate`,`username`,`domain`,`public_key`,`ap_url`,`ap_inbox`,`ap_outbox`,`ap_shared_inbox`,`ap_id`,`ap_followers`,`ap_following`,`about`,`gender`,`avatar`,`profile_fields`,`flags`,middle_name,maiden_name`last_updated`)" +
					" VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP())", PreparedStatement.RETURN_GENERATED_KEYS);
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
		stmt.setString(17, user.serializeProfileFields());
		stmt.setLong(18, user.flags);
		stmt.setString(19, user.middleName);
		stmt.setString(20, user.maidenName);

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
			String[] components=apID.getPath().substring(1).split("/");
			if(components.length<2)
				return null;
			if(!"users".equals(components[0]))
				return null;
			return getById(Utils.parseIntOrDefault(components[1], 0));
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
		PreparedStatement stmt=conn.prepareStatement("SELECT DISTINCT IFNULL(`ap_shared_inbox`, `ap_inbox`) FROM `users` WHERE `id` IN (SELECT `follower_id` FROM `followings` WHERE `followee_id`=?)");
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
			stmt=conn.prepareStatement("SELECT `ap_id`,`username` FROM `followings` INNER JOIN `users` ON `users`.`id`=`"+fld1+"` WHERE `"+fld2+"`=? AND `accepted`=1 LIMIT ? OFFSET ?");
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

	public static void setFollowAccepted(int followerID, int followeeID, boolean accepted) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("UPDATE `followings` SET `accepted`=? WHERE `follower_id`=? AND `followee_id`=?");
		stmt.setBoolean(1, accepted);
		stmt.setInt(2, followerID);
		stmt.setInt(3, followeeID);
		stmt.execute();
	}

	public static List<Account> getAllAccounts(int offset, int count) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT a1.*, a2.user_id AS inviter_user_id FROM accounts AS a1 LEFT JOIN accounts AS a2 ON a1.invited_by=a2.id LIMIT ?,?");
		stmt.setInt(1, offset);
		stmt.setInt(2, count);
		ArrayList<Account> accounts=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					Account acc=Account.fromResultSet(res);
					int inviterID=res.getInt("inviter_user_id");
					if(inviterID!=0){
						acc.invitedBy=getById(inviterID);
					}
					accounts.add(acc);
				}while(res.next());
			}
		}
		return accounts;
	}

	public static synchronized Account getAccount(int id) throws SQLException{
		Account acc=accountCache.get(id);
		if(acc!=null)
			return acc;
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT a1.*, a2.user_id AS inviter_user_id FROM accounts AS a1 LEFT JOIN accounts AS a2 ON a1.invited_by=a2.id WHERE a1.id=?");
		stmt.setInt(1, id);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				acc=Account.fromResultSet(res);
				int inviterID=res.getInt("inviter_user_id");
				if(inviterID!=0){
					acc.invitedBy=getById(inviterID);
				}
				accountCache.put(acc.id, acc);
				return acc;
			}
		}
		return null;
	}

	public static void setAccountAccessLevel(int id, Account.AccessLevel level) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("UPDATE accounts SET access_level=? WHERE id=?");
		stmt.setInt(1, level.ordinal());
		stmt.setInt(2, id);
		stmt.execute();
		synchronized(UserStorage.class){
			accountCache.remove(id);
		}
	}
}
