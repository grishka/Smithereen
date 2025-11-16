package smithereen.storage;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.model.apps.AppAuthCode;
import smithereen.model.apps.AppAccessGrant;
import smithereen.model.apps.AppAccessToken;
import smithereen.model.apps.ClientApp;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.media.MediaFileRecord;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.util.NamedMutexCollection;

public class AppsStorage{
	private static final Logger LOG=LoggerFactory.getLogger(AppsStorage.class);
	private static NamedMutexCollection foreignAppUpdateLocks=new NamedMutexCollection();

	// region Apps

	public static void putOrUpdateForeignApp(ClientApp app) throws SQLException{
		String key=app.apID.toString().toLowerCase();
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			foreignAppUpdateLocks.acquire(key);
			final long existingAppID=new SQLQueryBuilder(conn)
					.selectFrom("api_applications")
					.columns("id")
					.where("ap_id=?", app.apID.toString())
					.executeAndGetLong();

			SQLQueryBuilder builder=new SQLQueryBuilder(conn);
			if(existingAppID==-1){
				builder.insertIgnoreInto("api_applications");
			}else{
				builder.update("api_applications").where("id=?", existingAppID);
			}

			builder.value("ap_id", app.apID.toString())
					.value("username", app.username)
					.value("domain", app.domain)
					.value("name", app.name)
					.value("description", app.description)
					.value("public_key", app.publicKey==null ? null : app.publicKey.getEncoded())
					.value("type", app.type)
					.value("logo", app.logo==null ? null : app.logo.asActivityPubObject(new JsonObject(), new SerializerContext(null, (String)null)).toString())
					.value("developer_id", app.developerID==0 ? null : app.developerID)
					.value("ap_inbox", app.apInbox==null ? null : app.apInbox.toString())
					.value("ap_shared_inbox", app.apSharedInbox==null ? null : app.apSharedInbox.toString())
					.value("extra", app.serializeExtraFields())
					.valueExpr("last_updated", "CURRENT_TIMESTAMP()");

			if(existingAppID==-1){
				app.id=builder.executeAndGetIDLong();
			}else{
				app.id=existingAppID;
				builder.executeNoResult();
			}
			app.lastUpdated=Instant.now();
		}catch(SQLIntegrityConstraintViolationException x){
			// Username conflict
			try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
				long oldID=new SQLQueryBuilder(conn)
						.selectFrom("api_applications")
						.where("username=? AND domain=? AND ap_id<>?", app.username, app.domain, app.apID.toString())
						.executeAndGetLong();
				if(oldID==-1){
					LOG.warn("Didn't find an existing app with username {}@{} while trying to deduplicate {}", app.username, app.domain, app.apID);
					throw x;
				}
				new SQLQueryBuilder(conn)
						.update("api_applications")
						.value("username", null)
						.where("id=?", oldID)
						.executeNoResult();
				putOrUpdateForeignApp(app);
			}
		}finally{
			foreignAppUpdateLocks.release(key);
		}
	}

	public static ClientApp getAppByID(long id) throws SQLException{
		ClientApp app=new SQLQueryBuilder()
				.selectFrom("api_applications")
				.where("id=?", id)
				.executeAndGetSingleObject(ClientApp::fromResultSet);
		if(app!=null)
			postprocessApps(List.of(app));
		return app;
	}

	public static Map<Long, ClientApp> getAppsByIDs(Collection<Long> ids) throws SQLException{
		Map<Long, ClientApp> apps=new SQLQueryBuilder()
				.selectFrom("api_applications")
				.whereIn("id", ids)
				.executeAsStream(ClientApp::fromResultSet)
				.collect(Collectors.toMap(a->a.id, Function.identity()));
		postprocessApps(apps.values());
		return apps;
	}

	public static long getAppIdByActivityPubID(URI apID) throws SQLException{
		long id=new SQLQueryBuilder()
				.selectFrom("api_applications")
				.where("ap_id=?", apID.toString())
				.executeAndGetLong();
		return id==-1 ? 0 : id;
	}

	public static long createApp(int userID, String name, String description, LocalImage logo) throws SQLException{
		byte[] publicKey, privateKey;
		try{
			KeyPairGenerator kpg=KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair pair=kpg.generateKeyPair();
			publicKey=pair.getPublic().getEncoded();
			privateKey=pair.getPrivate().getEncoded();
		}catch(NoSuchAlgorithmException x){
			throw new RuntimeException(x);
		}
		JsonObject serializedLogo=logo==null ? null : MediaStorageUtils.serializeAttachment(logo);
		return new SQLQueryBuilder()
				.insertInto("api_applications")
				.value("name", name)
				.value("description", description)
				.value("logo", serializedLogo==null ? null : serializedLogo.toString())
				.value("developer_id", userID)
				.value("public_key", publicKey)
				.value("private_key", privateKey)
				.executeAndGetIDLong();
	}

	public static void updateApp(long id, String name, String description, LocalImage logo) throws SQLException{
		JsonObject serializedLogo=logo==null ? null : MediaStorageUtils.serializeAttachment(logo);
		new SQLQueryBuilder()
				.update("api_applications")
				.where("id=?", id)
				.value("name", name)
				.value("description", description)
				.value("logo", serializedLogo==null ? null : serializedLogo.toString())
				.executeNoResult();
	}

	public static List<Long> getUserManagedApps(int userID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("api_applications")
				.columns("id")
				.where("developer_id=?", userID)
				.executeAndGetLongStream()
				.boxed()
				.toList();
	}

	private static void postprocessApps(Collection<ClientApp> apps) throws SQLException{
		Set<Long> needFiles=apps.stream()
				.map(a->a.logo instanceof LocalImage li ? li : null)
				.filter(Objects::nonNull)
				.map(li->li.fileID)
				.collect(Collectors.toSet());
		if(needFiles.isEmpty())
			return;
		Map<Long, MediaFileRecord> fileRecords=MediaStorage.getMediaFileRecords(needFiles);
		for(ClientApp a:apps){
			if(a.logo instanceof LocalImage li){
				MediaFileRecord mfr=fileRecords.get(li.fileID);
				if(mfr!=null)
					li.fillIn(mfr);
			}
		}
	}

	public static long getAppIdByUsername(@NotNull String username) throws SQLException{
		String domain;
		if(username.contains("@")){
			String[] parts=username.split("@", 2);
			username=parts[0];
			domain=parts[1];
		}else{
			domain="";
		}
		if(username.length()>Actor.USERNAME_MAX_LENGTH)
			username=username.substring(0, Actor.USERNAME_MAX_LENGTH);
		return new SQLQueryBuilder()
				.selectFrom("api_applications")
				.columns("id")
				.where("username=? AND domain=?", username, domain)
				.executeAndGetLong();
	}

	// endregion
	// region Access grants

	public static AppAccessGrant getAccessGrant(int accountID, long appID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("api_grants")
				.where("account_id=? AND app_id=?", accountID, appID)
				.executeAndGetSingleObject(AppAccessGrant::fromResultSet);
	}

	public static void createOrUpdateAccessGrant(int accountID, long appID, EnumSet<ClientAppPermission> permissions) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("api_grants")
				.value("account_id", accountID)
				.value("app_id", appID)
				.value("permissions", Utils.serializeEnumSet(permissions))
				.onDuplicateKeyUpdate()
				.executeNoResult();
	}

	public static void deleteAccessGrant(int accountID, long appID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("api_grants")
				.where("account_id=? AND app_id=?", accountID, appID)
				.executeNoResult();
	}

	public static List<AppAccessGrant> getAllAccessGrants(int accountID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("api_grants")
				.where("account_id=?", accountID)
				.orderBy("granted_at DESC")
				.executeAsStream(AppAccessGrant::fromResultSet)
				.toList();
	}

	// endregion
	// region Access tokens

	public static void createAccessToken(byte[] id, int accountID, long appID, InetAddress ip, Instant expiresAt, EnumSet<ClientAppPermission> permissions) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("api_tokens")
				.value("id", id)
				.value("account_id", accountID)
				.value("app_id", appID)
				.value("ip", Utils.serializeInetAddress(ip))
				.value("user_agent", 0)
				.value("expires_at", expiresAt)
				.value("permissions", Utils.serializeEnumSet(permissions))
				.executeNoResult();
	}

	public static AppAccessToken getAccessToken(byte[] id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("api_tokens")
				.where("id=?", (Object)id)
				.executeAndGetSingleObject(AppAccessToken::fromResultSet);
	}

	public static void deleteAccessToken(byte[] id) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("api_tokens")
				.where("id=?", (Object)id)
				.executeNoResult();
	}

	public static void deleteAccessTokens(List<byte[]> ids) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("api_tokens")
				.whereIn("id", ids)
				.executeNoResult();
	}

	public static List<byte[]> getExpiredAccessTokens() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("api_tokens")
				.columns("id")
				.where("expires_at IS NOT NULL AND expires_at<CURRENT_TIMESTAMP()")
				.executeAsStream(r->r.getBytes(1))
				.toList();
	}

	public static void setAccessTokenLastUse(byte[] id, InetAddress ip, long uaHash) throws SQLException{
		new SQLQueryBuilder()
				.update("api_tokens")
				.valueExpr("last_active", "CURRENT_TIMESTAMP()")
				.value("ip", Utils.serializeInetAddress(ip))
				.value("user_agent", uaHash)
				.where("id=?", (Object)id)
				.executeNoResult();
	}

	public static List<byte[]> getUserAppAccessTokens(int accountID, long appID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("api_tokens")
				.columns("id")
				.where("account_id=? AND app_id=?", accountID, appID)
				.executeAsStream(r->r.getBytes(1))
				.toList();
	}

	public static List<byte[]> getAllUserAccessTokens(int accountID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("api_tokens")
				.columns("id")
				.where("account_id=?", accountID)
				.executeAsStream(r->r.getBytes(1))
				.toList();
	}

	// endregion
	// region OAuth auth codes

	public static void createAuthCode(byte[] id, int accountID, long appID, EnumSet<ClientAppPermission> permissions, Instant expiresAt, String extra) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("api_codes")
				.value("id", id)
				.value("account_id", accountID)
				.value("app_id", appID)
				.value("permissions", Utils.serializeEnumSet(permissions))
				.value("expires_at", expiresAt)
				.value("extra", extra)
				.executeNoResult();
	}

	public static AppAuthCode getAndDeleteAuthCode(byte[] id) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			AppAuthCode code=new SQLQueryBuilder(conn)
					.selectFrom("api_codes")
					.where("id=?", (Object)id)
					.executeAndGetSingleObject(AppAuthCode::fromResultSet);
			if(code==null)
				return null;
			new SQLQueryBuilder(conn)
					.deleteFrom("api_codes")
					.where("id=?", (Object)id)
					.executeNoResult();
			return code;
		}
	}

	public static void deleteExpiredCodes() throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("api_codes")
				.where("expires_at<CURRENT_TIMESTAMP()")
				.executeNoResult();
	}

	// endregion
}
