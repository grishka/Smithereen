package smithereen.storage;

import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.model.Account;
import smithereen.model.AdminNotifications;
import smithereen.model.EmailCode;
import smithereen.model.Group;
import smithereen.model.OtherSession;
import smithereen.model.PaginatedList;
import smithereen.model.SessionInfo;
import smithereen.model.SignupInvitation;
import smithereen.model.SignupRequest;
import smithereen.model.User;
import smithereen.model.UserBanStatus;
import smithereen.model.UserPermissions;
import smithereen.model.UserPreferences;
import smithereen.model.UserRole;
import smithereen.model.notifications.Notification;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import spark.Request;
import spark.Session;

public class SessionStorage{

	private static final SecureRandom random=new SecureRandom();

	private static final LruCache<Integer, UserPermissions> permissionsCache=new LruCache<>(500);

	public static String putNewSession(@NotNull Session sess, String userAgent, InetAddress ip) throws SQLException{
		byte[] sid=new byte[64];
		SessionInfo info=sess.attribute("info");
		Account account=info.account;
		if(account==null)
			throw new IllegalArgumentException("putNewSession requires a logged in session");
		random.nextBytes(sid);
		long uaHash=Utils.hashUserAgent(userAgent);
		new SQLQueryBuilder()
				.insertIgnoreInto("user_agents")
				.value("hash", uaHash)
				.value("user_agent", userAgent)
				.executeNoResult();
		new SQLQueryBuilder()
				.insertInto("sessions")
				.value("id", sid)
				.value("account_id", account.id)
				.value("user_agent", uaHash)
				.value("ip", Utils.serializeInetAddress(ip))
				.executeNoResult();
		return Base64.getEncoder().encodeToString(sid);
	}

	public static boolean fillSession(String psid, Session sess, Request req) throws SQLException{
		byte[] sid;
		try{
			sid=Base64.getDecoder().decode(psid);
		}catch(Exception x){
			return false;
		}
		if(sid.length!=64)
			return false;

		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			record SessionRow(int accountID, InetAddress ip, long uaHash){}
			SessionRow sr=new SQLQueryBuilder(conn)
					.selectFrom("sessions")
					.allColumns()
					.where("id=?", (Object)sid)
					.executeAndGetSingleObject(r->{
						try{
							return new SessionRow(r.getInt("account_id"), InetAddress.getByAddress(r.getBytes("ip")), r.getLong("user_agent"));
						}catch(UnknownHostException e){
							throw new RuntimeException(e);
						}
					});
			if(sr==null)
				return false;
			Account acc=new SQLQueryBuilder(conn)
					.selectFrom("accounts")
					.allColumns()
					.where("id=?", sr.accountID)
					.executeAndGetSingleObject(Account::fromResultSet);
			if(acc==null)
				return false;
			SessionInfo info=new SessionInfo();
			info.account=acc;
			info.csrfToken=Utils.csrfTokenFromSessionID(sid);
			info.ip=sr.ip;
			info.userAgentHash=sr.uaHash;
			if(info.account.prefs.locale==null){
				Locale requestLocale=req.raw().getLocale();
				if(requestLocale!=null){
					info.account.prefs.locale=requestLocale;
					SessionStorage.updatePreferences(info.account.id, info.account.prefs);
				}
			}
			sess.attribute("info", info);
		}
		return true;
	}

	public static Account getAccountForUsernameAndPassword(@NotNull String usernameOrEmail, @NotNull String password) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			MessageDigest md=MessageDigest.getInstance("SHA-256");
			byte[] hashedPassword=md.digest(password.getBytes(StandardCharsets.UTF_8));
			PreparedStatement stmt;
			if(usernameOrEmail.contains("@")){
				stmt=conn.prepareStatement("SELECT * FROM `accounts` WHERE `email`=? AND `password`=?");
			}else{
				stmt=conn.prepareStatement("SELECT * FROM `accounts` WHERE `user_id` IN (SELECT `id` FROM `users` WHERE `username`=?) AND `password`=?");
			}
			stmt.setString(1, usernameOrEmail);
			stmt.setBytes(2, hashedPassword);
			try(ResultSet res=stmt.executeQuery()){
				if(res.next()){
					return Account.fromResultSet(res);
				}
				return null;
			}
		}catch(NoSuchAlgorithmException ignore){}
		throw new AssertionError();
	}

	public static void deleteSession(@NotNull String psid) throws SQLException{
		byte[] sid=Base64.getDecoder().decode(psid);
		if(sid.length!=64)
			return;

		new SQLQueryBuilder()
				.deleteFrom("sessions")
				.where("id=?", (Object) sid)
				.executeNoResult();
	}

	public static void deleteSession(int accountID, @NotNull byte[] sid) throws SQLException{
		if(sid.length!=64)
			return;

		new SQLQueryBuilder()
				.deleteFrom("sessions")
				.where("id=? AND account_id=?", (Object) sid, accountID)
				.executeNoResult();
	}

	public static SignupResult registerNewAccount(@NotNull String username, @NotNull String password, @NotNull String email, @NotNull String firstName, @NotNull String lastName, @NotNull User.Gender gender, @NotNull String invite) throws SQLException{
		SignupResult[] result={SignupResult.SUCCESS};
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			DatabaseUtils.doWithTransaction(conn, ()->{
				SignupInvitation inv=getInvitationByCode(Utils.hexStringToByteArray(invite));
				PreparedStatement stmt=conn.prepareStatement("UPDATE `signup_invitations` SET `signups_remaining`=`signups_remaining`-1 WHERE `signups_remaining`>0 AND `code`=?");
				stmt.setBytes(1, Utils.hexStringToByteArray(invite));
				if(stmt.executeUpdate()!=1){
					result[0]=SignupResult.INVITE_INVALID;
					return;
				}

				new SQLQueryBuilder(conn)
						.deleteFrom("signup_requests")
						.where("email=?", email)
						.executeNoResult();

				int inviterAccountID=inv.ownerID;

				KeyPairGenerator kpg;
				MessageDigest md;
				try{
					kpg=KeyPairGenerator.getInstance("RSA");
					md=MessageDigest.getInstance("SHA-256");
				}catch(NoSuchAlgorithmException x){
					throw new RuntimeException(x);
				}
				kpg.initialize(2048);
				KeyPair pair=kpg.generateKeyPair();

				stmt=conn.prepareStatement("INSERT INTO `users` (`fname`, `lname`, `username`, `public_key`, `private_key`, gender) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				stmt.setString(1, firstName);
				stmt.setString(2, lastName);
				stmt.setString(3, username);
				stmt.setBytes(4, pair.getPublic().getEncoded());
				stmt.setBytes(5, pair.getPrivate().getEncoded());
				stmt.setInt(6, gender.ordinal());
				stmt.execute();
				int userID;
				try(ResultSet res=stmt.getGeneratedKeys()){
					res.next();
					userID=res.getInt(1);
				}

				byte[] hashedPassword=md.digest(password.getBytes(StandardCharsets.UTF_8));
				stmt=conn.prepareStatement("INSERT INTO `accounts` (`user_id`, `email`, `password`, `invited_by`) VALUES (?, ?, ?, ?)");
				stmt.setInt(1, userID);
				stmt.setString(2, email);
				stmt.setBytes(3, hashedPassword);
				if(inviterAccountID!=0)
					stmt.setInt(4, inviterAccountID);
				else
					stmt.setNull(4, Types.INTEGER);
				stmt.execute();

				int inviterUserID=0;
				if(inviterAccountID!=0){
					stmt=conn.prepareStatement("SELECT `user_id` FROM `accounts` WHERE `id`=?");
					stmt.setInt(1, inviterAccountID);
					try(ResultSet res=stmt.executeQuery()){
						res.next();
						inviterUserID=res.getInt(1);
					}

					if(!inv.noAddFriend){
						stmt=conn.prepareStatement("INSERT INTO `followings` (`follower_id`, `followee_id`, `mutual`) VALUES (?, ?, 1), (?, ?, 1)");
						stmt.setInt(1, inviterUserID);
						stmt.setInt(2, userID);
						stmt.setInt(3, userID);
						stmt.setInt(4, inviterUserID);
						stmt.execute();
					}
				}

				conn.createStatement().execute("COMMIT");

				if(inviterUserID!=0){
					Notification n=new Notification();
					n.actorID=userID;
					n.type=Notification.Type.INVITE_SIGNUP;
					NotificationsStorage.putNotification(inviterUserID, n);
				}

				new SQLQueryBuilder(conn)
						.insertInto("qsearch_index")
						.value("user_id", userID)
						.value("string", UserStorage.getQSearchStringForUser(Objects.requireNonNull(UserStorage.getById(userID))))
						.executeNoResult();
			});
		}
		return result[0];
	}

	public static SignupResult registerNewAccount(@NotNull String username, @NotNull String password, @NotNull String email, @NotNull String firstName, @NotNull String lastName, @NotNull User.Gender gender) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			DatabaseUtils.doWithTransaction(conn, ()->{
				new SQLQueryBuilder(conn)
						.deleteFrom("signup_requests")
						.where("email=?", email)
						.executeNoResult();

				KeyPairGenerator kpg;
				MessageDigest md;
				try{
					kpg=KeyPairGenerator.getInstance("RSA");
					md=MessageDigest.getInstance("SHA-256");
				}catch(NoSuchAlgorithmException x){
					throw new RuntimeException(x);
				}
				kpg.initialize(2048);
				KeyPair pair=kpg.generateKeyPair();

				PreparedStatement stmt=conn.prepareStatement("INSERT INTO `users` (`fname`, `lname`, `username`, `public_key`, `private_key`, gender) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
				stmt.setString(1, firstName);
				stmt.setString(2, lastName);
				stmt.setString(3, username);
				stmt.setBytes(4, pair.getPublic().getEncoded());
				stmt.setBytes(5, pair.getPrivate().getEncoded());
				stmt.setInt(6, gender.ordinal());
				stmt.execute();
				int userID;
				try(ResultSet res=stmt.getGeneratedKeys()){
					res.next();
					userID=res.getInt(1);
				}

				byte[] hashedPassword=md.digest(password.getBytes(StandardCharsets.UTF_8));
				stmt=conn.prepareStatement("INSERT INTO `accounts` (`user_id`, `email`, `password`, `invited_by`) VALUES (?, ?, ?, ?)");
				stmt.setInt(1, userID);
				stmt.setString(2, email);
				stmt.setBytes(3, hashedPassword);
				stmt.setNull(4, Types.INTEGER);
				stmt.execute();

				conn.createStatement().execute("COMMIT");
				new SQLQueryBuilder(conn)
						.insertInto("qsearch_index")
						.value("user_id", userID)
						.value("string", UserStorage.getQSearchStringForUser(Objects.requireNonNull(UserStorage.getById(userID))))
						.executeNoResult();
			});
		}
		return SignupResult.SUCCESS;
	}

	public static void updateActivationInfo(int accountID, Account.ActivationInfo info) throws SQLException{
		new SQLQueryBuilder()
				.update("accounts")
				.value("activation_info", info==null ? null : Utils.gson.toJson(info))
				.where("id=?", accountID)
				.executeNoResult();
	}

	public static boolean updatePassword(int accountID, String oldPassword, String newPassword) throws SQLException{
		try{
			MessageDigest md=MessageDigest.getInstance("SHA-256");
			byte[] hashedOld=md.digest(oldPassword.getBytes(StandardCharsets.UTF_8));
			byte[] hashedNew=md.digest(newPassword.getBytes(StandardCharsets.UTF_8));
			return new SQLQueryBuilder()
					.update("accounts")
					.value("password", hashedNew)
					.where("id=? AND `password`=?", accountID, hashedOld)
					.executeUpdate()==1;
		}catch(NoSuchAlgorithmException ignore){}
		return false;
	}

	public static void updateEmail(int accountID, String email) throws SQLException{
		new SQLQueryBuilder()
				.update("accounts")
				.value("email", email)
				.where("id=?", accountID)
				.executeNoResult();
	}

	public static void updatePreferences(int accountID, UserPreferences prefs) throws SQLException{
		new SQLQueryBuilder()
				.update("accounts")
				.value("preferences", Utils.gson.toJson(prefs))
				.where("id=?", accountID)
				.executeNoResult();
	}

	public static Account getAccountByEmail(String email) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("accounts")
				.allColumns()
				.where("email=?", email)
				.executeAndGetSingleObject(Account::fromResultSet);
	}

	public static Account getAccountByUsername(String username) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int uid=new SQLQueryBuilder(conn)
					.selectFrom("users")
					.columns("id")
					.where("username=? AND ap_id IS NULL", username)
					.executeAndGetInt();
			if(uid==-1)
				return null;
			return new SQLQueryBuilder(conn)
					.selectFrom("accounts")
					.allColumns()
					.where("user_id=?", uid)
					.executeAndGetSingleObject(Account::fromResultSet);
		}
	}

	public static Account getAccountByUserID(int userID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("accounts")
				.allColumns()
				.where("user_id=?", userID)
				.executeAndGetSingleObject(Account::fromResultSet);
	}

	public static String storeEmailCode(EmailCode code) throws SQLException{
		byte[] _id=new byte[64];
		random.nextBytes(_id);
		String id=Base64.getUrlEncoder().withoutPadding().encodeToString(_id);
		new SQLQueryBuilder()
				.insertInto("email_codes")
				.value("code", _id)
				.value("account_id", code.accountID)
				.value("type", code.type)
				.value("extra", Objects.toString(code.extra, null))
				.executeNoResult();
		return id;
	}

	public static EmailCode getEmailCode(String id) throws SQLException{
		byte[] _id;
		try{
			_id=Base64.getUrlDecoder().decode(id);
		}catch(IllegalArgumentException x){
			return null;
		}
		if(_id.length!=64)
			return null;
		return new SQLQueryBuilder()
				.selectFrom("email_codes")
				.allColumns()
				.where("code=?", (Object) _id)
				.executeAndGetSingleObject(res->{
					EmailCode code=new EmailCode();
					code.accountID=res.getInt("account_id");
					code.type=EmailCode.Type.values()[res.getInt("type")];
					String extra=res.getString("extra");
					if(extra!=null)
						code.extra=JsonParser.parseString(extra).getAsJsonObject();
					code.createdAt=res.getTimestamp("created_at");
					return code;
				});
	}

	public static void deleteEmailCode(String id) throws SQLException{
		byte[] _id;
		try{
			_id=Base64.getUrlDecoder().decode(id);
		}catch(IllegalArgumentException x){
			return;
		}
		if(_id.length!=64)
			return;
		new SQLQueryBuilder()
				.deleteFrom("email_codes")
				.where("code=?", (Object) _id)
				.executeNoResult();
	}

	public static void deleteExpiredEmailCodes() throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("email_codes")
				.where("created_at<?", new Timestamp(System.currentTimeMillis()-EmailCode.VALIDITY_MS))
				.executeNoResult();
	}

	public static boolean updatePassword(int accountID, String newPassword) throws SQLException{
		try{
			MessageDigest md=MessageDigest.getInstance("SHA-256");
			byte[] hashedNew=md.digest(newPassword.getBytes(StandardCharsets.UTF_8));
			return new SQLQueryBuilder()
					.update("accounts")
					.value("password", hashedNew)
					.where("id=?", accountID)
					.executeUpdate()==1;
		}catch(NoSuchAlgorithmException ignore){}
		return false;
	}

	public static void setLastActive(int accountID, String psid, Instant time, InetAddress lastIP, String userAgent, long uaHash) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			byte[] serializedIP=Utils.serializeInetAddress(lastIP);
			new SQLQueryBuilder(conn)
					.update("accounts")
					.value("last_active", time)
					.value("last_ip", serializedIP)
					.where("id=?", accountID)
					.executeNoResult();
			byte[] sid=Base64.getDecoder().decode(psid);
			new SQLQueryBuilder(conn)
					.update("sessions")
					.value("last_active", time)
					.value("ip", serializedIP)
					.value("user_agent", uaHash)
					.where("id=?", (Object) sid)
					.executeNoResult();
			new SQLQueryBuilder(conn)
					.insertIgnoreInto("user_agents")
					.value("hash", uaHash)
					.value("user_agent", userAgent)
					.executeNoResult();
		}
	}
//
//	public static void setIpAndUserAgent(String psid, InetAddress ip, String userAgent, long uaHash) throws SQLException{
//		byte[] sid=Base64.getDecoder().decode(psid);
//		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
//			new SQLQueryBuilder(conn)
//					.update("sessions")
//					.where("id=?", (Object) sid)
//					.value("ip", Utils.serializeInetAddress(ip))
//					.value("user_agent", uaHash)
//					.executeNoResult();
//		}
//	}

	public static synchronized void removeFromUserPermissionsCache(int userID){
		permissionsCache.remove(userID);
	}

	public static synchronized void resetPermissionsCache(){
		permissionsCache.evictAll();
	}

	public static synchronized UserPermissions getUserPermissions(Account account) throws SQLException{
		UserPermissions r=permissionsCache.get(account.user.id);
		if(r!=null)
			return r;
		r=new UserPermissions(account);
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=new SQLQueryBuilder(conn)
					.selectFrom("group_admins")
					.columns("group_id", "level")
					.where("user_id=?", account.user.id)
					.createStatement();
			try(ResultSet res=stmt.executeQuery()){
				while(res.next()){
					r.managedGroups.put(res.getInt(1), Group.AdminLevel.values()[res.getInt(2)]);
				}
			}
			stmt.close();
		}
		r.canInviteNewUsers=switch(Config.signupMode){
			case OPEN, INVITE_ONLY -> account.user.banStatus==UserBanStatus.NONE || account.user.banStatus==UserBanStatus.HIDDEN;
			case CLOSED, MANUAL_APPROVAL -> r.hasPermission(UserRole.Permission.MANAGE_INVITES);
		};
		permissionsCache.put(account.user.id, r);
		return r;
	}

	public static boolean isEmailInvited(String email) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("signup_invitations")
				.count()
				.where("email=?", email)
				.executeAndGetInt()>0;
	}

	public static SignupInvitation getInvitationByID(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("signup_invitations")
				.allColumns()
				.where("id=? AND signups_remaining>0", id)
				.executeAndGetSingleObject(SignupInvitation::fromResultSet);
	}

	public static SignupInvitation getInvitationByCode(byte[] code) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("signup_invitations")
				.allColumns()
				.where("code=? AND signups_remaining>0", (Object) code)
				.executeAndGetSingleObject(SignupInvitation::fromResultSet);
	}

	public static void deleteInvitation(int id) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("signup_invitations")
				.where("id=?", id)
				.executeNoResult();
	}

	public static PaginatedList<User> getInvitedUsers(int selfAccountID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("accounts")
					.count()
					.where("invited_by=?", selfAccountID)
					.executeAndGetInt();
			if(total==0){
				return PaginatedList.emptyList(count);
			}
			return new PaginatedList<>(UserStorage.getByIdAsList(new SQLQueryBuilder(conn)
					.selectFrom("accounts")
					.columns("user_id")
					.where("invited_by=?", selfAccountID)
					.limit(count, offset)
					.executeAndGetIntList()), total, offset, count);
		}
	}

	public static boolean isThereInviteRequestWithEmail(String email) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("signup_requests")
				.count()
				.where("email=?", email)
				.executeAndGetInt()>0;
	}

	public static void putInviteRequest(String email, String firstName, String lastName, String reason) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("signup_requests")
				.value("email", email)
				.value("first_name", firstName)
				.value("last_name", lastName)
				.value("reason", reason)
				.executeNoResult();
		AdminNotifications an=AdminNotifications.getInstance(null);
		if(an!=null){
			an.signupRequestsCount=getInviteRequestCount();
		}
	}

	public static int getInviteRequestCount() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("signup_requests")
				.count()
				.executeAndGetInt();
	}

	public static PaginatedList<SignupRequest> getInviteRequests(int offset, int count) throws SQLException{
		int total=getInviteRequestCount();
		if(total==0)
			return PaginatedList.emptyList(count);
		return new PaginatedList<>(new SQLQueryBuilder()
				.selectFrom("signup_requests")
				.allColumns()
				.limit(count, offset)
				.orderBy("id DESC")
				.executeAsStream(SignupRequest::fromResultSet)
				.toList(), total, offset, count);
	}

	public static void deleteInviteRequest(int id) throws SQLException{
		int numRows=new SQLQueryBuilder()
				.deleteFrom("signup_requests")
				.where("id=?", id)
				.executeUpdate();
		AdminNotifications an=AdminNotifications.getInstance(null);
		if(an!=null)
			an.signupRequestsCount=Math.max(0, an.signupRequestsCount-numRows);
	}

	public static SignupRequest getInviteRequest(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("signup_requests")
				.allColumns()
				.where("id=?", id)
				.executeAndGetSingleObject(SignupRequest::fromResultSet);
	}

	public static SignupRequest getInviteRequestByEmail(String email) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("signup_requests")
				.allColumns()
				.where("email=?", email)
				.executeAndGetSingleObject(SignupRequest::fromResultSet);
	}

	public static List<OtherSession> getAccountSessions(int accountID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT sessions.*, user_agents.user_agent AS user_agent_str FROM sessions LEFT JOIN user_agents ON sessions.user_agent=user_agents.hash WHERE account_id=? ORDER BY last_active DESC", accountID);
			try(ResultSet res=stmt.executeQuery()){
				return DatabaseUtils.resultSetToObjectStream(res, OtherSession::fromResultSet, null).toList();
			}
		}
	}

	public static void deleteSessionsExcept(int accountID, byte[] sid) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("sessions")
				.where("account_id=? AND id<>?", accountID, sid)
				.executeNoResult();
	}

	public enum SignupResult{
		SUCCESS,
		USERNAME_TAKEN,
		INVITE_INVALID
	}
}
