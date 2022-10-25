package smithereen.storage;

import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
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
import smithereen.data.Account;
import smithereen.data.AdminNotifications;
import smithereen.data.EmailCode;
import smithereen.data.Group;
import smithereen.data.PaginatedList;
import smithereen.data.SessionInfo;
import smithereen.data.SignupInvitation;
import smithereen.data.SignupRequest;
import smithereen.data.User;
import smithereen.data.UserPermissions;
import smithereen.data.UserPreferences;
import smithereen.data.notifications.Notification;
import spark.Request;
import spark.Session;

public class SessionStorage{

	private static SecureRandom random=new SecureRandom();

	private static LruCache<Integer, UserPermissions> permissionsCache=new LruCache<>(500);

	public static String putNewSession(@NotNull Session sess) throws SQLException{
		byte[] sid=new byte[64];
		SessionInfo info=sess.attribute("info");
		Account account=info.account;
		if(account==null)
			throw new IllegalArgumentException("putNewSession requires a logged in session");
		random.nextBytes(sid);
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("INSERT INTO `sessions` (`id`, `account_id`) VALUES (?, ?)");
		stmt.setBytes(1, sid);
		stmt.setInt(2, account.id);
		stmt.execute();
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

		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT * FROM `accounts` WHERE `id` IN (SELECT `account_id` FROM `sessions` WHERE `id`=?)");
		stmt.setBytes(1, sid);
		try(ResultSet res=stmt.executeQuery()){
			if(!res.first())
				return false;
			SessionInfo info=new SessionInfo();
			info.account=Account.fromResultSet(res);
			info.csrfToken=Utils.csrfTokenFromSessionID(sid);
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
		try{
			MessageDigest md=MessageDigest.getInstance("SHA-256");
			byte[] hashedPassword=md.digest(password.getBytes(StandardCharsets.UTF_8));
			PreparedStatement stmt;
			if(usernameOrEmail.contains("@")){
				stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT * FROM `accounts` WHERE `email`=? AND `password`=?");
			}else{
				stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT * FROM `accounts` WHERE `user_id` IN (SELECT `id` FROM `users` WHERE `username`=?) AND `password`=?");
			}
			stmt.setString(1, usernameOrEmail);
			stmt.setBytes(2, hashedPassword);
			try(ResultSet res=stmt.executeQuery()){
				if(res.first()){
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

		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("DELETE FROM `sessions` WHERE `id`=?");
		stmt.setBytes(1, sid);
		stmt.execute();
	}

	public static SignupResult registerNewAccount(@NotNull String username, @NotNull String password, @NotNull String email, @NotNull String firstName, @NotNull String lastName, @NotNull User.Gender gender, @NotNull String invite) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		conn.createStatement().execute("START TRANSACTION");
		try{
			SignupInvitation inv=getInvitationByCode(Utils.hexStringToByteArray(invite));
			PreparedStatement stmt=conn.prepareStatement("UPDATE `signup_invitations` SET `signups_remaining`=`signups_remaining`-1 WHERE `signups_remaining`>0 AND `code`=?");
			stmt.setBytes(1, Utils.hexStringToByteArray(invite));
			if(stmt.executeUpdate()!=1){
				conn.createStatement().execute("ROLLBACK");
				return SignupResult.INVITE_INVALID;
			}

			new SQLQueryBuilder(conn)
					.deleteFrom("signup_requests")
					.where("email=?", email)
					.executeNoResult();

			int inviterAccountID=inv.ownerID;

			KeyPairGenerator kpg=KeyPairGenerator.getInstance("RSA");
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
				res.first();
				userID=res.getInt(1);
			}

			MessageDigest md=MessageDigest.getInstance("SHA-256");
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
					res.first();
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
					.value("string", UserStorage.getQSearchStringForUser(UserStorage.getById(userID)))
					.createStatement()
					.execute();
		}catch(SQLException x){
			conn.createStatement().execute("ROLLBACK");
			throw new SQLException(x);
		}catch(NoSuchAlgorithmException ignore){}
		return SignupResult.SUCCESS;
	}

	public static SignupResult registerNewAccount(@NotNull String username, @NotNull String password, @NotNull String email, @NotNull String firstName, @NotNull String lastName, @NotNull User.Gender gender) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		conn.createStatement().execute("START TRANSACTION");
		try{
			new SQLQueryBuilder(conn)
					.deleteFrom("signup_requests")
					.where("email=?", email)
					.executeNoResult();

			KeyPairGenerator kpg=KeyPairGenerator.getInstance("RSA");
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
				res.first();
				userID=res.getInt(1);
			}

			MessageDigest md=MessageDigest.getInstance("SHA-256");
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
					.value("string", UserStorage.getQSearchStringForUser(UserStorage.getById(userID)))
					.createStatement()
					.execute();
		}catch(SQLException x){
			conn.createStatement().execute("ROLLBACK");
			throw new SQLException(x);
		}catch(NoSuchAlgorithmException ignore){}
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
			Connection conn=DatabaseConnectionManager.getConnection();
			PreparedStatement stmt=conn.prepareStatement("UPDATE `accounts` SET `password`=? WHERE `id`=? AND `password`=?");
			stmt.setBytes(1, hashedNew);
			stmt.setInt(2, accountID);
			stmt.setBytes(3, hashedOld);
			return stmt.executeUpdate()==1;
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
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("UPDATE `accounts` SET `preferences`=? WHERE `id`=?");
		stmt.setString(1, Utils.gson.toJson(prefs));
		stmt.setInt(2, accountID);
		stmt.execute();
	}

	public static Account getAccountByEmail(String email) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("accounts")
				.allColumns()
				.where("email=?", email)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			return res.first() ? Account.fromResultSet(res) : null;
		}
	}

	public static Account getAccountByUsername(String username) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.selectFrom("users")
				.columns("id")
				.where("username=? AND ap_id IS NULL", username)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				int uid=res.getInt(1);
				stmt=new SQLQueryBuilder(conn)
						.selectFrom("accounts")
						.allColumns()
						.where("user_id=?", uid)
						.createStatement();
				try(ResultSet res2=stmt.executeQuery()){
					res2.first();
					return Account.fromResultSet(res2);
				}
			}
		}
		return null;
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
				.createStatement()
				.execute();
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
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("email_codes")
				.allColumns()
				.where("code=?", (Object) _id)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			if(!res.first())
				return null;
			EmailCode code=new EmailCode();
			code.accountID=res.getInt("account_id");
			code.type=EmailCode.Type.values()[res.getInt("type")];
			String extra=res.getString("extra");
			if(extra!=null)
				code.extra=JsonParser.parseString(extra).getAsJsonObject();
			code.createdAt=res.getTimestamp("created_at");
			return code;
		}
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
				.createStatement()
				.execute();
	}

	public static void deleteExpiredEmailCodes() throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("email_codes")
				.where("created_at<?", new Timestamp(System.currentTimeMillis()-EmailCode.VALIDITY_MS))
				.createStatement()
				.execute();
	}

	public static boolean updatePassword(int accountID, String newPassword) throws SQLException{
		try{
			MessageDigest md=MessageDigest.getInstance("SHA-256");
			byte[] hashedNew=md.digest(newPassword.getBytes(StandardCharsets.UTF_8));
			Connection conn=DatabaseConnectionManager.getConnection();
			PreparedStatement stmt=conn.prepareStatement("UPDATE `accounts` SET `password`=? WHERE `id`=?");
			stmt.setBytes(1, hashedNew);
			stmt.setInt(2, accountID);
			return stmt.executeUpdate()==1;
		}catch(NoSuchAlgorithmException ignore){}
		return false;
	}

	public static void setLastActive(int accountID, String psid, Instant time) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		new SQLQueryBuilder(conn)
				.update("accounts")
				.value("last_active", time)
				.where("id=?", accountID)
				.createStatement()
				.execute();
		byte[] sid=Base64.getDecoder().decode(psid);
		new SQLQueryBuilder(conn)
				.update("sessions")
				.value("last_active", time)
				.where("id=?", sid)
				.createStatement()
				.execute();
	}

	public static synchronized void removeFromUserPermissionsCache(int userID){
		permissionsCache.remove(userID);
	}

	public static synchronized UserPermissions getUserPermissions(Account account) throws SQLException{
		UserPermissions r=permissionsCache.get(account.user.id);
		if(r!=null)
			return r;
		r=new UserPermissions(account);
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("group_admins")
				.columns("group_id", "level")
				.where("user_id=?", account.user.id)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			res.beforeFirst();
			while(res.next()){
				r.managedGroups.put(res.getInt(1), Group.AdminLevel.values()[res.getInt(2)]);
			}
		}
		r.canInviteNewUsers=switch(Config.signupMode){
			case OPEN, INVITE_ONLY -> true;
			case CLOSED, MANUAL_APPROVAL -> r.serverAccessLevel==Account.AccessLevel.ADMIN || r.serverAccessLevel==Account.AccessLevel.MODERATOR;
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
				.where("code=? AND signups_remaining>0", code)
				.executeAndGetSingleObject(SignupInvitation::fromResultSet);
	}

	public static void deleteInvitation(int id) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("signup_invitations")
				.where("id=?", id)
				.executeNoResult();
	}

	public static PaginatedList<User> getInvitedUsers(int selfAccountID, int offset, int count) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
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
				.createStatement()
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

	public enum SignupResult{
		SUCCESS,
		USERNAME_TAKEN,
		INVITE_INVALID
	}
}
