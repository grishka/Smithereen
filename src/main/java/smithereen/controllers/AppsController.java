package smithereen.controllers;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.objects.LocalImage;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.Account;
import smithereen.model.User;
import smithereen.model.apps.AppAuthCode;
import smithereen.model.apps.AppAccessGrant;
import smithereen.model.apps.AppAccessToken;
import smithereen.model.apps.ClientApp;
import smithereen.model.apps.ClientAppPermission;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.storage.AppsStorage;
import smithereen.storage.MediaStorage;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.SessionStorage;
import smithereen.util.ByteArrayMapKey;
import smithereen.util.JsonObjectBuilder;
import spark.Request;
import spark.utils.StringUtils;

public class AppsController{
	private static final Logger LOG=LoggerFactory.getLogger(AppsController.class);

	private final ApplicationContext context;
	private final SecureRandom srand=new SecureRandom();
	private final LruCache<ByteArrayMapKey, AppAccessToken> accessTokenCache=new LruCache<>(5000);
	private final LruCache<Long, ClientApp> appsCache=new LruCache<>(5000);
	private HashMap<ByteArrayMapKey, AccessTokenLastUseUpdate> accessTokenPendingAccessUpdates=new HashMap<>();
	private final Object accessTokenAccessUpdateLock=new Object();
	private final ScheduledExecutorService asyncUpdater;

	public AppsController(ApplicationContext context){
		this.context=context;
		asyncUpdater=Executors.newSingleThreadScheduledExecutor();
		asyncUpdater.submit(()->Thread.currentThread().setName("ApiTokenAccessUpdater"));
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
			appsCache.put(app.id, app);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public ClientApp getAppByID(long id){
		ClientApp app=appsCache.get(id);
		if(app!=null)
			return app;
		try{
			app=AppsStorage.getAppByID(id);
			if(app==null)
				throw new ObjectNotFoundException();
			appsCache.put(app.id, app);
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

	public Map<Long, ClientApp> getAppsByIDs(Collection<Long> ids){
		if(ids.isEmpty())
			return Map.of();
		try{
			HashMap<Long, ClientApp> res=new HashMap<>();
			HashSet<Long> remainingIDs=new HashSet<>();
			for(long id:ids){
				ClientApp app=appsCache.get(id);
				if(app!=null)
					res.put(app.id, app);
				else
					remainingIDs.add(id);
			}

			if(!remainingIDs.isEmpty()){
				Map<Long, ClientApp> moreApps=AppsStorage.getAppsByIDs(remainingIDs);
				moreApps.forEach(appsCache::put);
				res.putAll(moreApps);
			}

			return res;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public long createApp(User self, String name, String description, String logoID){
		try{
			LocalImage logo=null;
			if(StringUtils.isNotEmpty(logoID)){
				logo=MediaStorageUtils.getLocalImage(logoID);
			}
			long id=AppsStorage.createApp(self.id, name, description, logo);
			if(logo!=null){
				MediaStorage.createMediaFileReference(logo.fileID, id, MediaFileReferenceType.APP_LOGO, self.id);
			}
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateApp(ClientApp app, String name, String description, String logoID){
		try{
			LocalImage logo=app.logo instanceof LocalImage li ? li : null;
			String existingLogoID=logo!=null ? logo.getLocalID() : null;
			if(existingLogoID!=null && !existingLogoID.equals(logoID)){
				MediaStorage.deleteMediaFileReference(app.id, MediaFileReferenceType.APP_LOGO, ((LocalImage)app.logo).fileID);
			}
			if(StringUtils.isNotEmpty(logoID)){
				logo=MediaStorageUtils.getLocalImage(logoID);
			}
			AppsStorage.updateApp(app.id, name, description, logo);
			if(!Objects.equals(logoID, existingLogoID) && logo!=null)
				MediaStorage.createMediaFileReference(logo.fileID, app.id, MediaFileReferenceType.APP_LOGO, app.developerID);
			appsCache.remove(app.id);
			// TODO Update{Application}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<Long> getUserManagedApps(User self){
		try{
			return AppsStorage.getUserManagedApps(self.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public long tryGetAppIdForUsername(@NotNull String username){
		try{
			return AppsStorage.getAppIdByUsername(username);
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

	public List<AppAccessGrant> getAllAccessGrants(Account account){
		try{
			return AppsStorage.getAllAccessGrants(account.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteAccessGrantAndRevokeTokens(Account account, ClientApp app){
		try{
			AppAccessGrant grant=AppsStorage.getAccessGrant(account.id, app.id);
			if(grant==null)
				return;
			AppsStorage.deleteAccessGrant(account.id, app.id);
			// TODO send Remove{Application}
			List<byte[]> tokens=AppsStorage.getUserAppAccessTokens(account.id, app.id);
			if(tokens.isEmpty())
				return;
			AppsStorage.deleteAccessTokens(tokens);
			for(byte[] token:tokens){
				accessTokenCache.remove(new ByteArrayMapKey(token));
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	// endregion
	// region Access tokens

	public AppAccessToken createAccessToken(Account account, ClientApp app, EnumSet<ClientAppPermission> permissions, Request req){
		try{
			AppsStorage.createOrUpdateAccessGrant(account.id, app.id, permissions);
			// TODO send Add{Application}
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

	public void updateAccessTokenLastAccess(AppAccessToken token, InetAddress ip, String userAgent){
		ByteArrayMapKey key=new ByteArrayMapKey(token.id());
		synchronized(accessTokenAccessUpdateLock){
			if(accessTokenPendingAccessUpdates.isEmpty()){
				asyncUpdater.schedule(this::doPendingTokenAccessUpdates, 10, TimeUnit.SECONDS);
			}
			accessTokenPendingAccessUpdates.put(key, new AccessTokenLastUseUpdate(ip, userAgent));
		}
	}

	public void revokeUserAccessTokensExcept(Account account, byte[] token){
		try{
			List<byte[]> tokensToRevoke=AppsStorage.getAllUserAccessTokens(account.id)
					.stream()
					.filter(t->!Arrays.equals(t, token))
					.toList();
			if(tokensToRevoke.isEmpty())
				return;
			AppsStorage.deleteAccessTokens(tokensToRevoke);
			for(byte[] t:tokensToRevoke){
				accessTokenCache.remove(new ByteArrayMapKey(t));
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void revokeAccessToken(byte[] token){
		try{
			AppsStorage.deleteAccessToken(token);
			accessTokenCache.remove(new ByteArrayMapKey(token));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private void doPendingTokenAccessUpdates(){
		HashMap<ByteArrayMapKey, AccessTokenLastUseUpdate> updates=accessTokenPendingAccessUpdates;
		synchronized(accessTokenAccessUpdateLock){
			accessTokenPendingAccessUpdates=new HashMap<>();
		}
		try{
			Map<String, Long> userAgents=updates.values().stream()
					.map(AccessTokenLastUseUpdate::userAgent)
					.distinct()
					.collect(Collectors.toMap(Function.identity(), Utils::hashUserAgent));
			SessionStorage.putUserAgents(userAgents);
			for(Map.Entry<ByteArrayMapKey, AccessTokenLastUseUpdate> e:updates.entrySet()){
				AppsStorage.setAccessTokenLastUse(e.getKey().key(), e.getValue().ip, userAgents.get(e.getValue().userAgent));
			}
		}catch(SQLException x){
			LOG.error("Failed to perform last token access updates", x);
		}
	}

	private record AccessTokenLastUseUpdate(InetAddress ip, String userAgent){}

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
