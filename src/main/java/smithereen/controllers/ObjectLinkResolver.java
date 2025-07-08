package smithereen.controllers;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.ActivityPubBoardTopic;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
import smithereen.activitypub.objects.ActivityPubTaggedPerson;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.CollectionQueryResult;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.ServiceActor;
import smithereen.activitypub.tasks.FetchCollectionTotalTask;
import smithereen.exceptions.FederationException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UnsupportedRemoteObjectTypeException;
import smithereen.model.ActorStatus;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.GroupAdmin;
import smithereen.model.MailMessage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.Post;
import smithereen.model.Server;
import smithereen.model.User;
import smithereen.model.UserBanStatus;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.storage.CommentStorage;
import smithereen.storage.FederationStorage;
import smithereen.storage.GroupStorage;
import smithereen.storage.MailStorage;
import smithereen.storage.PhotoStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import smithereen.text.TextProcessor;
import smithereen.util.NamedMutexCollection;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;

import static smithereen.Utils.*;

public class ObjectLinkResolver{

	private static final Pattern POSTS=Pattern.compile("^/posts/(\\d+)$");
	private static final Pattern USERS=Pattern.compile("^/users/(\\d+)$");
	private static final Pattern GROUPS=Pattern.compile("^/groups/(\\d+)$");
	private static final Pattern MESSAGES=Pattern.compile("^/activitypub/objects/messages/([a-zA-Z0-9_-]+)$");
	private static final Pattern ALBUMS=Pattern.compile("^/albums/([a-zA-Z0-9_-]+)$");
	private static final Pattern PHOTOS=Pattern.compile("^/photos/([a-zA-Z0-9_-]+)$");
	private static final Pattern COMMENTS=Pattern.compile("^/comments/([a-zA-Z0-9_-]+)$");
	private static final Pattern TOPICS=Pattern.compile("^/topics/([a-zA-Z0-9_-]+)$");

	private static final Logger LOG=LoggerFactory.getLogger(ObjectLinkResolver.class);

	private final HashMap<URI, ActorToken> actorTokensCache=new HashMap<>();
	private final NamedMutexCollection actorTokenMutexes=new NamedMutexCollection();
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
		String mutexName=group.activityPubID.toString();
		actorTokenMutexes.acquire(mutexName);
		try{
			return actorTokensCache.computeIfAbsent(group.activityPubID, id->{
				JsonObject token=ActivityPub.fetchActorToken(context, actor, fg);
				if(token==null)
					throw new FederationException();
				return new ActorToken(token, Utils.parseISODate(token.getAsJsonPrimitive("validUntil").getAsString()));
			}).token();
		}catch(FederationException x){
			return null;
		}finally{
			actorTokenMutexes.release(mutexName);
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
		LOG.debug("Resolving ActivityPub link: {}, expected type: {}, allow storage {}, force refetch {}", _link, expectedType.getName(), allowStorage, forceRefetch);
		URI link;
		if("bear".equals(_link.getScheme())){
			link=URI.create(UriBuilder.parseQueryString(_link.getRawQuery()).get("u"));
		}else{
			link=_link;
		}
		if(!forceRefetch || Config.isLocal(link)){
			if(allowFetching){
				try{
					return resolveLocally(link, expectedType);
				}catch(ObjectNotFoundException ignore){}
			}else{
				return resolveLocally(link, expectedType);
			}
		}
		if(!Config.isLocal(link)){
			if(allowFetching){
				try{
					ActivityPubObject obj=ActivityPub.fetchRemoteObject(_link, null, actorToken, context);
					if(obj instanceof NoteOrQuestion noq && !allowStorage && expectedType.isAssignableFrom(NoteOrQuestion.class)){
						User author=resolve(noq.attributedTo, User.class, allowFetching, true, false);
						if(author.banStatus==UserBanStatus.SUSPENDED)
							throw new ObjectNotFoundException("Post author is suspended on this server");
						return ensureTypeAndCast(obj, expectedType);
					}
					T o=convertToNativeObject(obj, expectedType);
					if(!bypassCollectionCheck){ // TODO make this a generalized interface OwnedObject or something
						if(o instanceof Post post && obj.inReplyTo==null && post.ownerID!=post.authorID){
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
						storeOrUpdateRemoteObject(o, obj);
					return o;
				}catch(IOException x){
					throw new ObjectNotFoundException("Can't resolve remote object: "+link, x);
				}
			}
			throw new ObjectNotFoundException("Can't resolve remote object locally: "+link);
		}

		throw new ObjectNotFoundException("Invalid local URI");
	}

	@NotNull
	public <T extends ActivityPubObject> T resolve(URI _link, Class<T> expectedType, boolean allowFetching, boolean allowStorage, boolean forceRefetch, JsonObject actorToken, boolean bypassCollectionCheck){
		Class<?> nativeType;
		if(expectedType.isAssignableFrom(ActivityPubObject.class) && (allowStorage || Config.isLocal(_link))){
			nativeType=Object.class;
		}else if(NoteOrQuestion.class.isAssignableFrom(expectedType) && (allowStorage || Config.isLocal(_link))){
			nativeType=Post.class;
		}else{
			nativeType=expectedType;
		}
		return convertToActivityPubObject(resolveNative(_link, nativeType, allowFetching, allowStorage, forceRefetch, actorToken, bypassCollectionCheck), expectedType);
	}

	public <T> T resolveLocally(URI link, Class<T> expectedType){
		try{
			if(Config.isLocal(link)){
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
						return ensureTypeAndCast(msgs.getFirst(), expectedType);
				}

				matcher=ALBUMS.matcher(link.getPath());
				if(matcher.find()){
					long id=XTEA.deobfuscateObjectID(Utils.decodeLong(matcher.group(1)), ObfuscatedObjectIDType.PHOTO_ALBUM);
					return ensureTypeAndCast(context.getPhotosController().getAlbumIgnoringPrivacy(id), expectedType);
				}

				matcher=PHOTOS.matcher(link.getPath());
				if(matcher.find()){
					long id=XTEA.deobfuscateObjectID(Utils.decodeLong(matcher.group(1)), ObfuscatedObjectIDType.PHOTO);
					return ensureTypeAndCast(context.getPhotosController().getPhotoIgnoringPrivacy(id), expectedType);
				}

				matcher=COMMENTS.matcher(link.getPath());
				if(matcher.find()){
					long id=XTEA.deobfuscateObjectID(Utils.decodeLong(matcher.group(1)), ObfuscatedObjectIDType.COMMENT);
					return ensureTypeAndCast(context.getCommentsController().getCommentIgnoringPrivacy(id), expectedType);
				}

				matcher=TOPICS.matcher(link.getPath());
				if(matcher.find()){
					return ensureTypeAndCast(context.getBoardController().getTopicIgnoringPrivacy(XTEA.decodeObjectID(matcher.group(1), ObfuscatedObjectIDType.BOARD_TOPIC)), expectedType);
				}
			}else{
				ObjectTypeAndID tid=FederationStorage.getObjectTypeAndID(link);
				if(tid!=null){
					if(tid.type==ObjectType.USER && expectedType.isAssignableFrom(ForeignUser.class)){
						User user=UserStorage.getUserByActivityPubID(link);
						if(user!=null)
							return ensureTypeAndCast(user, expectedType);
						user=serviceActorCache.get(link);
						if(user!=null)
							return ensureTypeAndCast(user, expectedType);
					}
					if(tid.type==ObjectType.GROUP && expectedType.isAssignableFrom(ForeignGroup.class)){
						ForeignGroup group=GroupStorage.getForeignGroupByActivityPubID(link);
						if(group!=null)
							return ensureTypeAndCast(group, expectedType);
					}
					if(tid.type==ObjectType.POST && expectedType.isAssignableFrom(Post.class)){
						Post post=PostStorage.getPostByID(link);
						if(post!=null)
							return ensureTypeAndCast(post, expectedType);
					}
					if(tid.type==ObjectType.MESSAGE && expectedType.isAssignableFrom(MailMessage.class)){
						List<MailMessage> msgs=MailStorage.getMessages(link);
						if(!msgs.isEmpty())
							return ensureTypeAndCast(msgs.getFirst(), expectedType);
					}
					if(tid.type==ObjectType.PHOTO_ALBUM && expectedType.isAssignableFrom(PhotoAlbum.class)){
						long id=PhotoStorage.getAlbumIdByActivityPubId(link);
						if(id!=-1)
							return ensureTypeAndCast(context.getPhotosController().getAlbumIgnoringPrivacy(id), expectedType);
					}
					if(tid.type==ObjectType.PHOTO && expectedType.isAssignableFrom(Photo.class)){
						long id=PhotoStorage.getPhotoIdByActivityPubId(link);
						if(id!=-1)
							return ensureTypeAndCast(context.getPhotosController().getPhotoIgnoringPrivacy(id), expectedType);
					}
					if(tid.type==ObjectType.COMMENT && expectedType.isAssignableFrom(Comment.class)){
						long id=CommentStorage.getCommentIdByActivityPubId(link);
						if(id!=-1)
							return ensureTypeAndCast(context.getCommentsController().getCommentIgnoringPrivacy(id), expectedType);
					}
					if(tid.type==ObjectType.USER_STATUS && expectedType.isAssignableFrom(ActorStatus.class)){
						User user=context.getUsersController().getUserOrThrow(tid.idInt());
						if(user.status!=null && link.equals(user.status.apId()))
							return ensureTypeAndCast(user.status, expectedType);
					}
					if(tid.type==ObjectType.GROUP_STATUS && expectedType.isAssignableFrom(ActorStatus.class)){
						Group group=context.getGroupsController().getGroupOrThrow(tid.idInt());
						if(group.status!=null && link.equals(group.status.apId()))
							return ensureTypeAndCast(group.status, expectedType);
					}
					if(tid.type==ObjectType.BOARD_TOPIC && expectedType.isAssignableFrom(BoardTopic.class)){
						BoardTopic topic=context.getBoardController().getTopicIgnoringPrivacy(tid.id);
						return ensureTypeAndCast(topic, expectedType);
					}
				}else{
					if(expectedType.isAssignableFrom(ForeignUser.class)){
						User user=serviceActorCache.get(link);
						if(user!=null)
							return ensureTypeAndCast(user, expectedType);
					}
				}
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		throw new ObjectNotFoundException("Can't resolve object link locally: "+link);
	}

	public void storeOrUpdateRemoteObject(Object o, ActivityPubObject origObj){
		try{
			switch(o){
				case ForeignUser fu -> {
					if(fu.isServiceActor){
						serviceActorCache.put(fu.activityPubID, fu);
					}else{
						User existing=null;
						if(fu.id!=0){
							existing=UserStorage.getById(fu.id);
						}
						if(fu.movedToURL!=null){
							User movedTo=null;
							if(fu.movedTo>0){
								try{
									movedTo=context.getUsersController().getUserOrThrow(fu.movedTo);
								}catch(ObjectNotFoundException ignore){}
							}
							if(movedTo==null || !Objects.equals(movedTo.activityPubID, fu.movedToURL))
								handleNewlyFetchedMovedUser(fu);
						}
						if(fu.following!=null && fu.followers!=null){
							ArrayList<FetchCollectionTotalTask> collections=new ArrayList<>();
							collections.add(new FetchCollectionTotalTask(context, fu.following));
							collections.add(new FetchCollectionTotalTask(context, fu.followers));
							if(fu.getFriendsURL() instanceof URI friends){
								collections.add(new FetchCollectionTotalTask(context, friends));
							}
							List<Future<Long>> totals=context.getActivityPubWorker().invokeAll(collections);
							long followingCount=-1, followersCount=-1, friendsCount=-1;
							try{
								followingCount=totals.get(0).get();
							}catch(ExecutionException ignored){}
							try{
								followersCount=totals.get(1).get();
							}catch(ExecutionException ignored){}
							if(collections.size()>2){
								try{
									friendsCount=totals.get(2).get();
								}catch(ExecutionException ignored){}
							}
							if(followingCount!=-1)
								fu.setFollowingCount(followingCount);
							if(followersCount!=-1)
								fu.setFollowersCount(followersCount);
							if(friendsCount!=-1)
								fu.setFriendsCount(friendsCount);
						}
						UserStorage.putOrUpdateForeignUser(fu);
						maybeUpdateServerFeaturesFromActor(fu);
						User partner=null;
						if(fu.relationshipPartnerActivityPubID!=null && fu.relationshipPartnerID==0){
							try{
								partner=resolve(fu.relationshipPartnerActivityPubID, User.class, true, true, false);
								fu.relationshipPartnerID=partner.id;
								UserStorage.putOrUpdateForeignUser(fu);
							}catch(ObjectNotFoundException ignore){}
						}
						if(existing!=null && fu.relationship!=null){
							context.getUsersController().maybeCreateRelationshipStatusNewsfeedEntry(existing, fu.relationship, partner);
						}
					}
				}
				case ForeignGroup fg -> {
					for(GroupAdmin adm:fg.adminsForActivityPub){
						try{
							adm.user=resolve(adm.activityPubUserID, User.class, true, false, false);
						}catch(ObjectNotFoundException ignore){}
					}
					fg.adminsForActivityPub.removeIf(adm->adm.user==null);
					for(GroupAdmin adm:fg.adminsForActivityPub){
						storeOrUpdateRemoteObject(adm.user, adm.user);
					}
					GroupStorage.putOrUpdateForeignGroup(fg);
					maybeUpdateServerFeaturesFromActor(fg);
				}
				case Post p -> PostStorage.putForeignWallPost(p);
				case PhotoAlbum pa -> context.getPhotosController().putOrUpdateForeignAlbum(pa);
				case Photo p -> context.getPhotosController().putOrUpdateForeignPhoto(p, (ActivityPubPhoto) origObj);
				case Comment c -> context.getCommentsController().putOrUpdateForeignComment(c);
				case BoardTopic t -> {
					if(t.id>0){
						BoardTopic old=context.getBoardController().getTopicIgnoringPrivacy(t.id);
						if(!Objects.equals(old.title, t.title))
							context.getBoardController().renameTopic(t, t.title);
						if(!Objects.equals(old.pinnedAt, t.pinnedAt))
							context.getBoardController().setTopicPinned(t, t.pinnedAt);
						if(old.isClosed!=t.isClosed)
							context.getBoardController().setTopicClosed(t, t.isClosed);
					}else{
						ActivityPubBoardTopic origTopic=(ActivityPubBoardTopic) origObj;
						NoteOrQuestion firstComment;
						if(uriHostMatches(origTopic.activityPubID, origTopic.firstCommentID) && origTopic.first!=null
								&& origTopic.first.object instanceof CollectionPage page && !page.items.isEmpty() && page.items.getFirst().object instanceof NoteOrQuestion noq){
							firstComment=noq;
						}else{
							firstComment=resolve(origTopic.firstCommentID, NoteOrQuestion.class, true, false, false);
						}
						context.getBoardController().putForeignTopic(t, firstComment);
					}
				}
				case null, default -> {}
			}
		}catch(SQLException|InterruptedException x){
			throw new InternalServerErrorException(x);
		}
	}

	private void maybeUpdateServerFeaturesFromActor(Actor actor){
		EnumSet<Server.Feature> features=EnumSet.noneOf(Server.Feature.class);
		if(actor.hasWall()){
			features.add(Server.Feature.WALL_POSTS);
		}
		if(actor.hasPhotoAlbums()){
			features.add(Server.Feature.PHOTO_ALBUMS);
		}
		if(!features.isEmpty()){
			context.getModerationController().addServerFeatures(actor.domain, features);
		}
	}

	private static <T> T ensureTypeAndCast(Object obj, Class<T> type){
		if(type.isInstance(obj))
			return type.cast(obj);
		throw new UnsupportedRemoteObjectTypeException("Expected object of type "+type.getName()+", but got "+obj.getClass().getName()+" instead");
	}

	public <T extends ActivityPubObject> T convertToActivityPubObject(Object o, Class<T> type){
		if(o instanceof ActivityPubObject apo)
			return ensureTypeAndCast(apo, type);
		if(type.isAssignableFrom(NoteOrQuestion.class)){
			if(o instanceof Post post)
				return type.cast(NoteOrQuestion.fromNativePost(post, context));
			if(o instanceof MailMessage message)
				return type.cast(NoteOrQuestion.fromNativeMessage(message, context));
			if(o instanceof Comment comment)
				return type.cast(NoteOrQuestion.fromNativeComment(comment, context));
		}
		if(type.isAssignableFrom(ActivityPubPhotoAlbum.class) && o instanceof PhotoAlbum pa){
			return type.cast(ActivityPubPhotoAlbum.fromNativeAlbum(pa, context));
		}else if(type.isAssignableFrom(ActivityPubPhoto.class) && o instanceof Photo p){
			return type.cast(ActivityPubPhoto.fromNativePhoto(p, context.getPhotosController().getAlbumIgnoringPrivacy(p.albumID), context));
		}else if(type.isAssignableFrom(ActivityPubBoardTopic.class) && o instanceof BoardTopic t){
			return type.cast(ActivityPubBoardTopic.fromNativeTopic(t, context));
		}
		throw new UnsupportedRemoteObjectTypeException("Native type "+o.getClass().getName()+" does not have an ActivityPub representation");
	}

	public <T> T convertToNativeObject(ActivityPubObject o, Class<T> type){
		if(o instanceof NoteOrQuestion noq && noq.isWallPostOrComment(context) && type.isAssignableFrom(Post.class)){
			return type.cast(noq.asNativePost(context));
		}else if(o instanceof NoteOrQuestion noq && type.isAssignableFrom(Comment.class)){
			return type.cast(noq.asNativeComment(context));
		}else if(o instanceof ActivityPubPhotoAlbum pa && type.isAssignableFrom(PhotoAlbum.class)){
			return type.cast(pa.asNativePhotoAlbum(context));
		}else if(o instanceof ActivityPubPhoto p && type.isAssignableFrom(Photo.class)){
			return type.cast(p.asNativePhoto(context));
		}else if(o instanceof ActivityPubBoardTopic t && type.isAssignableFrom(BoardTopic.class)){
			return type.cast(t.asNativeTopic(context));
		}else if(type.isAssignableFrom(o.getClass())){
			return type.cast(o);
		}
		throw new UnsupportedRemoteObjectTypeException("Can't convert ActivityPub "+o.getClass().getName()+" to a native object of type "+type.getName());
	}

	public void ensureObjectIsInCollection(@NotNull Actor collectionOwner, @NotNull URI collectionID, @NotNull URI objectID){
		LOG.debug("Checking whether object {} belongs to collection {} owned by {}", objectID, collectionID, collectionOwner.activityPubID);
		if(Config.isLocal(collectionID))
			throw new FederationException(collectionID+" is a local collection. Must submit this object with a Create activity first.");
		if(!performCollectionQuery(collectionOwner, collectionID, objectID))
			throw new FederationException("Object "+objectID+" is not in collection "+collectionID);
		LOG.debug("Object {} was confirmed to be contained in {}", objectID, collectionID);
	}

	public boolean performCollectionQuery(@NotNull Actor collectionOwner, @NotNull URI collectionID, @NotNull URI objectID){
		if(collectionOwner.collectionQueryEndpoint==null)
			return true; // There's nothing we can do anyway
		if(Utils.uriHostMatches(collectionID, objectID))
			return true; // This collection is on the same server as the object. We trust that that server is sane.
		CollectionQueryResult cqr=ActivityPub.performCollectionQuery(collectionOwner, collectionID, List.of(objectID));
		List<LinkOrObject> res=cqr.items;
		return !res.isEmpty() && objectID.equals(res.getFirst().link);
	}

	public UsernameResolutionResult resolveUsernameLocally(String username){
		return resolveUsername(username, false, EnumSet.allOf(UsernameOwnerType.class));
	}

	public UsernameResolutionResult resolveUsername(String username, boolean allowFetching, EnumSet<UsernameOwnerType> allowedTypes){
		if(allowedTypes.isEmpty())
			throw new IllegalArgumentException("allowedTypes can't be empty");

		String name, domain;
		if(username.contains("@")){
			Matcher matcher=TextProcessor.USERNAME_DOMAIN_PATTERN.matcher(username);
			if(!matcher.find())
				throw new ObjectNotFoundException();
			name=matcher.group(1);
			domain=matcher.group(2);
			if(domain.equalsIgnoreCase(Config.domain))
				domain=null;
		}else{
			name=username;
			domain=null;
		}

		username=name+(domain==null ? "" : ("@"+domain));

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

		if(allowFetching && domain!=null){
			URI id;
			try{
				id=ActivityPub.resolveUsername(name, domain);
			}catch(IOException x){
				throw new ObjectNotFoundException("Can't resolve username: "+username, x);
			}
			Actor actor=resolve(id, Actor.class, true, false, false);
			if(actor instanceof Group g && allowedTypes.contains(UsernameOwnerType.GROUP)){
				storeOrUpdateRemoteObject(g, g);
				return new UsernameResolutionResult(UsernameOwnerType.GROUP, g.id);
			}else if(actor instanceof User u && allowedTypes.contains(UsernameOwnerType.USER)){
				storeOrUpdateRemoteObject(u, u);
				return new UsernameResolutionResult(UsernameOwnerType.GROUP, u.id);
			}else{
				throw new ObjectNotFoundException();
			}
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

	public static ObjectTypeAndID getObjectIdFromLocalURL(URI uri){
		if(!Config.isLocal(uri))
			throw new IllegalArgumentException("Not a local URL");

		String path=uri.getPath();
		Matcher matcher=POSTS.matcher(path);
		if(matcher.find()){
			return new ObjectTypeAndID(ObjectType.POST, Integer.parseInt(matcher.group(1)));
		}

		matcher=USERS.matcher(path);
		if(matcher.find()){
			return new ObjectTypeAndID(ObjectType.USER, Integer.parseInt(matcher.group(1)));
		}

		matcher=GROUPS.matcher(path);
		if(matcher.find()){
			return new ObjectTypeAndID(ObjectType.GROUP, Integer.parseInt(matcher.group(1)));
		}

		matcher=MESSAGES.matcher(path);
		if(matcher.find()){
			return new ObjectTypeAndID(ObjectType.MESSAGE, Utils.decodeLong(matcher.group(1)));
		}

		matcher=ALBUMS.matcher(path);
		if(matcher.find()){
			return new ObjectTypeAndID(ObjectType.PHOTO_ALBUM, XTEA.deobfuscateObjectID(Utils.decodeLong(matcher.group(1)), ObfuscatedObjectIDType.PHOTO_ALBUM));
		}

		matcher=PHOTOS.matcher(path);
		if(matcher.find()){
			return new ObjectTypeAndID(ObjectType.PHOTO, XTEA.deobfuscateObjectID(Utils.decodeLong(matcher.group(1)), ObfuscatedObjectIDType.PHOTO));
		}

		matcher=COMMENTS.matcher(path);
		if(matcher.find()){
			return new ObjectTypeAndID(ObjectType.COMMENT, XTEA.deobfuscateObjectID(Utils.decodeLong(matcher.group(1)), ObfuscatedObjectIDType.COMMENT));
		}

		matcher=TOPICS.matcher(path);
		if(matcher.find()){
			return new ObjectTypeAndID(ObjectType.BOARD_TOPIC, XTEA.deobfuscateObjectID(Utils.decodeLong(matcher.group(1)), ObfuscatedObjectIDType.BOARD_TOPIC));
		}

		return null;
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

	// Types for objects that AP IDs can point to, in "ap_id_index" database table
	// They are FourCCs for easier extensibility
	public enum ObjectType{
		USER('U', 'S', 'E', 'R'),
		GROUP('G', 'R', 'U', 'P'),
		POST('P', 'O', 'S', 'T'),
		MESSAGE('D', 'M', 'S', 'G'),
		PHOTO_ALBUM('P', 'A', 'L', 'B'),
		PHOTO('P', 'H', 'T', 'O'),
		COMMENT('C', 'M', 'N', 'T'),
		USER_STATUS('U', 'S', 'T', 'A'),
		GROUP_STATUS('G', 'S', 'T', 'A'),
		BOARD_TOPIC('B', 'T', 'O', 'P');

		public final int id;

		ObjectType(char... id){
			this.id=((int)id[0] << 24) | ((int)id[1] << 16) | ((int)id[2] << 8) | (int)id[3];
		}

		public static ObjectType fromID(int id){
			for(ObjectType t:values()){
				if(t.id==id)
					return t;
			}
			throw new IllegalArgumentException("Unknown object type ID '"+Utils.decodeFourCC(id)+"'");
		}
	}

	public record ObjectTypeAndID(ObjectType type, long id){
		public int idInt(){
			return (int)id;
		}

		public static ObjectTypeAndID fromResultSet(ResultSet res) throws SQLException{
			return new ObjectTypeAndID(ObjectType.fromID(res.getInt("object_type")), res.getLong("object_id"));
		}
	}
}
