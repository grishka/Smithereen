package smithereen.controllers;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.Post;
import smithereen.data.UriBuilder;
import smithereen.data.User;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UnsupportedRemoteObjectTypeException;
import smithereen.storage.GroupStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;

import static smithereen.Utils.parseIntOrDefault;

public class ObjectLinkResolver{

	private static final Pattern POSTS=Pattern.compile("^/posts/(\\d+)$");
	private static final Pattern USERS=Pattern.compile("^/users/(\\d+)$");
	private static final Pattern GROUPS=Pattern.compile("^/groups/(\\d+)$");

	private static final Logger LOG=LoggerFactory.getLogger(ObjectLinkResolver.class);

	private final HashMap<ActorTokenKey, ActorToken> actorTokensCache=new HashMap<>();
	private final HashMap<ActorTokenKey, Object> actorTokenLocks=new HashMap<>();

	private final ApplicationContext context;

	public ObjectLinkResolver(ApplicationContext context){
		this.context=context;
	}

	private static Post getPost(String _id) throws SQLException{
		int id=parseIntOrDefault(_id, 0);
		if(id==0)
			throw new ObjectNotFoundException("Invalid local post ID");
		Post post=PostStorage.getPostByID(id, false);
		if(post==null)
			throw new ObjectNotFoundException("Post with ID "+id+" not found");
		return post;
	}

	private static User getUser(String _id) throws SQLException{
		int id=parseIntOrDefault(_id, 0);
		if(id==0)
			throw new ObjectNotFoundException();
		User user=UserStorage.getById(id);
		if(user==null || user instanceof ForeignUser)
			throw new ObjectNotFoundException();
		return user;
	}

	private static Group getGroup(String _id) throws SQLException{
		int id=parseIntOrDefault(_id, 0);
		if(id==0)
			throw new ObjectNotFoundException();
		Group group=GroupStorage.getById(id);
		if(group==null || group instanceof ForeignGroup)
			throw new ObjectNotFoundException();
		return group;
	}

	public ActivityPubObject resolve(URI link){
		return resolve(link, ActivityPubObject.class, false, true, false);
	}

	public JsonObject getActorToken(User user, Group group){
		if(!(group instanceof ForeignGroup fg)){
			return ActivityPub.generateActorToken(context, user, group);
		}else if(fg.actorTokenEndpoint==null){
			return null;
		}
		ActorTokenKey key=new ActorTokenKey(user.id, group.id);
		boolean needWait;
		Object lock;
		synchronized(actorTokensCache){
			ActorToken token=actorTokensCache.get(key);
			if(token!=null && token.isValid())
				return token.token;
			if(!actorTokenLocks.containsKey(key)){
				// This thread will request the token, other threads will wait
				lock=new Object();
				actorTokenLocks.put(key, lock);
				needWait=false;
			}else{
				// Other thread is already requesting the token, we'll wait for it to do that
				lock=actorTokenLocks.get(key);
				needWait=true;
			}
		}
		if(needWait){
			synchronized(lock){
				while(actorTokenLocks.containsKey(key)){
					try{
						lock.wait();
					}catch(InterruptedException ignore){}
				}
				synchronized(actorTokensCache){
					return actorTokensCache.get(key).token;
				}
			}
		}else{
			JsonObject token=ActivityPub.fetchActorToken(context, user, fg);
			synchronized(actorTokensCache){
				if(token!=null)
					actorTokensCache.put(key, new ActorToken(token, Utils.parseISODate(token.getAsJsonPrimitive("validUntil").getAsString())));
			}
			synchronized(lock){
				actorTokenLocks.remove(key);
				lock.notifyAll();
				return token;
			}
		}
	}

	@NotNull
	public <T extends ActivityPubObject> T resolve(URI _link, Class<T> expectedType, boolean allowFetching, boolean allowStorage, boolean forceRefetch){
		return resolve(_link, expectedType, allowFetching, allowStorage, forceRefetch, null, (JsonObject) null);
	}

	@NotNull
	public <T extends ActivityPubObject> T resolve(URI _link, Class<T> expectedType, boolean allowFetching, boolean allowStorage, boolean forceRefetch, Actor signer, Actor owner){
		JsonObject actorToken=null;
		if(!Config.isLocal(_link) && owner instanceof Group g && g.accessType!=Group.AccessType.OPEN && signer instanceof User u){
			actorToken=getActorToken(u, g);
		}
		return resolve(_link, expectedType, allowFetching, allowStorage, forceRefetch, signer, actorToken);
	}

	@NotNull
	public <T extends ActivityPubObject> T resolve(URI _link, Class<T> expectedType, boolean allowFetching, boolean allowStorage, boolean forceRefetch, Actor signer, JsonObject actorToken){
		try{
			LOG.debug("Resolving ActivityPub link: {}, expected type: {}", _link, expectedType.getName());
			URI link;
			if("bear".equals(_link.getScheme())){
				link=URI.create(UriBuilder.parseQueryString(_link.getRawQuery()).get("u"));
			}else{
				link=_link;
			}
			if(!Config.isLocal(link)){
				if(!forceRefetch){
					User user=UserStorage.getUserByActivityPubID(link);
					if(user!=null)
						return ensureTypeAndCast(user, expectedType);
					ForeignGroup group=GroupStorage.getForeignGroupByActivityPubID(link);
					if(group!=null)
						return ensureTypeAndCast(group, expectedType);
					Post post=PostStorage.getPostByID(link);
					if(post!=null)
						return ensureTypeAndCast(post, expectedType);
				}
				if(allowFetching){
					try{
						ActivityPubObject obj=ActivityPub.fetchRemoteObject(_link, signer, actorToken);
						if(obj!=null){
							T o=ensureTypeAndCast(obj, expectedType);
							o.resolveDependencies(context, allowFetching, allowStorage);
							if(allowStorage)
								storeOrUpdateRemoteObject(o);
							return o;
						}else{
							throw new UnsupportedRemoteObjectTypeException();
						}
					}catch(IOException x){
						throw new ObjectNotFoundException("Can't resolve remote object: "+link, x);
					}
				}
				throw new ObjectNotFoundException("Can't resolve remote object locally: "+link);
			}

			Matcher matcher=POSTS.matcher(link.getPath());
			if(matcher.find()){
				return ensureTypeAndCast(getPost(matcher.group(1)), expectedType);
			}

			matcher=USERS.matcher(link.getPath());
			if(matcher.find()){
				return ensureTypeAndCast(getUser(matcher.group(1)), expectedType);
			}

			matcher=GROUPS.matcher(link.getPath());
			if(matcher.find()){
				return ensureTypeAndCast(getGroup(matcher.group(1)), expectedType);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}

		throw new ObjectNotFoundException("Invalid local URI");
	}

	public void storeOrUpdateRemoteObject(ActivityPubObject o){
		try{
			o.storeDependencies(context);
			if(o instanceof ForeignUser fu)
				UserStorage.putOrUpdateForeignUser(fu);
			else if(o instanceof ForeignGroup fg)
				GroupStorage.putOrUpdateForeignGroup(fg);
			else if(o instanceof Post p)
				PostStorage.putForeignWallPost(p);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private static <T extends ActivityPubObject> T ensureTypeAndCast(ActivityPubObject obj, Class<T> type){
		if(type.isInstance(obj))
			return type.cast(obj);
		throw new IllegalStateException("Expected object of type "+type.getName()+", but got "+obj.getClass().getName()+" instead");
	}

	private record ActorTokenKey(int userID, int groupID){}

	private record ActorToken(JsonObject token, Instant validUntil){
		public boolean isValid(){
			return validUntil.isAfter(Instant.now());
		}
	}
}
