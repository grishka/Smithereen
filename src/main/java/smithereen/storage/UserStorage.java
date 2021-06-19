package smithereen.storage;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.ContextCollector;
import smithereen.data.Account;
import smithereen.data.ForeignUser;
import smithereen.data.FriendRequest;
import smithereen.data.FriendshipStatus;
import smithereen.data.Invitation;
import smithereen.data.UriBuilder;
import smithereen.data.User;
import smithereen.data.UserNotifications;
import spark.utils.StringUtils;

public class UserStorage{
	private static LruCache<Integer, User> cache=new LruCache<>(500);
	private static LruCache<String, User> cacheByUsername=new LruCache<>(500);
	private static LruCache<URI, ForeignUser> cacheByActivityPubID=new LruCache<>(500);
	private static LruCache<Integer, Account> accountCache=new LruCache<>(500);

	private static Comparator<User> idComparator=Comparator.comparingInt(u->u.id);

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

	public static List<User> getById(List<Integer> ids) throws SQLException{
		return getById(ids, true);
	}

	public static List<User> getById(List<Integer> ids, boolean sorted) throws SQLException{
		if(ids.isEmpty())
			return Collections.emptyList();
		if(ids.size()==1)
			return Collections.singletonList(getById(ids.get(0)));
		List<User> result=new ArrayList<>(ids.size());
		synchronized(UserStorage.class){
			Iterator<Integer> itr=ids.iterator();
			while(itr.hasNext()){
				Integer id=itr.next();
				User user=cache.get(id);
				if(user!=null){
					itr.remove();
					result.add(user);
				}
			}
		}
		if(ids.isEmpty()){
			if(sorted)
				result.sort(idComparator);
			return result;
		}
		Connection conn=DatabaseConnectionManager.getConnection();
		try(ResultSet res=conn.createStatement().executeQuery("SELECT * FROM users WHERE id IN ("+ids.stream().map(Object::toString).collect(Collectors.joining(","))+")")){
			res.beforeFirst();
			int resultSizeBefore=result.size();
			while(res.next()){
				String domain=res.getString("domain");
				User user;
				if(StringUtils.isNotEmpty(domain))
					user=ForeignUser.fromResultSet(res);
				else
					user=User.fromResultSet(res);
				result.add(user);
			}
			synchronized(UserStorage.class){
				for(User user:result.subList(resultSizeBefore, result.size())){
					putIntoCache(user);
				}
			}
			if(sorted)
				result.sort(idComparator);
			return result;
		}
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
				putIntoCache(user);
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
		PreparedStatement stmt=new SQLQueryBuilder().selectFrom("followings").columns("followee_id").where("follower_id=? AND mutual=1", userID).createStatement();
		try(ResultSet res=stmt.executeQuery()){
			return getById(DatabaseUtils.intResultSetToList(res));
		}
	}

	public static List<URI> getActivityPubFriendList(int userID, int offset, int count) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT followee_id, ap_id FROM followings JOIN users ON followee_id=users.id WHERE follower_id=? AND mutual=1 ORDER BY followee_id ASC LIMIT ? OFFSET ?");
		stmt.setInt(1, userID);
		stmt.setInt(2, count);
		stmt.setInt(3, offset);
		try(ResultSet res=stmt.executeQuery()){
			res.beforeFirst();
			ArrayList<URI> ids=new ArrayList<>();
			while(res.next()){
				String apID=res.getString(2);
				ids.add(apID==null ? Config.localURI("/users/"+res.getInt(1)) : URI.create(apID));
			}
			return ids;
		}
	}

	public static int getUserFriendsCount(int userID) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("followings")
				.count()
				.where("follower_id=? AND mutual=1", userID)
				.createStatement();
		return DatabaseUtils.oneFieldToInt(stmt.executeQuery());
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
		PreparedStatement stmt=conn.prepareStatement("SELECT followee_id FROM `followings` WHERE `follower_id`=? AND `mutual`=1 ORDER BY RAND() LIMIT 6");
		stmt.setInt(1, userID);
		try(ResultSet res=stmt.executeQuery()){
			return getById(DatabaseUtils.intResultSetToList(res), false);
		}
	}

	public static int getMutualFriendsCount(int userID, int otherUserID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT COUNT(*) FROM followings AS friends1 INNER JOIN followings AS friends2 ON friends1.followee_id=friends2.followee_id WHERE friends1.follower_id=? AND friends2.follower_id=? AND friends1.mutual=1 AND friends2.mutual=1");
		stmt.setInt(1, userID);
		stmt.setInt(2, otherUserID);
		try(ResultSet res=stmt.executeQuery()){
			res.first();
			return res.getInt(1);
		}
	}

	public static List<User> getRandomMutualFriendsForProfile(int userID, int otherUserID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT friends1.followee_id FROM followings AS friends1 INNER JOIN followings AS friends2 ON friends1.followee_id=friends2.followee_id WHERE friends1.follower_id=? AND friends2.follower_id=? AND friends1.mutual=1 AND friends2.mutual=1 ORDER BY RAND() LIMIT 3");
		stmt.setInt(1, userID);
		stmt.setInt(2, otherUserID);
		try(ResultSet res=stmt.executeQuery()){
			return getById(DatabaseUtils.intResultSetToList(res), false);
		}
	}

	public static List<User> getMutualFriendListForUser(int userID, int otherUserID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT friends1.followee_id FROM followings AS friends1 INNER JOIN followings AS friends2 ON friends1.followee_id=friends2.followee_id WHERE friends1.follower_id=? AND friends2.follower_id=? AND friends1.mutual=1 AND friends2.mutual=1");
		stmt.setInt(1, userID);
		stmt.setInt(2, otherUserID);
		try(ResultSet res=stmt.executeQuery()){
			return getById(DatabaseUtils.intResultSetToList(res));
		}
	}

	public static List<User> getNonMutualFollowers(int userID, boolean followers, boolean accepted) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		String fld1=followers ? "follower_id" : "followee_id";
		String fld2=followers ? "followee_id" : "follower_id";
		PreparedStatement stmt=conn.prepareStatement("SELECT `"+fld1+"` FROM followings WHERE `"+fld2+"`=? AND `mutual`=0 AND `accepted`=?");
		stmt.setInt(1, userID);
		stmt.setBoolean(2, accepted);
		try(ResultSet res=stmt.executeQuery()){
			return getById(DatabaseUtils.intResultSetToList(res));
		}
	}

	public static List<FriendRequest> getIncomingFriendRequestsForUser(int userID, int offset, int count) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT message, from_user_id FROM `friend_requests` WHERE `to_user_id`=? LIMIT ?,?");
		stmt.setInt(1, userID);
		stmt.setInt(2, offset);
		stmt.setInt(3, count);
		ArrayList<FriendRequest> reqs=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					FriendRequest req=FriendRequest.fromResultSet(res);
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

	public static void changeBasicInfo(User user, String firstName, String lastName, String middleName, String maidenName, User.Gender gender, java.sql.Date bdate, String about) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		new SQLQueryBuilder()
				.update("users")
				.where("id=?", user.id)
				.value("fname", firstName)
				.value("lname", lastName)
				.value("gender", gender)
				.value("bdate", bdate)
				.value("middle_name", middleName)
				.value("maiden_name", maidenName)
				.value("about", about)
				.createStatement()
				.execute();
		synchronized(UserStorage.class){
			removeFromCache(user);
		}
	}

	public static int getLocalUserCount() throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		return DatabaseUtils.oneFieldToInt(conn.createStatement().executeQuery("SELECT count(*) FROM `accounts`"));
	}

	public static int getActiveLocalUserCount(long time) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("accounts")
				.count()
				.where("last_active>?", new Timestamp(System.currentTimeMillis()-time))
				.createStatement();
		return DatabaseUtils.oneFieldToInt(stmt.executeQuery());
	}

	public static void updateProfilePicture(User user, String serializedPic) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("UPDATE `users` SET `avatar`=? WHERE `id`=?");
		stmt.setString(1, serializedPic);
		stmt.setInt(2, user.id);
		stmt.execute();
		synchronized(UserStorage.class){
			removeFromCache(user);
		}
	}

	public static synchronized int putOrUpdateForeignUser(ForeignUser user) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.selectFrom("users")
				.columns("id")
				.where("ap_id=?", Objects.toString(user.activityPubID))
				.createStatement();
		int existingUserID=0;
		try(ResultSet res=stmt.executeQuery()){
			if(res.first())
				existingUserID=res.getInt(1);
		}
		SQLQueryBuilder bldr=new SQLQueryBuilder(conn);
		if(existingUserID!=0){
			bldr.update("users").where("id=?", existingUserID);
		}else{
			bldr.insertInto("users");
		}

		bldr.valueExpr("last_updated", "CURRENT_TIMESTAMP()")
				.value("fname", user.firstName)
				.value("lname", user.lastName)
				.value("bdate", user.birthDate)
				.value("username", user.username)
				.value("domain", user.domain)
				.value("public_key", user.publicKey.getEncoded())
				.value("ap_url", Objects.toString(user.url, null))
				.value("ap_inbox", Objects.toString(user.inbox, null))
				.value("ap_outbox", Objects.toString(user.outbox, null))
				.value("ap_shared_inbox", Objects.toString(user.sharedInbox, null))
				.value("ap_id", user.activityPubID.toString())
				.value("ap_followers", Objects.toString(user.followers, null))
				.value("ap_following", Objects.toString(user.following, null))
				.value("about", user.summary)
				.value("gender", user.gender)
				.value("avatar", user.icon!=null ? user.icon.get(0).asActivityPubObject(new JsonObject(), new ContextCollector()).toString() : null)
				.value("profile_fields", user.serializeProfileFields())
				.value("flags", user.flags)
				.value("middle_name", user.middleName)
				.value("maiden_name", user.maidenName)
				.value("ap_wall", Objects.toString(user.getWallURL(), null))
				.value("ap_friends", Objects.toString(user.getFriendsURL(), null))
				.value("ap_groups", Objects.toString(user.getGroupsURL(), null));
		stmt=existingUserID!=0 ? bldr.createStatement() : bldr.createStatement(PreparedStatement.RETURN_GENERATED_KEYS);

		stmt.executeUpdate();
		if(existingUserID==0){
			try(ResultSet res=stmt.getGeneratedKeys()){
				res.first();
				existingUserID=res.getInt(1);
			}
		}
		user.id=existingUserID;
		putIntoCache(user);

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
				cacheByUsername.put(user.getFullUsername().toLowerCase(), user);
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
			stmt=conn.prepareStatement("SELECT `ap_id`,`id` FROM `followings` INNER JOIN `users` ON `users`.`id`=`"+fld1+"` WHERE `"+fld2+"`=? AND `accepted`=1 LIMIT ? OFFSET ?");
			stmt.setInt(1, userID);
			stmt.setInt(2, count);
			stmt.setInt(3, offset);
			ArrayList<URI> list=new ArrayList<>();
			try(ResultSet res=stmt.executeQuery()){
				if(res.first()){
					do{
						String _u=res.getString(1);
						if(_u==null){
							list.add(Config.localURI("/users/"+res.getInt(2)));
						}else{
							list.add(URI.create(_u));
						}
					}while(res.next());
				}
			}
			return list;
		}
		return Collections.emptyList();
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

	public static List<User> getAdmins() throws SQLException{
		ResultSet res=new SQLQueryBuilder()
				.selectFrom("accounts")
				.columns("user_id")
				.where("access_level=?", Account.AccessLevel.ADMIN.ordinal())
				.createStatement()
				.executeQuery();
		return getById(DatabaseUtils.intResultSetToList(res), true);
	}

	public static int getPeerDomainCount() throws SQLException{
		ResultSet res=new SQLQueryBuilder()
				.selectFrom("users")
				.selectExpr("COUNT(DISTINCT domain)")
				.createStatement()
				.executeQuery();
		return DatabaseUtils.oneFieldToInt(res)-1; // -1 for local domain (empty string)
	}

	public static List<String> getPeerDomains() throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("users")
				.distinct()
				.columns("domain")
				.orderBy("domain asc")
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			ArrayList<String> domains=new ArrayList<>();
			res.beforeFirst();
			while(res.next()){
				String d=res.getString(1);
				if(d.length()>0)
					domains.add(d);
			}
			return domains;
		}
	}

	public static boolean isUserBlocked(int ownerID, int targetID) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("blocks_user_user")
				.count()
				.where("owner_id=? AND user_id=?", ownerID, targetID)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			res.first();
			return res.getInt(1)==1;
		}
	}

	public static void blockUser(int selfID, int targetID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		new SQLQueryBuilder(conn)
				.insertInto("blocks_user_user")
				.value("owner_id", selfID)
				.value("user_id", targetID)
				.createStatement()
				.execute();
		new SQLQueryBuilder(conn)
				.deleteFrom("followings")
				.where("(follower_id=? AND followee_id=?) OR (follower_id=? AND followee_id=?)", selfID, targetID, targetID, selfID)
				.createStatement()
				.execute();
		new SQLQueryBuilder(conn)
				.deleteFrom("friend_requests")
				.where("(from_user_id=? AND to_user_id=?) OR (from_user_id=? AND to_user_id=?)", selfID, targetID, targetID, selfID)
				.createStatement()
				.execute();
	}

	public static void unblockUser(int selfID, int targetID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("blocks_user_user")
				.where("owner_id=? AND user_id=?", selfID, targetID)
				.createStatement()
				.execute();
	}

	public static List<User> getBlockedUsers(int selfID) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("blocks_user_user")
				.columns("user_id")
				.where("owner_id=?", selfID)
				.createStatement();
		return getById(DatabaseUtils.intResultSetToList(stmt.executeQuery()), true);
	}

	public static boolean isDomainBlocked(int selfID, String domain) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("blocks_user_domain")
				.count()
				.where("owner_id=? AND domain=?", selfID, domain)
				.createStatement();
		return DatabaseUtils.oneFieldToInt(stmt.executeQuery())==1;
	}

	public static List<String> getBlockedDomains(int selfID) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("blocks_user_domain")
				.columns("domain")
				.where("owner_id=?", selfID)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			ArrayList<String> arr=new ArrayList<>();
			res.beforeFirst();
			while(res.next()){
				arr.add(res.getString(1));
			}
			return arr;
		}
	}

	public static void blockDomain(int selfID, String domain) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("blocks_user_domain")
				.value("owner_id", selfID)
				.value("domain", domain)
				.createStatement()
				.execute();
	}

	public static void unblockDomain(int selfID, String domain) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("blocks_user_domain")
				.where("owner_id=? AND domain=?", selfID, domain)
				.createStatement()
				.execute();
	}

	private static void putIntoCache(User user){
		cache.put(user.id, user);
		cacheByUsername.put(user.getFullUsername().toLowerCase(), user);
		if(user instanceof ForeignUser)
			cacheByActivityPubID.put(user.activityPubID, (ForeignUser) user);
	}

	private static void removeFromCache(User user){
		cache.remove(user.id);
		cacheByUsername.remove(user.getFullUsername().toLowerCase());
		if(user instanceof ForeignUser)
			cacheByActivityPubID.remove(user.activityPubID);
	}

	public static void putAccountBanInfo(int accountID, Account.BanInfo banInfo) throws SQLException{
		new SQLQueryBuilder()
				.update("accounts")
				.value("ban_info", banInfo!=null ? Utils.gson.toJson(banInfo) : null)
				.where("id=?", accountID)
				.createStatement()
				.execute();
		synchronized(UserStorage.class){
			accountCache.remove(accountID);
		}
	}
}
