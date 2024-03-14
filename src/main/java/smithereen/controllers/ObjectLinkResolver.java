package smithereen.controllers;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.CollectionQueryResult;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.ServiceActor;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.MailMessage;
import smithereen.model.Post;
import smithereen.util.UriBuilder;
import smithereen.model.User;
import smithereen.exceptions.FederationException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.UserBanStatus;
import smithereen.storage.GroupStorage;
import smithereen.storage.MailStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;

import static smithereen.Utils.parseIntOrDefault;

public class ObjectLinkResolver{

	private static final Pattern POSTS=Pattern.compile("^/posts/(\\d+)$");
	private static final Pattern USERS=Pattern.compile("^/users/(\\d+)$");
	private static final Pattern GROUPS=Pattern.compile("^/groups/(\\d+)$");
	private static final Pattern MESSAGES=Pattern.compile("^/activitypub/objects/messages/([a-zA-Z0-9_-]+)$");

	private static final Logger LOG=LoggerFactory.getLogger(ObjectLinkResolver.class);

	private final HashMap<URI, ActorToken> actorTokensCache=new HashMap<>();
	private final HashMap<URI, Object> actorTokenLocks=new HashMap<>();
	private final LruCache<URI, ForeignUser> serviceActorCache=new LruCache<>(200);

	private final ApplicationContext context;

	public ObjectLinkResolver(ApplicationContext context){
		this.context=context;
	}

	private Post getPost(String _id) throws SQLException{
		int id=parseIntOrDefault(_id, 0);
		if(id==0)
			throw new ObjectNotFoundException("Invalid local post ID");
		Post post=PostStorage.getPostByID(id, false);
		if(post==null)
			throw new ObjectNotFoundException("Post with ID "+id+" not found");
		return post;
	}

	private User getUser(String _id){
		int id=parseIntOrDefault(_id, 0);
		if(id==0)
			throw new ObjectNotFoundException();
		return context.getUsersController().getLocalUserOrThrow(id);
	}

	private Group getGroup(String _id){
		int id=parseIntOrDefault(_id, 0);
		if(id==0)
			throw new ObjectNotFoundException();
		return context.getGroupsController().getLocalGroupOrThrow(id);
	}

	public ActivityPubObject resolve(URI link){
		return resolve(link, ActivityPubObject.class, false, true, false);
	}

	public JsonObject getActorToken(Actor actor, Group group){
		if(!(group instanceof ForeignGroup fg)){
			return ActivityPub.generateActorToken(context, actor, group);
		}else if(fg.actorTokenEndpoint==null){
			return null;
		}
		boolean needWait;
		Object lock;
		synchronized(actorTokensCache){
			ActorToken token=actorTokensCache.get(group.activityPubID);
			if(token!=null && token.isValid())
				return token.token;
			if(!actorTokenLocks.containsKey(group.activityPubID)){
				// This thread will request the token, other threads will wait
				lock=new Object();
				actorTokenLocks.put(group.activityPubID, lock);
				needWait=false;
			}else{
				// Other thread is already requesting the token, we'll wait for it to do that
				lock=actorTokenLocks.get(group.activityPubID);
				needWait=true;
			}
		}
		if(needWait){
			synchronized(lock){
				while(actorTokenLocks.containsKey(group.activityPubID)){
					try{
						lock.wait();
					}catch(InterruptedException ignore){}
				}
				synchronized(actorTokensCache){
					return actorTokensCache.get(group.activityPubID).token;
				}
			}
		}else{
			JsonObject token=ActivityPub.fetchActorToken(context, actor, fg);
			synchronized(actorTokensCache){
				if(token!=null)
					actorTokensCache.put(group.activityPubID, new ActorToken(token, Utils.parseISODate(token.getAsJsonPrimitive("validUntil").getAsString())));
			}
			synchronized(lock){
				actorTokenLocks.remove(group.activityPubID);
				lock.notifyAll();
				return token;
			}
		}
	}

	@NotNull
	public <T extends ActivityPubObject> T resolve(URI _link, Class<T> expectedType, boolean allowFetching, boolean allowStorage, boolean forceRefetch){
		return resolve(_link, expectedType, allowFetching, allowStorage, forceRefetch, (JsonObject) null, false);
	}

	@NotNull
	public <T extends ActivityPubObject> T resolve(URI _link, Class<T> expectedType, boolean allowFetching, boolean allowStorage, boolean forceRefetch, Actor owner, boolean bypassCollectionCheck){
		JsonObject actorToken=null;
		if(!Config.isLocal(_link) && owner instanceof Group g && g.accessType!=Group.AccessType.OPEN){
			actorToken=getActorToken(ServiceActor.getInstance(), g);
		}
		return resolve(_link, expectedType, allowFetching, allowStorage, forceRefetch, actorToken, bypassCollectionCheck);
	}

	@NotNull
	public <T> T resolveNative(URI _link, Class<T> expectedType, boolean allowFetching, boolean allowStorage, boolean forceRefetch, Actor owner, boolean bypassCollectionCheck){
		JsonObject actorToken=null;
		if(!Config.isLocal(_link) && owner instanceof Group g && g.accessType!=Group.AccessType.OPEN){
			actorToken=getActorToken(ServiceActor.getInstance(), g);
		}
		return resolveNative(_link, expectedType, allowFetching, allowStorage, forceRefetch, actorToken, bypassCollectionCheck);
	}

	@NotNull
	public <T> T resolveNative(URI _link, Class<T> expectedType, boolean allowFetching, boolean allowStorage, boolean forceRefetch, JsonObject actorToken, boolean bypassCollectionCheck){
		try{
			LOG.debug("Resolving ActivityPub link: {}, expected type: {}, allow storage {}, force refetch {}", _link, expectedType.getName(), allowStorage, forceRefetch);
			URI link;
			if("bear".equals(_link.getScheme())){
				link=URI.create(UriBuilder.parseQueryString(_link.getRawQuery()).get("u"));
			}else{
				link=_link;
			}
			if(!Config.isLocal(link)){
				if(!forceRefetch){
					if(expectedType.isAssignableFrom(ForeignUser.class)){
						User user=UserStorage.getUserByActivityPubID(link);
						if(user!=null)
							return ensureTypeAndCast(user, expectedType);
						user=serviceActorCache.get(link);
						if(user!=null)
							return ensureTypeAndCast(user, expectedType);
					}
					if(expectedType.isAssignableFrom(ForeignGroup.class)){
						ForeignGroup group=GroupStorage.getForeignGroupByActivityPubID(link);
						if(group!=null)
							return ensureTypeAndCast(group, expectedType);
					}
					if(expectedType.isAssignableFrom(Post.class)){
						Post post=PostStorage.getPostByID(link);
						if(post!=null)
							return ensureTypeAndCast(post, expectedType);
					}
					if(expectedType.isAssignableFrom(MailMessage.class)){
						List<MailMessage> msgs=MailStorage.getMessages(link);
						if(!msgs.isEmpty())
							return ensureTypeAndCast(msgs.get(0), expectedType);
					}
				}
				if(allowFetching){
					try{
						ActivityPubObject obj=ActivityPub.fetchRemoteObject(_link, null, actorToken, context);
						if(obj instanceof ForeignGroup fg){
							fg.resolveDependencies(context, allowFetching, allowStorage);
						}
						if(obj instanceof ForeignUser fu){
							if(allowStorage && fu.movedToURL!=null){
								handleNewlyFetchedMovedUser(fu);
							}
						}
						if(obj instanceof NoteOrQuestion && !allowStorage && expectedType.isAssignableFrom(NoteOrQuestion.class)){
							return ensureTypeAndCast(obj, expectedType);
						}
						T o=convertToNativeObject(obj, expectedType);
						if(!bypassCollectionCheck && o instanceof Post post && obj.inReplyTo==null){ // TODO make this a generalized interface OwnedObject or something
							if(post.ownerID!=post.authorID){
								Actor owner=context.getWallController().getContentAuthorAndOwner(post).owner();
								ensureObjectIsInCollection(owner, owner.getWallURL(), post.getActivityPubID());
							}
						}
						if(o instanceof Post post){
							User author=context.getUsersController().getUserOrThrow(post.authorID);
							if(author.banStatus==UserBanStatus.SUSPENDED)
								throw new ObjectNotFoundException("Post author is suspended on this server");
						}
						if(allowStorage)
							storeOrUpdateRemoteObject(o);
						return o;
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

			matcher=MESSAGES.matcher(link.getPath());
			if(matcher.find()){
				long id=Utils.decodeLong(matcher.group(1));
				List<MailMessage> msgs=MailStorage.getMessages(Set.of(id));
				if(!msgs.isEmpty())
					return ensureTypeAndCast(msgs.get(0), expectedType);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}

		throw new ObjectNotFoundException("Invalid local URI");
	}

	@NotNull
	public <T extends ActivityPubObject> T resolve(URI _link, Class<T> expectedType, boolean allowFetching, boolean allowStorage, boolean forceRefetch, JsonObject actorToken, boolean bypassCollectionCheck){
		Class<?> nativeType;
		if(expectedType.isAssignableFrom(ActivityPubObject.class)){
			nativeType=Object.class;
		}else if(NoteOrQuestion.class.isAssignableFrom(expectedType)){
			nativeType=Post.class;
		}else{
			nativeType=expectedType;
		}
		return convertToActivityPubObject(resolveNative(_link, nativeType, allowFetching, allowStorage, forceRefetch, actorToken, bypassCollectionCheck), expectedType);
	}

	public void storeOrUpdateRemoteObject(Object o){
		try{
			if(o instanceof ForeignUser fu){
				if(fu.isServiceActor){
					serviceActorCache.put(fu.activityPubID, fu);
				}else{
					UserStorage.putOrUpdateForeignUser(fu);
				}
			}else if(o instanceof ForeignGroup fg){
				fg.storeDependencies(context);
				GroupStorage.putOrUpdateForeignGroup(fg);
			}else if(o instanceof Post p){
				PostStorage.putForeignWallPost(p);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private static <T> T ensureTypeAndCast(Object obj, Class<T> type){
		if(type.isInstance(obj))
			return type.cast(obj);
		throw new IllegalStateException("Expected object of type "+type.getName()+", but got "+obj.getClass().getName()+" instead");
	}

	public <T extends ActivityPubObject> T convertToActivityPubObject(Object o, Class<T> type){
		if(o instanceof ActivityPubObject apo)
			return ensureTypeAndCast(apo, type);
		if(type.isAssignableFrom(NoteOrQuestion.class)){
			if(o instanceof Post post)
				return type.cast(NoteOrQuestion.fromNativePost(post, context));
			if(o instanceof MailMessage message)
				return type.cast(NoteOrQuestion.fromNativeMessage(message, context));
		}
		throw new IllegalStateException("Native type "+o.getClass().getName()+" does not have an ActivityPub representation");
	}

	public <T> T convertToNativeObject(ActivityPubObject o, Class<T> type){
		if(o instanceof NoteOrQuestion noq && type.isAssignableFrom(Post.class)){
			return type.cast(noq.asNativePost(context));
		}else if(type.isAssignableFrom(o.getClass())){
			return type.cast(o);
		}
		throw new IllegalStateException("Can't convert ActivityPub "+o.getClass().getName()+" to a native object of type "+type.getName());
	}

	public void ensureObjectIsInCollection(@NotNull Actor collectionOwner, @NotNull URI collectionID, @NotNull URI objectID){
		LOG.debug("Checking whether object {} belongs to collection {} owned by {}", objectID, collectionID, collectionOwner.activityPubID);
		if(Config.isLocal(collectionID))
			throw new FederationException(collectionID+" is a local collection. Must submit this object with a Create activity first.");
		if(collectionOwner.collectionQueryEndpoint==null)
			return; // There's nothing we can do anyway
		if(collectionID.getHost().equals(objectID.getHost()))
			return; // This collection is on the same server as the object. We trust that that server is sane.
		CollectionQueryResult cqr=ActivityPub.performCollectionQuery(collectionOwner, collectionID, List.of(objectID));
		List<LinkOrObject> res=cqr.items;
		if(res.isEmpty() || !objectID.equals(res.get(0).link))
			throw new FederationException("Object "+objectID+" is not in collection "+collectionID);
		LOG.debug("Object {} was confirmed to be contained in {}", objectID, collectionID);
	}

	public UsernameResolutionResult resolveUsernameLocally(String username){
		return resolveUsername(username, false, EnumSet.allOf(UsernameOwnerType.class));
	}

	public UsernameResolutionResult resolveUsername(String username, boolean allowFetching, EnumSet<UsernameOwnerType> allowedTypes){
		if(allowedTypes.isEmpty())
			throw new IllegalArgumentException("allowedTypes can't be empty");

		if(allowFetching)
			throw new UnsupportedOperationException(); // TODO

		if(allowedTypes.contains(UsernameOwnerType.USER)){
			int user=context.getUsersController().tryGetUserIdByUsername(username);
			if(user>0)
				return new UsernameResolutionResult(UsernameOwnerType.USER, user);
		}

		if(allowedTypes.contains(UsernameOwnerType.GROUP)){
			int group=context.getGroupsController().tryGetGroupIdForUsername(username);
			if(group>0)
				return new UsernameResolutionResult(UsernameOwnerType.GROUP, group);
		}

		throw new ObjectNotFoundException();
	}

	public static int getUserIDFromLocalURL(URI url){
		if(!Config.isLocal(url))
			return 0;
		Matcher matcher=USERS.matcher(url.getPath());
		if(!matcher.find())
			return 0;
		return Integer.parseInt(matcher.group(1));
	}

	private void handleNewlyFetchedMovedUser(ForeignUser user){
		try{
			User newUser=resolve(user.movedToURL, User.class, true, true, false);
			if(newUser.alsoKnownAs.contains(user.activityPubID) && user.movedTo!=newUser.id){
				user.movedTo=newUser.id;
			}
		}catch(ObjectNotFoundException x){
			LOG.warn("User {} moved to {} but the new URL can't be fetched", user.activityPubID, user.movedToURL, x);
		}
	}

	private record ActorToken(JsonObject token, Instant validUntil){
		public boolean isValid(){
			return validUntil.isAfter(Instant.now());
		}
	}

	public enum UsernameOwnerType{
		USER,
		GROUP
	}

	public record UsernameResolutionResult(UsernameOwnerType type, int localID){}
}
