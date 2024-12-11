package smithereen.controllers;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpTimeoutException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.FederationException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.RemoteObjectFetchException;
import smithereen.exceptions.UnsupportedRemoteObjectTypeException;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.SearchResult;
import smithereen.model.User;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentReplyParent;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.util.QuickSearchResults;
import smithereen.storage.GroupStorage;
import smithereen.storage.SearchStorage;
import smithereen.storage.UserStorage;
import smithereen.text.TextProcessor;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class SearchController{
	private static final Logger LOG=LoggerFactory.getLogger(SearchController.class);

	private final ApplicationContext context;

	public SearchController(ApplicationContext context){
		this.context=context;
	}

	public QuickSearchResults quickSearch(String query, User self){
		try{
			if(StringUtils.isEmpty(query) || query.length()<2)
				return new QuickSearchResults(List.of(), List.of(), List.of());

			List<User> users=List.of();
			List<Group> groups=List.of();
			List<URI> externalObjects=List.of();
			if(isURL(query)){
				if(!query.startsWith("http:") && !query.startsWith("https:"))
					query="https://"+query;
				query=normalizeURLDomain(query);
				URI uri=URI.create(query);
				try{
					ActivityPubObject obj=context.getObjectLinkResolver().resolve(uri, ActivityPubObject.class, false, false, false);
					if(obj instanceof User u){
						users=List.of(u);
					}else if(obj instanceof Group g){
						groups=List.of(g);
					}else{
						externalObjects=List.of(uri);
					}
				}catch(ObjectNotFoundException x){
					if(!Config.isLocal(uri)){
						try{
							Actor actor=context.getObjectLinkResolver().resolve(uri, Actor.class, false, false, false);
							if(actor instanceof User u){
								users=List.of(u);
							}else if(actor instanceof Group g){
								groups=List.of(g);
							}else{
								throw new AssertionError();
							}
						}catch(ObjectNotFoundException|IllegalStateException xx){
							externalObjects=List.of(uri);
						}
					}
				}
			}else if(isUsernameAndDomain(query)){
				Matcher matcher=TextProcessor.USERNAME_DOMAIN_PATTERN.matcher(query);
				matcher.find();
				String username=matcher.group(1);
				String domain=matcher.group(2);
				String full=username;
				if(domain!=null)
					full+='@'+domain;
				User user=UserStorage.getByUsername(full);
				SearchResult sr;
				if(user!=null){
					users=List.of(user);
				}else{
					Group group=GroupStorage.getByUsername(full);
					if(group!=null){
						groups=List.of(group);
					}else{
						externalObjects=List.of(URI.create(full));
					}
				}
			}else{
				List<SearchResult> results=SearchStorage.search(query, self.id, 10);
				users=new ArrayList<>();
				groups=new ArrayList<>();
				for(SearchResult result:results){
					switch(result.type){
						case USER -> users.add(result.user);
						case GROUP -> groups.add(result.group);
					}
				}
			}
			return new QuickSearchResults(users, groups, externalObjects);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<User> searchUsers(String query, User self, int count){
		try{
			return UserStorage.getByIdAsList(SearchStorage.searchUsers(query, self.id, count));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> searchFriends(String query, User self, int offset, int count){
		try{
			PaginatedList<Integer> ids=SearchStorage.searchFriends(query, self.id, offset, count);
			return new PaginatedList<>(ids, UserStorage.getByIdAsList(ids.list));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Group> searchGroups(User self, String query, boolean events, User target, int offset, int count){
		try{
			PaginatedList<Integer> ids=SearchStorage.searchGroups(query, events, target.id, offset, count, self!=null && self.id==target.id);
			return new PaginatedList<>(ids, GroupStorage.getByIdAsList(ids.list));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Object loadRemoteObject(User self, String query){
		Object obj;
		URI uri=null;

		// Is it a username@domain?
		Matcher matcher=TextProcessor.USERNAME_DOMAIN_PATTERN.matcher(query);
		if(matcher.find() && matcher.start()==0 && matcher.end()==query.length()){
			String username=matcher.group(1);
			String domain=matcher.group(2);
			try{
				uri=ActivityPub.resolveUsername(username, domain);
			}catch(IOException x){
				LOG.debug("Error getting remote user", x);
				throw new RemoteObjectFetchException(x, RemoteObjectFetchException.ErrorType.NETWORK_ERROR, null);
			}
		}

		if(uri==null){
			try{
				uri=new URI(query);
			}catch(URISyntaxException x){
				throw new BadRequestException(x);
			}
		}

		// Try resolving locally first
		try{
			obj=context.getObjectLinkResolver().resolveLocally(uri, Object.class);
			return switch(obj){
				case Actor a -> a;
				case Post p ->{
					try{
						context.getActivityPubWorker().fetchAllReplies(p).get(30, TimeUnit.SECONDS);
					}catch(Throwable x){
						LOG.trace("Error fetching replies", x);
					}
					yield p;
				}
				case PhotoAlbum pa -> pa;
				case Comment c -> c;
				case Photo p -> p;
				default -> throw new RemoteObjectFetchException(RemoteObjectFetchException.ErrorType.UNSUPPORTED_OBJECT_TYPE, uri);
			};
		}catch(ObjectNotFoundException ignore){}

		// Didn't find anything locally. Let's fetch then
		try{
			obj=context.getObjectLinkResolver().resolve(uri, ActivityPubObject.class, true, false, false, (JsonObject) null, false);
		}catch(UnsupportedRemoteObjectTypeException x){
			LOG.debug("Unsupported remote object", x);
			throw new RemoteObjectFetchException(x, RemoteObjectFetchException.ErrorType.UNSUPPORTED_OBJECT_TYPE, uri);
		}catch(ObjectNotFoundException x){
			LOG.debug("Remote object not found", x);
			switch(x.getCause()){
				case HttpTimeoutException cause -> throw new RemoteObjectFetchException(cause, RemoteObjectFetchException.ErrorType.TIMEOUT, uri);
				case IOException cause -> throw new RemoteObjectFetchException(cause, RemoteObjectFetchException.ErrorType.NETWORK_ERROR, uri);
				case null, default -> throw new RemoteObjectFetchException(x, RemoteObjectFetchException.ErrorType.NOT_FOUND, uri);
			}
		}catch(Exception x){
			LOG.debug("Other remote fetch exception", x);
			throw new RemoteObjectFetchException(x, RemoteObjectFetchException.ErrorType.OTHER_ERROR, uri);
		}
		return switch(obj){
			case ForeignUser user -> {
				if(user.isServiceActor)
					throw new RemoteObjectFetchException(RemoteObjectFetchException.ErrorType.UNSUPPORTED_OBJECT_TYPE, uri);
				context.getObjectLinkResolver().storeOrUpdateRemoteObject(user);
				yield user;
			}
			case ForeignGroup group -> {
				group.storeDependencies(context);
				context.getObjectLinkResolver().storeOrUpdateRemoteObject(group);
				yield group;
			}
			case NoteOrQuestion post -> {
				if(post.inReplyTo==null){
					URI repostID=post.getQuoteRepostID();
					Post nativePost=post.asNativePost(context);
					if(repostID!=null){
						try{
							List<Post> repostChain=context.getActivityPubWorker().fetchRepostChain(post).get();
							if(!repostChain.isEmpty())
								nativePost.setRepostedPost(repostChain.getFirst());
						}catch(InterruptedException x){
							throw new RuntimeException(x);
						}catch(ExecutionException x){
							LOG.trace("Failed to fetch repost chain for {}", post.activityPubID, x);
						}
					}
					context.getWallController().loadAndPreprocessRemotePostMentions(nativePost, post);
					context.getObjectLinkResolver().storeOrUpdateRemoteObject(nativePost);
					try{
						context.getActivityPubWorker().fetchAllReplies(nativePost).get(30, TimeUnit.SECONDS);
					}catch(Throwable x){
						LOG.trace("Error fetching replies", x);
					}
					yield nativePost;
				}else if(post.isWallPostOrComment(context)){
					Future<List<Post>> future=context.getActivityPubWorker().fetchWallReplyThread(post);
					try{
						List<Post> posts=future.get(30, TimeUnit.SECONDS);
						try{
							context.getActivityPubWorker().fetchAllReplies(posts.getFirst()).get(30, TimeUnit.SECONDS);
						}catch(TimeoutException tx){
							LOG.debug("Timed out fetching all replies for {}", posts.getFirst().getActivityPubID(), tx);
						}
						yield post.asNativePost(context);
					}catch(InterruptedException x){
						throw new RuntimeException(x);
					}catch(ExecutionException e){
						LOG.trace("Error fetching remote object", e);
						Throwable x=e.getCause();
						switch(x){
							case UnsupportedRemoteObjectTypeException ignored -> throw new RemoteObjectFetchException(RemoteObjectFetchException.ErrorType.UNSUPPORTED_OBJECT_TYPE, uri);
							case ObjectNotFoundException onfe -> {
								switch(onfe.getCause()){
									case IOException iox -> throw new RemoteObjectFetchException(iox, RemoteObjectFetchException.ErrorType.NETWORK_ERROR, uri);
									case null, default -> throw new RemoteObjectFetchException(RemoteObjectFetchException.ErrorType.NOT_FOUND, uri);
								}
							}
							case IOException iox -> throw new RemoteObjectFetchException(iox, RemoteObjectFetchException.ErrorType.NETWORK_ERROR, uri);
							default -> throw new RemoteObjectFetchException(x, RemoteObjectFetchException.ErrorType.OTHER_ERROR, uri);
						}
					}catch(TimeoutException e){
						LOG.debug("Timed out fetching parent thread for {}", uri, e);
						throw new RemoteObjectFetchException("Timed out fetching parent reply thread", RemoteObjectFetchException.ErrorType.OTHER_ERROR, uri);
					}
				}else{
					Future<List<CommentReplyParent>> future=context.getActivityPubWorker().fetchCommentReplyThread(post);
					try{
						List<CommentReplyParent> comments=future.get(30, TimeUnit.SECONDS);
						yield comments.getLast();
					}catch(InterruptedException x){
						throw new RuntimeException(x);
					}catch(ExecutionException e){
						LOG.trace("Error fetching remote object", e);
						Throwable x=e.getCause();
						switch(x){
							case UnsupportedRemoteObjectTypeException ignored -> throw new RemoteObjectFetchException(RemoteObjectFetchException.ErrorType.UNSUPPORTED_OBJECT_TYPE, uri);
							case ObjectNotFoundException onfe -> {
								switch(onfe.getCause()){
									case IOException iox -> throw new RemoteObjectFetchException(iox, RemoteObjectFetchException.ErrorType.NETWORK_ERROR, uri);
									case null, default -> throw new RemoteObjectFetchException(RemoteObjectFetchException.ErrorType.NOT_FOUND, uri);
								}
							}
							case IOException iox -> throw new RemoteObjectFetchException(iox, RemoteObjectFetchException.ErrorType.NETWORK_ERROR, uri);
							default -> throw new RemoteObjectFetchException(x, RemoteObjectFetchException.ErrorType.OTHER_ERROR, uri);
						}
					}catch(TimeoutException x){
						LOG.debug("Timed out fetching parent thread for {}", uri, x);
						throw new RemoteObjectFetchException("Timed out fetching parent reply thread", RemoteObjectFetchException.ErrorType.OTHER_ERROR, uri);
					}
				}
			}
			case ActivityPubPhotoAlbum apAlbum -> {
				PhotoAlbum album=null;
				try{
					album=apAlbum.asNativePhotoAlbum(context);
					context.getObjectLinkResolver().storeOrUpdateRemoteObject(album);
					context.getActivityPubWorker().fetchPhotoAlbumContents(apAlbum, album).get(30, TimeUnit.SECONDS);
					yield album;
				}catch(FederationException | ExecutionException x){
					throw new RemoteObjectFetchException(x, RemoteObjectFetchException.ErrorType.UNSUPPORTED_OBJECT_TYPE, uri);
				}catch(InterruptedException x){
					throw new RuntimeException(x);
				}catch(TimeoutException x){
					yield album;
				}
			}
			case ActivityPubPhoto apPhoto -> {
				Photo photo=null;
				try{
					photo=apPhoto.asNativePhoto(context);
					PhotoAlbum album=context.getPhotosController().getAlbumIgnoringPrivacy(photo.albumID);
					context.getActivityPubWorker().fetchPhotoAlbumContents(ActivityPubPhotoAlbum.fromNativeAlbum(album, context), album).get(30, TimeUnit.SECONDS);
					yield photo;
				}catch(ObjectNotFoundException x){
					throw new RemoteObjectFetchException(x, RemoteObjectFetchException.ErrorType.NOT_FOUND, uri);
				}catch(InterruptedException x){
					throw new RuntimeException(x);
				}catch(TimeoutException x){
					yield photo;
				}catch(FederationException | ExecutionException x){
					LOG.debug("Photo album fetch failed", x);
					throw new RemoteObjectFetchException(x, RemoteObjectFetchException.ErrorType.UNSUPPORTED_OBJECT_TYPE, uri);
				}
			}
			default -> throw new RemoteObjectFetchException(Config.DEBUG ? ("Object type: "+obj.getClass().getName()) : null, RemoteObjectFetchException.ErrorType.UNSUPPORTED_OBJECT_TYPE, uri);
		};
	}
}
