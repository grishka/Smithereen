package smithereen.storage;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.model.Account;
import smithereen.model.BirthdayReminder;
import smithereen.model.EventReminder;
import smithereen.model.ForeignUser;
import smithereen.model.FriendRequest;
import smithereen.model.FriendshipStatus;
import smithereen.model.PrivacySetting;
import smithereen.model.SignupInvitation;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.UserBanStatus;
import smithereen.model.UserNotifications;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.UserRole;
import smithereen.model.media.MediaFileRecord;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import spark.utils.StringUtils;

public class UserStorage{
	private static final Logger LOG=LoggerFactory.getLogger(UserStorage.class);

	private static LruCache<Integer, User> cache=new LruCache<>(500);
	private static LruCache<String, User> cacheByUsername=new LruCache<>(500);
	private static LruCache<URI, ForeignUser> cacheByActivityPubID=new LruCache<>(500);
	private static LruCache<Integer, Account> accountCache=new LruCache<>(500);
	private static final LruCache<Integer, BirthdayReminder> birthdayReminderCache=new LruCache<>(500);

	public static synchronized User getById(int id) throws SQLException{
		User user=cache.get(id);
		if(user!=null)
			return user;
		user=new SQLQueryBuilder()
				.selectFrom("users")
				.where("id=?", id)
				.executeAndGetSingleObject(User::fromResultSet);
		if(user!=null){
			if(user.icon!=null && !user.icon.isEmpty() && user.icon.getFirst() instanceof LocalImage li){
				MediaFileRecord mfr=MediaStorage.getMediaFileRecord(li.fileID);
				if(mfr!=null)
					li.fillIn(mfr);
			}
			cache.put(id, user);
			cacheByUsername.put(user.getFullUsername(), user);
		}
		return user;
	}

	public static List<User> getByIdAsList(List<Integer> ids) throws SQLException{
		if(ids.isEmpty())
			return Collections.emptyList();
		if(ids.size()==1)
			return Collections.singletonList(getById(ids.get(0)));
		Map<Integer, User> users=getById(ids);
		return ids.stream().map(users::get).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public static Map<Integer, User> getById(Collection<Integer> _ids) throws SQLException{
		if(_ids.isEmpty())
			return Collections.emptyMap();
		if(_ids.size()==1){
			for(int id:_ids){
				return Collections.singletonMap(id, getById(id));
			}
		}
		Set<Integer> ids=new HashSet<>(_ids);
		Map<Integer, User> result=new HashMap<>(ids.size());
		synchronized(UserStorage.class){
			Iterator<Integer> itr=ids.iterator();
			while(itr.hasNext()){
				Integer id=itr.next();
				User user=cache.get(id);
				if(user!=null){
					itr.remove();
					result.put(id, user);
				}
			}
		}
		if(ids.isEmpty()){
			return result;
		}
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			new SQLQueryBuilder(conn)
					.selectFrom("users")
					.allColumns()
					.whereIn("id", ids)
					.executeAsStream(User::fromResultSet)
					.forEach(u->result.put(u.id, u));
			Set<Long> needAvatars=result.values().stream()
					.map(u->u.icon!=null && !u.icon.isEmpty() && u.icon.getFirst() instanceof LocalImage li ? li : null)
					.filter(li->li!=null && li.fileRecord==null)
					.map(li->li.fileID)
					.collect(Collectors.toSet());
			if(!needAvatars.isEmpty()){
				Map<Long, MediaFileRecord> records=MediaStorage.getMediaFileRecords(needAvatars);
				for(User user:result.values()){
					if(user.icon!=null && !user.icon.isEmpty() && user.icon.getFirst() instanceof LocalImage li && li.fileRecord==null){
						MediaFileRecord mfr=records.get(li.fileID);
						if(mfr!=null)
							li.fillIn(mfr);
					}
				}
			}
			synchronized(UserStorage.class){
				for(int id:ids){
					User u=result.get(id);
					if(u!=null)
						putIntoCache(u);
				}
			}
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
		user=new SQLQueryBuilder()
				.selectFrom("users")
				.allColumns()
				.where("username=? AND domain=?", realUsername, domain)
				.executeAndGetSingleObject(User::fromResultSet);
		if(user!=null){
			if(user.icon!=null && !user.icon.isEmpty() && user.icon.getFirst() instanceof LocalImage li){
				MediaFileRecord mfr=MediaStorage.getMediaFileRecord(li.fileID);
				if(mfr!=null)
					li.fillIn(mfr);
			}
			putIntoCache(user);
		}
		return user;
	}

	public static int getIdByUsername(@NotNull String username) throws SQLException{
		String realUsername;
		String domain="";
		if(username.contains("@")){
			String[] parts=username.split("@");
			realUsername=parts[0];
			domain=parts[1];
		}else{
			realUsername=username;
		}
		if(realUsername.length()>Actor.USERNAME_MAX_LENGTH)
			realUsername=realUsername.substring(0, Actor.USERNAME_MAX_LENGTH);
		return new SQLQueryBuilder()
				.selectFrom("users")
				.columns("id")
				.where("username=? AND domain=?", realUsername, domain)
				.executeAndGetInt();
	}

	public static FriendshipStatus getFriendshipStatus(int selfUserID, int targetUserID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=conn.prepareStatement("SELECT `follower_id`,`followee_id`,`mutual`,`accepted` FROM `followings` WHERE (`follower_id`=? AND `followee_id`=?) OR (`follower_id`=? AND `followee_id`=?) LIMIT 1");
			stmt.setInt(1, selfUserID);
			stmt.setInt(2, targetUserID);
			stmt.setInt(3, targetUserID);
			stmt.setInt(4, selfUserID);
			FriendshipStatus status;
			try(ResultSet res=stmt.executeQuery()){
				if(res.next()){
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

			stmt=conn.prepareStatement("SELECT count(*) FROM `friend_requests` WHERE `from_user_id`=? AND `to_user_id`=?");
			if(status==FriendshipStatus.FOLLOWING){
				stmt.setInt(1, selfUserID);
				stmt.setInt(2, targetUserID);
			}else{
				stmt.setInt(2, selfUserID);
				stmt.setInt(1, targetUserID);
			}
			try(ResultSet res=stmt.executeQuery()){
				res.next();
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
	}

	public static FriendshipStatus getSimpleFriendshipStatus(int selfUserID, int targetUserID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection();
			ResultSet res=new SQLQueryBuilder(conn)
				.selectFrom("followings")
				.columns("follower_id", "followee_id", "mutual", "accepted")
				.where("(follower_id=? AND followee_id=?) OR (follower_id=? AND followee_id=?)", selfUserID, targetUserID, targetUserID, selfUserID)
				.limit(1, 0)
				.execute()){
			if(res.next()){
				boolean mutual=res.getBoolean(3);
				boolean accepted=res.getBoolean(4);
				if(mutual)
					return FriendshipStatus.FRIENDS;
				int follower=res.getInt(1);
				int followee=res.getInt(2);
				if(follower==selfUserID && followee==targetUserID)
					return accepted ? FriendshipStatus.FOLLOWING : FriendshipStatus.FOLLOW_REQUESTED;
				else
					return FriendshipStatus.FOLLOWED_BY;
			}else{
				return FriendshipStatus.NONE;
			}
		}
	}

	public static Set<Integer> intersectWithFriendIDs(int selfUserID, Collection<Integer> userIDs) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("followings")
				.columns("followee_id")
				.whereIn("followee_id", userIDs)
				.andWhere("mutual=1")
				.andWhere("follower_id=?", selfUserID)
				.executeAndGetIntStream()
				.boxed()
				.collect(Collectors.toSet());
	}

	public static void putFriendRequest(int selfUserID, int targetUserID, String message, boolean followAccepted) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			DatabaseUtils.doWithTransaction(conn, ()->{
				new SQLQueryBuilder(conn)
						.insertInto("friend_requests")
						.value("from_user_id", selfUserID)
						.value("to_user_id", targetUserID)
						.value("message", message)
						.executeNoResult();
				int following=new SQLQueryBuilder(conn)
						.selectFrom("followings")
						.count()
						.where("follower_id=? AND followee_id=?", selfUserID, targetUserID)
						.executeAndGetInt();
				if(following==0){
					new SQLQueryBuilder(conn)
							.insertInto("followings")
							.value("follower_id", selfUserID)
							.value("followee_id", targetUserID)
							.value("accepted", followAccepted)
							.executeNoResult();
				}
				synchronized(NotificationsStorage.class){
					UserNotifications res=NotificationsStorage.getNotificationsFromCache(targetUserID);
					if(res!=null)
						res.incNewFriendRequestCount(1);
				}
			});
		}
	}

	public static PaginatedList<User> getFriendListForUser(int userID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("followings")
					.count()
					.where("followee_id=? AND mutual=1", userID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Integer> ids=new SQLQueryBuilder(conn)
					.selectFrom("followings")
					.columns("followee_id")
					.where("follower_id=? AND mutual=1", userID)
					.orderBy("followee_id ASC")
					.limit(count, offset)
					.executeAndGetIntList();
			return new PaginatedList<>(getByIdAsList(ids), total, offset, count);
		}
	}

	public static List<Integer> getFriendIDsForUser(int userID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("followings")
				.columns("followee_id")
				.where("follower_id=? AND mutual=1", userID)
				.executeAndGetIntList();
	}

	public static List<URI> getActivityPubFriendList(int userID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=conn.prepareStatement("SELECT followee_id, ap_id FROM followings JOIN `users` ON followee_id=users.id WHERE follower_id=? AND mutual=1 ORDER BY followee_id ASC LIMIT ? OFFSET ?");
			stmt.setInt(1, userID);
			stmt.setInt(2, count);
			stmt.setInt(3, offset);
			try(ResultSet res=stmt.executeQuery()){
				ArrayList<URI> ids=new ArrayList<>();
				while(res.next()){
					String apID=res.getString(2);
					ids.add(apID==null ? Config.localURI("/users/"+res.getInt(1)) : URI.create(apID));
				}
				return ids;
			}
		}
	}

	public static int getUserFriendsCount(int userID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("followings")
				.count()
				.where("follower_id=? AND mutual=1", userID)
				.executeAndGetInt();
	}

	public static int getUserFollowerOrFollowingCount(int userID, boolean followers) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("followings")
				.count()
				.where((followers ? "followee_id" : "follower_id")+"=? AND accepted=1 AND mutual=0", userID)
				.executeAndGetInt();
	}

	public static PaginatedList<User> getRandomFriendsForProfile(int userID, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("followings")
					.count()
					.where("follower_id=? AND mutual=1", userID)
					.executeAndGetInt();
			PreparedStatement stmt=conn.prepareStatement("SELECT followee_id FROM `followings` WHERE `follower_id`=? AND `mutual`=1 ORDER BY RAND() LIMIT 6");
			stmt.setInt(1, userID);
			try(ResultSet res=stmt.executeQuery()){
				return new PaginatedList<>(getByIdAsList(DatabaseUtils.intResultSetToList(res)), total, 0, count);
			}
		}
	}

	public static int getMutualFriendsCount(int userID, int otherUserID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=conn.prepareStatement("SELECT COUNT(*) FROM followings AS friends1 INNER JOIN followings AS friends2 ON friends1.followee_id=friends2.followee_id WHERE friends1.follower_id=? AND friends2.follower_id=? AND friends1.mutual=1 AND friends2.mutual=1");
			stmt.setInt(1, userID);
			stmt.setInt(2, otherUserID);
			return DatabaseUtils.oneFieldToInt(stmt.executeQuery());
		}
	}

	public static PaginatedList<User> getRandomMutualFriendsForProfile(int userID, int otherUserID, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=DatabaseUtils.oneFieldToInt(SQLQueryBuilder.prepareStatement(conn, "SELECT COUNT(*) FROM followings AS friends1 INNER JOIN followings AS friends2 ON friends1.followee_id=friends2.followee_id WHERE friends1.follower_id=? AND friends2.follower_id=? AND friends1.mutual=1 AND friends2.mutual=1", userID, otherUserID).executeQuery());
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT friends1.followee_id FROM followings AS friends1 INNER JOIN followings AS friends2 ON friends1.followee_id=friends2.followee_id WHERE friends1.follower_id=? AND friends2.follower_id=? AND friends1.mutual=1 AND friends2.mutual=1 ORDER BY RAND() LIMIT ?", userID, otherUserID, count);
			try(ResultSet res=stmt.executeQuery()){
				return new PaginatedList<>(getByIdAsList(DatabaseUtils.intResultSetToList(res)), total, 0, count);
			}
		}
	}

	public static List<Integer> getMutualFriendIDsForUser(int userID, int otherUserID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=conn.prepareStatement("SELECT friends1.followee_id FROM followings AS friends1 INNER JOIN followings AS friends2 ON friends1.followee_id=friends2.followee_id WHERE friends1.follower_id=? AND friends2.follower_id=? AND friends1.mutual=1 AND friends2.mutual=1 LIMIT ? OFFSET ?");
			stmt.setInt(1, userID);
			stmt.setInt(2, otherUserID);
			stmt.setInt(3, count);
			stmt.setInt(4, offset);
			return DatabaseUtils.intResultSetToList(stmt.executeQuery());
		}
	}

	public static PaginatedList<User> getMutualFriendListForUser(int userID, int otherUserID, int offset, int count) throws SQLException{
		return new PaginatedList<>(getByIdAsList(getMutualFriendIDsForUser(userID, otherUserID, offset, count)), getMutualFriendsCount(userID, otherUserID), offset, count);
	}

	public static PaginatedList<User> getNonMutualFollowers(int userID, boolean followers, boolean accepted, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			String fld1=followers ? "follower_id" : "followee_id";
			String fld2=followers ? "followee_id" : "follower_id";
			int total=new SQLQueryBuilder(conn)
					.selectFrom("followings")
					.count()
					.where(fld2+"=? AND accepted=? AND mutual=0", userID, accepted)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Integer> ids=new SQLQueryBuilder(conn)
					.selectFrom("followings")
					.columns(fld1)
					.where(fld2+"=? AND accepted=? AND mutual=0", userID, accepted)
					.orderBy(fld1+" ASC")
					.limit(count, offset)
					.executeAndGetIntList();
			return new PaginatedList<>(getByIdAsList(ids), total, offset, count);
		}
	}

	public static PaginatedList<FriendRequest> getIncomingFriendRequestsForUser(int userID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("friend_requests")
					.count()
					.where("to_user_id=?", userID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			// 1. collect the IDs of mutual friends for each friend request
			Map<Integer, List<Integer>> mutualFriendIDs=new HashMap<>();
			List<FriendRequest> reqs=new SQLQueryBuilder(conn)
					.selectFrom("friend_requests")
					.columns("message", "from_user_id")
					.where("to_user_id=?", userID)
					.orderBy("id DESC")
					.limit(count, offset)
					.executeAsStream(FriendRequest::fromResultSet)
					.peek(req->{
						try{
							req.mutualFriendsCount=getMutualFriendsCount(userID, req.from.id);
							if(req.mutualFriendsCount>0){
								mutualFriendIDs.put(req.from.id, getMutualFriendIDsForUser(userID, req.from.id, 0, 4));
							}
						}catch(SQLException x){
							LOG.warn("Exception while getting mutual friends for {} and {}", userID, req.from.id, x);
						}
					}).toList();
			if(mutualFriendIDs.isEmpty())
				return new PaginatedList<>(reqs, total, offset, count);
			// 2. make a list of distinct users we need
			Set<Integer> needUsers=mutualFriendIDs.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
			// 3. get them all in one go
			Map<Integer, User> mutualFriends=getById(needUsers);
			// 4. finally, put them into friend requests
			for(FriendRequest req: reqs){
				List<Integer> ids=mutualFriendIDs.get(req.from.id);
				if(ids==null)
					continue;
				req.mutualFriends=ids.stream().map(mutualFriends::get).collect(Collectors.toList());
			}
			return new PaginatedList<>(reqs, total, offset, count);
		}
	}

	public static boolean acceptFriendRequest(int userID, int targetUserID, boolean followAccepted) throws SQLException{
		boolean[] result={true};
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			DatabaseUtils.doWithTransaction(conn, ()->{
				PreparedStatement stmt=conn.prepareStatement("DELETE FROM `friend_requests` WHERE `from_user_id`=? AND `to_user_id`=?");
				stmt.setInt(1, targetUserID);
				stmt.setInt(2, userID);
				if(stmt.executeUpdate()!=1){
					conn.createStatement().execute("ROLLBACK");
					result[0]=false;
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
					conn.createStatement().execute("ROLLBACK");
					result[0]=false;
					return;
				}
				synchronized(NotificationsStorage.class){
					UserNotifications n=NotificationsStorage.getNotificationsFromCache(userID);
					if(n!=null)
						n.incNewFriendRequestCount(-1);
				}
				removeBirthdayReminderFromCache(List.of(userID, targetUserID));
			});
		}
		return result[0];
	}

	public static void deleteFriendRequest(int userID, int targetUserID) throws SQLException{
		int rows=new SQLQueryBuilder()
				.deleteFrom("friend_requests")
				.where("from_user_id=? AND to_user_id=?", targetUserID, userID)
				.executeUpdate();
		synchronized(NotificationsStorage.class){
			UserNotifications n=NotificationsStorage.getNotificationsFromCache(userID);
			if(n!=null)
				n.incNewFriendRequestCount(-rows);
		}
	}

	public static void unfriendUser(int userID, int targetUserID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			DatabaseUtils.doWithTransaction(conn, ()->{
				PreparedStatement stmt=conn.prepareStatement("DELETE FROM `followings` WHERE `follower_id`=? AND `followee_id`=?");
				stmt.setInt(1, userID);
				stmt.setInt(2, targetUserID);
				stmt.execute();
				stmt=conn.prepareStatement("UPDATE `followings` SET `mutual`=0 WHERE `follower_id`=? AND `followee_id`=?");
				stmt.setInt(1, targetUserID);
				stmt.setInt(2, userID);
				stmt.execute();
				removeBirthdayReminderFromCache(List.of(userID, targetUserID));
			});
		}
	}

	public static void followUser(int userID, int targetUserID, boolean accepted, boolean ignoreAlreadyFollowing) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			conn.createStatement().execute("START TRANSACTION");
			try{
				boolean mutual=false;
				PreparedStatement stmt=conn.prepareStatement("SELECT count(*) FROM `followings` WHERE `follower_id`=? AND `followee_id`=?");
				stmt.setInt(1, targetUserID);
				stmt.setInt(2, userID);
				try(ResultSet res=stmt.executeQuery()){
					res.next();
					mutual=res.getInt(1)==1;
				}
				stmt.setInt(1, userID);
				stmt.setInt(2, targetUserID);
				try(ResultSet res=stmt.executeQuery()){
					res.next();
					if(res.getInt(1)==1){
						if(ignoreAlreadyFollowing){
							conn.createStatement().execute("ROLLBACK");
							return;
						}
						throw new SQLException("Already following");
					}
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
					removeBirthdayReminderFromCache(List.of(userID, targetUserID));
				}

				conn.createStatement().execute("COMMIT");
			}catch(SQLException x){
				conn.createStatement().execute("ROLLBACK");
				throw new SQLException(x);
			}
		}
	}

	public static PaginatedList<SignupInvitation> getInvites(int userID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("signup_invitations")
					.count()
					.where("owner_id=? AND signups_remaining>0", userID)
					.executeAndGetInt();
			List<SignupInvitation> res=new SQLQueryBuilder(conn)
					.selectFrom("signup_invitations")
					.allColumns()
					.where("owner_id=? AND signups_remaining>0", userID)
					.orderBy("`created` DESC")
					.limit(count, offset)
					.executeAsStream(SignupInvitation::fromResultSet)
					.toList();
			return new PaginatedList<>(res, total, offset, count);
		}
	}

	public static int putInvite(int userID, byte[] code, int signups, String email, String extra) throws SQLException{
		return new SQLQueryBuilder()
				.insertInto("signup_invitations")
				.value("owner_id", userID)
				.value("code", code)
				.value("signups_remaining", signups)
				.value("email", email)
				.value("extra", extra)
				.executeAndGetID();
	}

	public static void changeBasicInfo(User user, String firstName, String lastName, String middleName, String maidenName, User.Gender gender, LocalDate bdate, String about, String aboutSource) throws SQLException{
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
				.value("about_source", aboutSource)
				.executeNoResult();
		synchronized(UserStorage.class){
			removeFromCache(user);
		}
		updateQSearchIndex(getById(user.id));
		removeBirthdayReminderFromCache(getFriendIDsForUser(user.id));
	}

	public static int getLocalUserCount() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("accounts")
				.count()
				.executeAndGetInt();
	}

	public static int getActiveLocalUserCount(long time) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("accounts")
				.count()
				.where("last_active>?", new Timestamp(System.currentTimeMillis()-time))
				.executeAndGetInt();
	}

	public static void updateProfilePicture(User user, String serializedPic) throws SQLException{
		new SQLQueryBuilder()
				.update("users")
				.value("avatar", serializedPic)
				.where("id=?", user.id)
				.executeNoResult();
		synchronized(UserStorage.class){
			removeFromCache(user);
		}
	}

	public static synchronized int putOrUpdateForeignUser(ForeignUser user) throws SQLException{
		if(user.isServiceActor)
			throw new IllegalArgumentException("Can't store a service actor as a user");

		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int existingUserID=new SQLQueryBuilder(conn)
					.selectFrom("users")
					.columns("id")
					.where("ap_id=?", Objects.toString(user.activityPubID))
					.executeAndGetInt();
			SQLQueryBuilder bldr=new SQLQueryBuilder(conn);
			if(existingUserID!=-1){
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
					.value("ap_shared_inbox", Objects.toString(user.sharedInbox, null))
					.value("ap_id", user.activityPubID.toString())
					.value("about", user.summary)
					.value("gender", user.gender)
					.value("avatar", user.icon!=null ? user.icon.get(0).asActivityPubObject(new JsonObject(), new SerializerContext(null, (String)null)).toString() : null)
					.value("profile_fields", user.serializeProfileFields())
					.value("flags", user.flags)
					.value("middle_name", user.middleName)
					.value("maiden_name", user.maidenName)
					.value("endpoints", user.serializeEndpoints())
					.value("privacy", user.privacySettings!=null ? Utils.gson.toJson(user.privacySettings) : null);

			boolean isNew=existingUserID==-1;
			if(isNew){
				existingUserID=bldr.executeAndGetID();
			}else{
				bldr.executeNoResult();
			}
			user.id=existingUserID;
			user.lastUpdated=Instant.now();
			putIntoCache(user);

			if(isNew){
				new SQLQueryBuilder(conn)
						.insertInto("qsearch_index")
						.value("user_id", existingUserID)
						.value("string", getQSearchStringForUser(user))
						.executeNoResult();
			}else{
				updateQSearchIndex(user);
			}

			return existingUserID;
		}catch(SQLIntegrityConstraintViolationException x){
			// Rare case: user with a matching username@domain but a different AP ID already exists
			try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
				int oldID=new SQLQueryBuilder(conn)
						.selectFrom("users")
						.columns("id")
						.where("username=? AND domain=? AND ap_id<>?", user.username, user.domain, user.activityPubID)
						.executeAndGetInt();
				if(oldID<=0){
					LOG.warn("Didn't find an existing user with username {}@{} while trying to deduplicate {}", user.username, user.domain, user.activityPubID);
					throw x;
				}
				LOG.info("Deduplicating user rows: username {}@{}, old local ID {}, new AP ID {}", user.username, user.domain, oldID, user.activityPubID);
				// Assign a temporary random username to this existing user row to get it out of the way
				new SQLQueryBuilder(conn)
						.update("users")
						.value("username", UUID.randomUUID().toString())
						.where("id=?", oldID)
						.executeNoResult();
				// Try again
				return putOrUpdateForeignUser(user);
			}
		}
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
		user=new SQLQueryBuilder()
				.selectFrom("users")
				.where("ap_id=?", apID)
				.executeAndGetSingleObject(ForeignUser::fromResultSet);
		if(user!=null){
			cacheByActivityPubID.put(apID, user);
			cache.put(user.id, user);
			cacheByUsername.put(user.getFullUsername().toLowerCase(), user);
		}
		return user;
	}

	public static List<URI> getFollowerInboxes(int userID) throws SQLException{
		return getFollowerInboxes(userID, Set.of());
	}

	public static List<URI> getFollowerInboxes(int userID, Set<Integer> except) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			String query="SELECT DISTINCT IFNULL(`ap_shared_inbox`, `ap_inbox`) FROM `users` WHERE `id` IN (SELECT `follower_id` FROM `followings` WHERE `followee_id`=?)";
			if(except!=null && !except.isEmpty()){
				query+=" AND `id` NOT IN ("+except.stream().map(Object::toString).collect(Collectors.joining(", "))+")";
			}
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, query, userID);
			return DatabaseUtils.resultSetToObjectStream(stmt.executeQuery(), r->{
				String url=r.getString(1);
				if(url==null)
					return null;
				return URI.create(url);
			}, null).filter(Objects::nonNull).collect(Collectors.toList());
		}
	}

	public static List<URI> getFriendInboxes(int userID, Set<Integer> except) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			String query="SELECT DISTINCT IFNULL(`ap_shared_inbox`, `ap_inbox`) FROM `users` WHERE `id` IN (SELECT `follower_id` FROM `followings` WHERE `followee_id`=? AND mutual=1)";
			if(except!=null && !except.isEmpty()){
				query+=" AND `id` NOT IN ("+except.stream().map(Object::toString).collect(Collectors.joining(", "))+")";
			}
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, query, userID);
			return DatabaseUtils.resultSetToObjectStream(stmt.executeQuery(), r->{
				String url=r.getString(1);
				if(url==null)
					return null;
				return URI.create(url);
			}, null).filter(Objects::nonNull).collect(Collectors.toList());
		}
	}

	public static List<URI> getUserFollowerURIs(int userID, boolean followers, int offset, int count, int[] total) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			String fld1=followers ? "follower_id" : "followee_id";
			String fld2=followers ? "followee_id" : "follower_id";
			if(total!=null){
				total[0]=new SQLQueryBuilder(conn)
						.selectFrom("followings")
						.count()
						.where(fld2+"=?", userID)
						.executeAndGetInt();
			}
			if(count>0){
				PreparedStatement stmt=conn.prepareStatement("SELECT `ap_id`,`id` FROM `followings` INNER JOIN `users` ON `users`.`id`=`"+fld1+"` WHERE `"+fld2+"`=? AND `accepted`=1 LIMIT ? OFFSET ?");
				stmt.setInt(1, userID);
				stmt.setInt(2, count);
				stmt.setInt(3, offset);
				ArrayList<URI> list=new ArrayList<>();
				try(ResultSet res=stmt.executeQuery()){
					while(res.next()){
						String _u=res.getString(1);
						if(_u==null){
							list.add(Config.localURI("/users/"+res.getInt(2)));
						}else{
							list.add(URI.create(_u));
						}
					}
				}
				return list;
			}
			return Collections.emptyList();
		}
	}

	public static List<Integer> getUserLocalFollowers(int userID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT follower_id FROM followings INNER JOIN `users` on `users`.id=follower_id WHERE followee_id=? AND accepted=1 AND `users`.ap_id IS NULL", userID);
			return DatabaseUtils.intResultSetToList(stmt.executeQuery());
		}
	}

	public static void setFollowAccepted(int followerID, int followeeID, boolean accepted) throws SQLException{
		new SQLQueryBuilder()
				.update("followings")
				.value("accepted", accepted)
				.where("follower_id=? AND followee_id=?", followerID, followeeID)
				.executeNoResult();
	}

	public static List<Account> getAllAccounts(int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=conn.prepareStatement("SELECT a1.*, a2.user_id AS inviter_user_id FROM accounts AS a1 LEFT JOIN accounts AS a2 ON a1.invited_by=a2.id LIMIT ?,?");
			stmt.setInt(1, offset);
			stmt.setInt(2, count);
			ArrayList<Account> accounts=new ArrayList<>();
			try(ResultSet res=stmt.executeQuery()){
				while(res.next()){
					Account acc=Account.fromResultSet(res);
					accounts.add(acc);
				}
			}
			return accounts;
		}
	}

	public static synchronized Account getAccount(int id) throws SQLException{
		Account acc=accountCache.get(id);
		if(acc!=null)
			return acc;
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=conn.prepareStatement("SELECT * FROM accounts WHERE id=?");
			stmt.setInt(1, id);
			try(ResultSet res=stmt.executeQuery()){
				if(res.next()){
					acc=Account.fromResultSet(res);
					accountCache.put(acc.id, acc);
					return acc;
				}
			}
		}
		return null;
	}

	public static Map<Integer, Account> getAccounts(Collection<Integer> ids) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("accounts")
				.allColumns()
				.whereIn("id", ids)
				.executeAsStream(Account::fromResultSet)
				.collect(Collectors.toMap(a->a.id, Function.identity()));
	}

	public static void setAccountRole(Account account, int role, int promotedBy) throws SQLException{
		new SQLQueryBuilder()
				.update("accounts")
				.value("role", role>0 ? role : null)
				.value("promoted_by", promotedBy>0 ? promotedBy : null)
				.where("id=?", account.id)
				.executeNoResult();
		synchronized(UserStorage.class){
			accountCache.remove(account.id);
		}
		SessionStorage.removeFromUserPermissionsCache(account.user.id);
	}

	public static synchronized void resetAccountsCache(){
		accountCache.evictAll();
	}

	public static List<User> getAdmins() throws SQLException{
		Set<Integer> rolesToShow=Config.userRoles.values()
				.stream()
				.filter(r->r.permissions().contains(UserRole.Permission.VISIBLE_IN_STAFF))
				.map(UserRole::id)
				.collect(Collectors.toSet());
		if(rolesToShow.isEmpty())
			return List.of();
		return getByIdAsList(new SQLQueryBuilder()
				.selectFrom("accounts")
				.columns("user_id")
				.whereIn("role", rolesToShow)
				.executeAndGetIntList());
	}

	public static int getPeerDomainCount() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("users")
				.selectExpr("COUNT(DISTINCT domain)")
				.executeAndGetInt()-1; // -1 for local domain (empty string)
	}

	public static List<String> getPeerDomains() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("users")
				.distinct()
				.columns("domain")
				.orderBy("domain asc")
				.executeAsStream(r->r.getString(1))
				.filter(s->s.length()>0)
				.toList();
	}

	public static boolean isUserBlocked(int ownerID, int targetID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("blocks_user_user")
				.count()
				.where("owner_id=? AND user_id=?", ownerID, targetID)
				.executeAndGetInt()==1;
	}

	public static void blockUser(int selfID, int targetID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			new SQLQueryBuilder(conn)
					.insertInto("blocks_user_user")
					.value("owner_id", selfID)
					.value("user_id", targetID)
					.executeNoResult();
			new SQLQueryBuilder(conn)
					.deleteFrom("followings")
					.where("(follower_id=? AND followee_id=?) OR (follower_id=? AND followee_id=?)", selfID, targetID, targetID, selfID)
					.executeNoResult();
			new SQLQueryBuilder(conn)
					.deleteFrom("friend_requests")
					.where("(from_user_id=? AND to_user_id=?) OR (from_user_id=? AND to_user_id=?)", selfID, targetID, targetID, selfID)
					.executeNoResult();
		}
	}

	public static void unblockUser(int selfID, int targetID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("blocks_user_user")
				.where("owner_id=? AND user_id=?", selfID, targetID)
				.executeNoResult();
	}

	public static List<User> getBlockedUsers(int selfID) throws SQLException{
		return getByIdAsList(new SQLQueryBuilder()
				.selectFrom("blocks_user_user")
				.columns("user_id")
				.where("owner_id=?", selfID)
				.executeAndGetIntList());
	}

	public static List<User> getBlockingUsers(int selfID) throws SQLException{
		return getByIdAsList(new SQLQueryBuilder()
				.selectFrom("blocks_user_user")
				.columns("owner_id")
				.where("user_id=?", selfID)
				.executeAndGetIntList());
	}

	public static boolean isDomainBlocked(int selfID, String domain) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("blocks_user_domain")
				.count()
				.where("owner_id=? AND domain=?", selfID, domain)
				.executeAndGetInt()==1;
	}

	public static List<String> getBlockedDomains(int selfID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("blocks_user_domain")
				.columns("domain")
				.where("owner_id=?", selfID)
				.executeAsStream(r->r.getString(1))
				.toList();
	}

	public static void blockDomain(int selfID, String domain) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("blocks_user_domain")
				.value("owner_id", selfID)
				.value("domain", domain)
				.executeNoResult();
	}

	public static void unblockDomain(int selfID, String domain) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("blocks_user_domain")
				.where("owner_id=? AND domain=?", selfID, domain)
				.executeNoResult();
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
				.executeNoResult();
		synchronized(UserStorage.class){
			accountCache.remove(accountID);
		}
	}

	static String getQSearchStringForUser(User user){
		StringBuilder sb=new StringBuilder(Utils.transliterate(user.firstName));
		if(user.lastName!=null){
			sb.append(' ');
			sb.append(Utils.transliterate(user.lastName));
		}
		if(user.middleName!=null){
			sb.append(' ');
			sb.append(Utils.transliterate(user.middleName));
		}
		if(user.maidenName!=null){
			sb.append(' ');
			sb.append(Utils.transliterate(user.maidenName));
		}
		sb.append(' ');
		sb.append(user.username);
		if(user.domain!=null){
			sb.append(' ');
			sb.append(user.domain);
		}
		return sb.toString();
	}

	static void updateQSearchIndex(User user) throws SQLException{
		new SQLQueryBuilder()
				.update("qsearch_index")
				.value("string", getQSearchStringForUser(user))
				.where("user_id=?", user.id)
				.executeNoResult();
	}

	public static void removeBirthdayReminderFromCache(List<Integer> userIDs){
		synchronized(birthdayReminderCache){
			for(Integer id:userIDs){
				birthdayReminderCache.remove(id);
			}
		}
	}

	public static void getFriendIdsWithBirthdaysTodayAndTomorrow(int userID, LocalDate date, List<Integer> today, List<Integer> tomorrow) throws SQLException{
		LocalDate nextDay=date.plusDays(1);
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT `users`.id, `users`.bdate FROM `users` RIGHT JOIN followings ON followings.followee_id=`users`.id"+
							" WHERE followings.follower_id=? AND followings.mutual=1 AND `users`.bdate IS NOT NULL"+
							" AND ((DAY(`users`.bdate)=? AND MONTH(`users`.bdate)=?) OR (DAY(`users`.bdate)=? AND MONTH(`users`.bdate)=?))",
					userID, date.getDayOfMonth(), date.getMonthValue(), nextDay.getDayOfMonth(), nextDay.getMonthValue());

			try(ResultSet res=stmt.executeQuery()){
				while(res.next()){
					int id=res.getInt(1);
					LocalDate bdate=DatabaseUtils.getLocalDate(res, 2);
					Objects.requireNonNull(bdate);
					if(bdate.getDayOfMonth()==date.getDayOfMonth()){
						today.add(id);
					}else{
						tomorrow.add(id);
					}
				}
			}
		}
	}

	public static BirthdayReminder getBirthdayReminderForUser(int userID, LocalDate date) throws SQLException{
		synchronized(birthdayReminderCache){
			BirthdayReminder r=birthdayReminderCache.get(userID);
			if(r!=null && r.forDay.equals(date))
				return r;
		}
		LocalDate nextDay=date.plusDays(1);
		ArrayList<Integer> today=new ArrayList<>(), tomorrow=new ArrayList<>();
		getFriendIdsWithBirthdaysTodayAndTomorrow(userID, date, today, tomorrow);
		BirthdayReminder r=new BirthdayReminder();
		r.forDay=date;
		if(!today.isEmpty()){
			r.day=date;
			r.userIDs=today;
		}else if(!tomorrow.isEmpty()){
			r.day=nextDay;
			r.userIDs=tomorrow;
		}else{
			r.userIDs=Collections.emptyList();
		}
		synchronized(birthdayReminderCache){
			birthdayReminderCache.put(userID, r);
			return r;
		}
	}

	public static List<Integer> getFriendsWithBirthdaysInMonth(int userID, int month) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT `users`.id FROM `users` RIGHT JOIN followings ON followings.followee_id=`users`.id"+
					" WHERE followings.follower_id=? AND followings.mutual=1 AND `users`.bdate IS NOT NULL AND MONTH(`users`.bdate)=?", userID, month);
			return DatabaseUtils.intResultSetToList(stmt.executeQuery());
		}
	}

	public static List<Integer> getFriendsWithBirthdaysOnDay(int userID, int month, int day) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT `users`.id FROM `users` RIGHT JOIN followings ON followings.followee_id=`users`.id"+
					" WHERE followings.follower_id=? AND followings.mutual=1 AND `users`.bdate IS NOT NULL AND MONTH(`users`.bdate)=? AND DAY(`users`.bdate)=?", userID, month, day);
			return DatabaseUtils.intResultSetToList(stmt.executeQuery());
		}
	}

	public static Map<URI, Integer> getFriendsByActivityPubIDs(Collection<URI> ids, int userID) throws SQLException{
		if(ids.isEmpty())
			return Map.of();
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			ArrayList<Integer> localIDs=new ArrayList<>();
			ArrayList<String> remoteIDs=new ArrayList<>();
			for(URI id: ids){
				if(Config.isLocal(id)){
					String path=id.getPath();
					if(StringUtils.isEmpty(path))
						continue;
					String[] pathSegments=path.split("/");
					if(pathSegments.length!=3 || !"users".equals(pathSegments[1])) // "", "users", id
						continue;
					int uid=Utils.safeParseInt(pathSegments[2]);
					if(uid>0)
						localIDs.add(uid);
				}else{
					remoteIDs.add(id.toString());
				}
			}
			HashMap<Integer, URI> localIdToApIdMap=new HashMap<>();
			if(!remoteIDs.isEmpty()){
				try(ResultSet res=new SQLQueryBuilder(conn).selectFrom("users").columns("id", "ap_id").whereIn("ap_id", remoteIDs).execute()){
					while(res.next()){
						int localID=res.getInt(1);
						localIDs.add(localID);
						localIdToApIdMap.put(localID, URI.create(res.getString(2)));
					}
				}
			}
			if(localIDs.isEmpty())
				return Map.of();
			return new SQLQueryBuilder(conn)
					.selectFrom("followings")
					.columns("followee_id")
					.whereIn("followee_id", localIDs)
					.andWhere("mutual=1 AND accepted=1 AND follower_id=?", userID)
					.executeAsStream(res->res.getInt(1))
					.collect(Collectors.toMap(id->localIdToApIdMap.computeIfAbsent(id, UserStorage::localUserURI), Function.identity()));
		}
	}

	private static URI localUserURI(int id){
		return Config.localURI("/users/"+id);
	}

	public static int getLocalFollowersCount(int userID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT COUNT(*) FROM `followings` JOIN `users` ON `follower_id`=`users`.id WHERE followee_id=? AND accepted=1 AND `users`.domain=''", userID);
			return DatabaseUtils.oneFieldToInt(stmt.executeQuery());
		}
	}

	public static void setPrivacySettings(User user, Map<UserPrivacySettingKey, PrivacySetting> settings) throws SQLException{
		new SQLQueryBuilder()
				.update("users")
				.value("privacy", Utils.gson.toJson(settings))
				.where("id=?", user.id)
				.executeNoResult();
		removeFromCache(user);
	}

	public static Set<Integer> getFriendIDsWithDomain(int userID, String domain) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT id FROM followings JOIN `users` ON `followee_id`=`users`.id WHERE follower_id=? AND mutual=1 AND `users`.domain=?", userID, domain);
			return DatabaseUtils.resultSetToObjectStream(stmt.executeQuery(), rs->rs.getInt(1), null).collect(Collectors.toSet());
		}
	}

	public static int getCountOfFriendsOfFriendsWithDomain(int userID, String domain) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn,
					"SELECT COUNT(*) FROM followings JOIN `users` ON followings.follower_id=`users`.id WHERE followee_id IN (SELECT follower_id FROM followings WHERE followee_id=? AND mutual=1) AND mutual=1 AND `users`.domain=?",
					userID, domain);
			return DatabaseUtils.oneFieldToInt(stmt.executeQuery());
		}
	}

	public static void deleteUser(User user) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("users")
				.where("id=?", user.id)
				.executeNoResult();
		removeFromCache(user);
	}

	public static void deleteAccount(Account account) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			// Delete media file refs first because triggers don't trigger on cascade deletes. Argh.
			new SQLQueryBuilder(conn)
					.deleteFrom("media_file_refs")
					.where("owner_user_id=?", account.user.id)
					.executeNoResult();

			new SQLQueryBuilder(conn)
					.deleteFrom("accounts")
					.where("id=?", account.id)
					.executeNoResult();
			new SQLQueryBuilder(conn)
					.deleteFrom("users")
					.where("id=?", account.user.id)
					.executeNoResult();
			removeFromCache(account.user);
			accountCache.remove(account.id);
		}
	}

	public static void removeAccountFromCache(int id){
		accountCache.remove(id);
	}

	public static void setUserBanStatus(User user, Account userAccount, UserBanStatus status, String banInfo) throws SQLException{
		new SQLQueryBuilder()
				.update("users")
				.value("ban_status", status)
				.value("ban_info", banInfo)
				.where("id=?", user.id)
				.executeNoResult();
		removeFromCache(user);
		accountCache.remove(userAccount.id);
	}

	public static List<User> getTerminallyBannedUsers() throws SQLException{
		return getByIdAsList(new SQLQueryBuilder()
				.selectFrom("users")
				.columns("id")
				.whereIn("ban_status", UserBanStatus.SELF_DEACTIVATED, UserBanStatus.SUSPENDED)
				.executeAndGetIntList());
	}
}
