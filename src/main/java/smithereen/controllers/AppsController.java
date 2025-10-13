package smithereen.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;

import smithereen.ApplicationContext;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.Account;
import smithereen.model.apps.AppAuthCode;
import smithereen.model.apps.AppAccessGrant;
import smithereen.model.apps.AppAccessToken;
import smithereen.model.apps.ClientApp;
import smithereen.model.apps.ClientAppPermission;
import smithereen.storage.AppsStorage;
import smithereen.util.ByteArrayMapKey;
import smithereen.util.JsonObjectBuilder;
import spark.Request;
import spark.utils.StringUtils;

public class AppsController{
	private static final Logger LOG=LoggerFactory.getLogger(AppsController.class);

	private final ApplicationContext context;
	private final SecureRandom srand=new SecureRandom();
	private final LruCache<ByteArrayMapKey, AppAccessToken> accessTokenCache=new LruCache<>(5000);

	public AppsController(ApplicationContext context){
		this.context=context;
	}

	public void deleteExpiredCodesAndTokens(){
		try{
			List<byte[]> expiredTokenIDs=AppsStorage.getExpiredAccessTokens();
			if(!expiredTokenIDs.isEmpty()){
				AppsStorage.deleteAccessTokens(expiredTokenIDs);
			}
			AppsStorage.deleteExpiredCodes();
		}catch(SQLException x){
			LOG.error("Failed to delete expired auth codes and tokens", x);
		}
	}

	// region Apps

	public void putOrUpdateForeignApp(ClientApp app){
		if(app.apID==null)
			throw new IllegalArgumentException("App must have an AP ID");
		try{
			AppsStorage.putOrUpdateForeignApp(app);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public ClientApp getAppByID(long id){
		try{
			ClientApp app=AppsStorage.getAppByID(id);
			if(app==null)
				throw new ObjectNotFoundException();
			return app;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public long getAppIdByActivityPubID(URI apID){
		try{
			return AppsStorage.getAppIdByActivityPubID(apID);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
	// region Access grants

	public AppAccessGrant getAccessGrantOrNull(Account account, ClientApp app){
		try{
			return AppsStorage.getAccessGrant(account.id, app.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
	// region Access tokens

	public AppAccessToken createAccessToken(Account account, ClientApp app, EnumSet<ClientAppPermission> permissions, Request req){
		try{
			AppsStorage.createOrUpdateAccessGrant(account.id, app.id, permissions);
			Instant expiresAt=permissions.contains(ClientAppPermission.OFFLINE) || permissions.contains(ClientAppPermission.PASSWORD_GRANT_USED) ? null : Instant.now().plus(1, ChronoUnit.HOURS);
			byte[] id=new byte[64];
			srand.nextBytes(id);
			InetAddress ip=Utils.getRequestIP(req);
			AppsStorage.createAccessToken(id, account.id, app.id, ip, expiresAt, permissions);
			AppAccessToken token=new AppAccessToken(id, account.id, app.id, expiresAt, permissions);
			accessTokenCache.put(new ByteArrayMapKey(id), token);
			return token;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public AppAccessToken getAccessTokenOrNull(byte[] id){
		ByteArrayMapKey key=new ByteArrayMapKey(id);
		AppAccessToken token=accessTokenCache.get(key);
		if(token!=null)
			return token;
		try{
			token=AppsStorage.getAccessToken(id);
			if(token!=null)
				accessTokenCache.put(key, token);
			return token;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
	// region OAuth auth codes

	public byte[] createAuthCode(Account account, ClientApp app, EnumSet<ClientAppPermission> permissions, String s256CodeChallenge, String redirectURI){
		try{
			byte[] id=new byte[64];
			srand.nextBytes(id);
			JsonObjectBuilder jb=new JsonObjectBuilder()
					.add("redirectURI", redirectURI);
			if(StringUtils.isNotEmpty(s256CodeChallenge))
				jb.add("codeChallenge", s256CodeChallenge);
			AppsStorage.createAuthCode(id, account.id, app.id, permissions, Instant.now().plus(10, ChronoUnit.MINUTES), jb.build().toString());
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public AppAuthCode getAndDeleteAuthCode(byte[] id){
		try{
			return AppsStorage.getAndDeleteAuthCode(id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
}
