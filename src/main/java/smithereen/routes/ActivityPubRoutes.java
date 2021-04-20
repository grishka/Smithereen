package smithereen.routes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import smithereen.activitypub.handlers.GroupBlockPersonHandler;
import smithereen.activitypub.handlers.GroupUndoBlockPersonHandler;
import smithereen.activitypub.handlers.PersonBlockPersonHandler;
import smithereen.activitypub.handlers.PersonUndoBlockPersonHandler;
import smithereen.activitypub.objects.activities.Block;
import smithereen.exceptions.BadRequestException;
import smithereen.BuildInfo;
import smithereen.ObjectLinkResolver;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityPub;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPubCache;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.DoublyNestedActivityTypeHandler;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.handlers.AcceptFollowGroupHandler;
import smithereen.activitypub.handlers.AcceptFollowPersonHandler;
import smithereen.activitypub.handlers.AnnounceNoteHandler;
import smithereen.activitypub.handlers.CreateNoteHandler;
import smithereen.activitypub.handlers.DeleteNoteHandler;
import smithereen.activitypub.handlers.DeletePersonHandler;
import smithereen.activitypub.handlers.FollowGroupHandler;
import smithereen.activitypub.handlers.FollowPersonHandler;
import smithereen.activitypub.handlers.LeaveGroupHandler;
import smithereen.activitypub.handlers.LikeNoteHandler;
import smithereen.activitypub.handlers.OfferFollowPersonHandler;
import smithereen.activitypub.handlers.RejectFollowPersonHandler;
import smithereen.activitypub.handlers.RejectOfferFollowPersonHandler;
import smithereen.activitypub.handlers.UndoAnnounceNoteHandler;
import smithereen.activitypub.handlers.UndoFollowGroupHandler;
import smithereen.activitypub.handlers.UndoFollowPersonHandler;
import smithereen.activitypub.handlers.UndoLikeNoteHandler;
import smithereen.activitypub.handlers.UpdateGroupHandler;
import smithereen.activitypub.handlers.UpdateNoteHandler;
import smithereen.activitypub.handlers.UpdatePersonHandler;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.Mention;
import smithereen.activitypub.objects.Tombstone;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.activitypub.objects.activities.Create;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Leave;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Offer;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.activitypub.objects.activities.Update;
import smithereen.data.Account;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.Group;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.jsonld.JLDProcessor;
import smithereen.jsonld.LinkedDataSignatures;
import smithereen.sparkext.ActivityPubCollectionPageResponse;
import smithereen.storage.GroupStorage;
import smithereen.storage.LikeStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class ActivityPubRoutes{

	private static ArrayList<ActivityTypeHandlerRecord<?, ?, ?, ?, ?>> typeHandlers=new ArrayList<>();

	public static void registerActivityHandlers(){
		registerActivityHandler(ForeignUser.class, Create.class, Post.class, new CreateNoteHandler());
		registerActivityHandler(ForeignUser.class, Like.class, Post.class, new LikeNoteHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Like.class, Post.class, new UndoLikeNoteHandler());
		registerActivityHandler(ForeignUser.class, Announce.class, Post.class, new AnnounceNoteHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Announce.class, Post.class, new UndoAnnounceNoteHandler());
		registerActivityHandler(ForeignUser.class, Update.class, Post.class, new UpdateNoteHandler());
		registerActivityHandler(ForeignUser.class, Delete.class, Post.class, new DeleteNoteHandler());

		registerActivityHandler(ForeignUser.class, Follow.class, User.class, new FollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Follow.class, User.class, new UndoFollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Accept.class, Follow.class, User.class, new AcceptFollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Reject.class, Follow.class, User.class, new RejectFollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Offer.class, Follow.class, User.class, new OfferFollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Reject.class, Offer.class, Follow.class, User.class, new RejectOfferFollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Update.class, ForeignUser.class, new UpdatePersonHandler());
		registerActivityHandler(ForeignUser.class, Delete.class, ForeignUser.class, new DeletePersonHandler());
		registerActivityHandler(ForeignUser.class, Block.class, User.class, new PersonBlockPersonHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Block.class, User.class, new PersonUndoBlockPersonHandler());

		registerActivityHandler(ForeignGroup.class, Update.class, ForeignGroup.class, new UpdateGroupHandler());
		registerActivityHandler(ForeignUser.class, Follow.class, Group.class, new FollowGroupHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Follow.class, Group.class, new UndoFollowGroupHandler());
		registerActivityHandler(ForeignUser.class, Leave.class, Group.class, new LeaveGroupHandler());
		registerActivityHandler(ForeignGroup.class, Accept.class, Follow.class, User.class, new AcceptFollowGroupHandler());
		registerActivityHandler(ForeignGroup.class, Block.class, User.class, new GroupBlockPersonHandler());
		registerActivityHandler(ForeignGroup.class, Undo.class, Block.class, User.class, new GroupUndoBlockPersonHandler());
	}

	@SuppressWarnings("SameParameterValue")
	private static <A extends Actor, T extends Activity, O extends ActivityPubObject> void registerActivityHandler(@NotNull Class<A> actorClass,
																												   @NotNull Class<T> activityClass,
																												   @NotNull Class<O> objectClass,
																												   @NotNull ActivityTypeHandler<A, T, O> handler){
		typeHandlers.add(new ActivityTypeHandlerRecord<>(actorClass, activityClass, null, null, objectClass, handler));
//		System.out.println("Registered handler "+handler.getClass().getName()+" for "+actorClass.getSimpleName()+" -> "+activityClass.getSimpleName()+"{"+objectClass.getSimpleName()+"}");
	}

	@SuppressWarnings("SameParameterValue")
	private static <A extends Actor, T extends Activity, N extends Activity, O extends ActivityPubObject> void registerActivityHandler(@NotNull Class<A> actorClass,
																												   @NotNull Class<T> activityClass,
																												   @NotNull Class<N> nestedActivityClass,
																												   @NotNull Class<O> objectClass,
																												   @NotNull NestedActivityTypeHandler<A, T, N, O> handler){
		typeHandlers.add(new ActivityTypeHandlerRecord<>(actorClass, activityClass, nestedActivityClass, null, objectClass, handler));
//		System.out.println("Registered handler "+handler.getClass().getName()+" for "+actorClass.getSimpleName()+" -> "+activityClass.getSimpleName()+"{"+nestedActivityClass.getSimpleName()+"{"+objectClass.getSimpleName()+"}}");
	}

	@SuppressWarnings("SameParameterValue")
	private static <A extends Actor, T extends Activity, N extends Activity, NN extends Activity, O extends ActivityPubObject> void registerActivityHandler(@NotNull Class<A> actorClass,
																																							@NotNull Class<T> activityClass,
																																							@NotNull Class<N> nestedActivityClass,
																																							@NotNull Class<NN> doublyNestedActivityClass,
																																							@NotNull Class<O> objectClass,
																																							@NotNull DoublyNestedActivityTypeHandler<A, T, N, NN, O> handler){
		typeHandlers.add(new ActivityTypeHandlerRecord<>(actorClass, activityClass, nestedActivityClass, doublyNestedActivityClass, objectClass, handler));
//		System.out.println("Registered handler "+handler.getClass().getName()+" for "+actorClass.getSimpleName()+" -> "+activityClass.getSimpleName()+"{"+nestedActivityClass.getSimpleName()+"{"+doublyNestedActivityClass.getSimpleName()+"{"+objectClass.getSimpleName()+"}}}");
	}

	public static Object userActor(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		Actor actor;
		if(username!=null){
			actor=UserStorage.getByUsername(username);
			if(actor==null){
				actor=GroupStorage.getByUsername(username);
			}
		}else{
			actor=UserStorage.getById(Utils.parseIntOrDefault(req.params(":id"), 0));
		}
		if(actor!=null && !(actor instanceof ForeignUser) && !(actor instanceof ForeignGroup)){
			if(actor instanceof Group){
				Group group=(Group) actor;
				group.adminsForActivityPub=GroupStorage.getGroupAdmins(group.id);
			}
			resp.type(ActivityPub.CONTENT_TYPE);
			return actor.asRootActivityPubObject();
		}
		throw new ObjectNotFoundException();
	}

	public static Object groupActor(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		Group group;
		if(username!=null)
			group=GroupStorage.getByUsername(username);
		else
			group=GroupStorage.getByID(Utils.parseIntOrDefault(req.params(":id"), 0));
		if(group!=null && !(group instanceof ForeignGroup)){
			group.adminsForActivityPub=GroupStorage.getGroupAdmins(group.id);
			resp.type(ActivityPub.CONTENT_TYPE);
			return group.asRootActivityPubObject();
		}
		throw new ObjectNotFoundException();
	}

	public static Object post(Request req, Response resp) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			throw new ObjectNotFoundException();
		}
		Post post=PostStorage.getPostByID(postID, true);
		if(post==null || !Config.isLocal(post.activityPubID)){
			throw new ObjectNotFoundException();
		}
		resp.type(ActivityPub.CONTENT_TYPE);
		return post.asRootActivityPubObject();
	}

	public static ActivityPubCollectionPageResponse postReplies(Request req, Response resp, int offset, int count) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		if(postID==0){
			throw new ObjectNotFoundException();
		}
		Post post=PostStorage.getPostByID(postID, true);
		if(post==null || !Config.isLocal(post.activityPubID)){
			throw new ObjectNotFoundException();
		}
		int[] _total={0};
		List<URI> ids=PostStorage.getImmediateReplyActivityPubIDs(post.getReplyKeyForReplies(), offset, count, _total);
		return ActivityPubCollectionPageResponse.forLinks(ids, _total[0]);
	}

	public static Object userInbox(Request req, Response resp) throws SQLException{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		User user=UserStorage.getById(id);
		if(user==null || user instanceof ForeignUser){
			throw new ObjectNotFoundException();
		}
		return inbox(req, resp, user);
	}

	public static Object groupInbox(Request req, Response resp) throws SQLException{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		Group group=GroupStorage.getByID(id);
		if(group==null || group instanceof ForeignGroup)
			throw new ObjectNotFoundException();
		return inbox(req, resp, group);
	}

	public static Object sharedInbox(Request req, Response resp) throws SQLException{
		return inbox(req, resp, null);
	}

	public static Object userOutbox(Request req, Response resp) throws SQLException{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		User user=UserStorage.getById(id);
		if(user==null || user instanceof ForeignUser){
			throw new ObjectNotFoundException();
		}
		int _minID=Utils.parseIntOrDefault(req.queryParams("min_id"), -1);
		int _maxID=Utils.parseIntOrDefault(req.queryParams("max_id"), -1);
		int minID=Math.max(0, _minID);
		int maxID=Math.max(0, _maxID);
		int[] _total={0};
		List<Post> posts=PostStorage.getWallPosts(user.id, false, minID, maxID, 0, _total, true);
		int total=_total[0];
		CollectionPage page=new CollectionPage(true);
		page.totalItems=total;
		page.items=new ArrayList<>();
		try{
			for(Post post:posts){
				Create activity=new Create();
				activity.object=new LinkOrObject(post);
				activity.published=post.published;
				activity.to=post.to;
				activity.cc=post.cc;
				activity.actor=new LinkOrObject(post.attributedTo);
				activity.activityPubID=new URI(post.activityPubID.getScheme(), post.activityPubID.getSchemeSpecificPart()+"/activityCreate", null);
				page.items.add(new LinkOrObject(activity));
			}
			URI baseURI=Config.localURI("/users/"+user.id+"/outbox");
			page.partOf=baseURI;
			if(posts.size()>0){
				page.next=URI.create(baseURI+"?max_id="+(posts.get(posts.size()-1).id-1));
				page.prev=URI.create(baseURI+"?min_id="+posts.get(0).id);
			}
			if(_minID!=-1)
				page.activityPubID=URI.create(baseURI+"?min_id="+minID);
			else
				page.activityPubID=URI.create(baseURI+"?max_id="+maxID);
			if(_minID==-1 && _maxID==-1){
				ActivityPubCollection collection=new ActivityPubCollection(true);
				collection.activityPubID=page.partOf;
				collection.totalItems=total;
				collection.first=new LinkOrObject(page);
				return collection.asRootActivityPubObject();
			}
		}catch(URISyntaxException ignore){}
		resp.type(ActivityPub.CONTENT_TYPE);
		return page.asRootActivityPubObject();
	}

	public static ActivityPubCollectionPageResponse groupOutbox(Request req, Response resp, int offset, int count){
		return ActivityPubCollectionPageResponse.forLinks(Collections.emptyList(), 0);
	}

	public static ActivityPubCollectionPageResponse userWall(Request req, Response resp, int offset, int count) throws SQLException{
		int id=parseIntOrDefault(req.params(":id"), 0);
		User user=UserStorage.getById(id);
		if(user==null || user instanceof ForeignUser)
			throw new ObjectNotFoundException();
		return actorWall(req, resp, offset, count, user);
	}

	public static ActivityPubCollectionPageResponse groupWall(Request req, Response resp, int offset, int count) throws SQLException{
		int id=parseIntOrDefault(req.params(":id"), 0);
		Group group=GroupStorage.getByID(id);
		if(group==null || group instanceof ForeignGroup)
			throw new ObjectNotFoundException();
		return actorWall(req, resp, offset, count, group);
	}

	private static ActivityPubCollectionPageResponse actorWall(Request req, Response resp, int offset, int count, Actor actor) throws SQLException{
		int[] total={0};
		List<URI> posts=PostStorage.getWallPostActivityPubIDs(actor.getLocalID(), actor instanceof Group, offset, count, total);
		return ActivityPubCollectionPageResponse.forLinks(posts, total[0]);
	}

	public static Object externalInteraction(Request req, Response resp, Account self) throws SQLException{
		// ?type=reblog
		// ?type=favourite
		// ?type=reply
		// user/remote_follow
		String ref=req.headers("referer");
		ActivityPubObject remoteObj;
		try{
			URI uri=new URI(req.queryParams("uri"));
			if(!"https".equals(uri.getScheme()) && !(Config.useHTTP && "http".equals(uri.getScheme()))){
				// try parsing as "username@domain" or "acct:username@domain"
				String rawUri=uri.getSchemeSpecificPart();
				if(rawUri.matches("^[^@\\s]+@[^@\\s]{4,}$")){
					String[] parts=rawUri.split("@");
					uri=ActivityPub.resolveUsername(parts[0], parts[1]);
					if(!"https".equals(uri.getScheme()) && !(Config.useHTTP && "http".equals(uri.getScheme()))){
						return "Failed to resolve object URI via webfinger";
					}
				}else{
					return "Invalid remote object URI";
				}
			}
			remoteObj=ObjectLinkResolver.resolve(uri, ActivityPubObject.class, true, true);
			if(remoteObj==null){
				return "Error fetching remote object";
			}
			if(remoteObj.activityPubID.getHost()==null || uri.getHost()==null || !remoteObj.activityPubID.getHost().equals(uri.getHost())){
				return "Object ID host doesn't match URI host";
			}
		}catch(IOException|JSONException|URISyntaxException x){
			return x.getMessage();
		}
		if(remoteObj instanceof ForeignUser){
			try{
				ForeignUser foreignUser=(ForeignUser) remoteObj;
//					System.out.println(foreignUser);
				UserStorage.putOrUpdateForeignUser(foreignUser);
				FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, foreignUser.id);
				if(status==FriendshipStatus.REQUEST_SENT){
					return Utils.wrapError(req, resp, "err_friend_req_already_sent");
				}else if(status==FriendshipStatus.FOLLOWING){
					return Utils.wrapError(req, resp, "err_already_following");
				}else if(status==FriendshipStatus.FRIENDS){
					return Utils.wrapError(req, resp, "err_already_friends");
				}
				return new RenderedTemplateResponse("remote_follow").with("user", foreignUser).with("title", Config.serverDisplayName).renderToString(req);
			}catch(Exception x){
				x.printStackTrace();
				return x.toString();
			}
		}else if(remoteObj instanceof ForeignGroup){
			ForeignGroup group=(ForeignGroup) remoteObj;
			GroupStorage.putOrUpdateForeignGroup(group);
			resp.redirect(Config.localURI("/"+group.getFullUsername()).toString());
			return "";
		}else if(remoteObj instanceof Post){
			try{
				// TODO refactor this to simulate Create{Note}
				Post post=(Post) remoteObj;
				if(post.user==null){
					ActivityPubObject obj=ActivityPub.fetchRemoteObject(post.attributedTo.toString());
					if(obj instanceof ForeignUser){
						post.user=(ForeignUser) obj;
						UserStorage.putOrUpdateForeignUser((ForeignUser) obj);
					}else{
						throw new IllegalArgumentException("Error fetching post author");
					}
				}
				if(post.owner==null){
					throw new UnsupportedOperationException("no post owner user - not yet implemented");
				}
				if(post.inReplyTo!=null){
					Post parent=PostStorage.getPostByID(post.inReplyTo);
					if(parent==null)
						throw new UnsupportedOperationException("no parent post - not yet implemented");
					post.setParent(parent);
				}
				if(post.tag!=null){
					for(ActivityPubObject tag:post.tag){
						if(tag instanceof Mention){
							URI uri=((Mention) tag).href;
							User mentionedUser=UserStorage.getUserByActivityPubID(uri);
							if(mentionedUser==null){
								try{
									ActivityPubObject _user=ActivityPub.fetchRemoteObject(uri.toString());
									if(_user instanceof ForeignUser){
										ForeignUser u=(ForeignUser) _user;
										UserStorage.putOrUpdateForeignUser(u);
										mentionedUser=u;
									}
								}catch(IOException ignore){}
							}
							if(mentionedUser!=null){
								if(post.mentionedUsers.isEmpty())
									post.mentionedUsers=new ArrayList<>();
								if(!post.mentionedUsers.contains(mentionedUser))
									post.mentionedUsers.add(mentionedUser);
							}
						}
					}
					if(!post.mentionedUsers.isEmpty() && StringUtils.isNotEmpty(post.content)){
						post.content=Utils.preprocessRemotePostMentions(post.content, post.mentionedUsers);
					}
				}
				PostStorage.putForeignWallPost(post);
				resp.redirect("/posts/"+post.id);
				return "";
			}catch(Exception x){
				x.printStackTrace();
				return x.toString();
			}
		}
		return "Referer: "+Utils.sanitizeHTML(ref)+"<hr/>URL: "+Utils.sanitizeHTML(req.queryParams("uri"))+"<hr/>Object:<br/><pre>"+Utils.sanitizeHTML(remoteObj.toString())+"</pre>";
	}

	public static Object remoteFollow(Request req, Response resp, Account self) throws SQLException{
		String username=req.params(":username");
		User _user=UserStorage.getByUsername(username);
//		System.out.println(_user);
		if(!(_user instanceof ForeignUser)){
			return Utils.wrapError(req, resp, "err_user_not_found");
		}
		ForeignUser user=(ForeignUser) _user;
		FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
		if(status==FriendshipStatus.REQUEST_SENT){
			return Utils.wrapError(req, resp, "err_friend_req_already_sent");
		}else if(status==FriendshipStatus.FOLLOWING){
			return Utils.wrapError(req, resp, "err_already_following");
		}else if(status==FriendshipStatus.FRIENDS){
			return Utils.wrapError(req, resp, "err_already_friends");
		}
		try{
			String msg=req.queryParams("message");
			if(user.supportsFriendRequests()){
				UserStorage.putFriendRequest(self.user.id, user.id, msg, false);
			}else{
				UserStorage.followUser(self.user.id, user.id, false);
			}
			Follow follow=new Follow();
			follow.actor=new LinkOrObject(self.user.activityPubID);
			follow.object=new LinkOrObject(user.activityPubID);
			follow.activityPubID=new URI(self.user.activityPubID.getScheme(), self.user.activityPubID.getSchemeSpecificPart(), "follow"+user.id);
			ActivityPub.postActivity(user.inbox, follow, self.user);
			if(user.supportsFriendRequests()){
				Offer offer=new Offer();
				offer.actor=new LinkOrObject(self.user.activityPubID);
				offer.activityPubID=new URI(self.user.activityPubID.getScheme(), self.user.activityPubID.getSchemeSpecificPart(), "friend_request"+user.id);
				if(StringUtils.isNotEmpty(msg)){
					offer.content=msg;
				}
				Follow revFollow=new Follow();
				revFollow.actor=new LinkOrObject(user.activityPubID);
				revFollow.object=new LinkOrObject(self.user.activityPubID);
				offer.object=new LinkOrObject(revFollow);
				ActivityPub.postActivity(user.inbox, offer, self.user);
			}
			return "Success";
		}catch(URISyntaxException ignore){
		}catch(IOException x){
			x.printStackTrace();
			return x.toString();
		}
		return "";
	}

	public static Object nodeInfo(Request req, Response resp) throws SQLException{
		resp.type("application/json; profile=\"http://nodeinfo.diaspora.software/ns/schema/2.0#\"");
		JSONObject root=new JSONObject();
		root.put("version", "2.0");
		root.put("protocols", Collections.singletonList("activitypub"));
		JSONObject software=new JSONObject();
		software.put("name", "smithereen");
		software.put("version", BuildInfo.VERSION);
		root.put("software", software);
		root.put("openRegistrations", Config.signupMode==Config.SignupMode.OPEN);
		JSONObject usage=new JSONObject();
		JSONObject users=new JSONObject();
		users.put("total", UserStorage.getLocalUserCount());
		usage.put("users", users);
		usage.put("localPosts", PostStorage.getLocalPostCount(false));
		usage.put("localComments", PostStorage.getLocalPostCount(true));
		root.put("usage", usage);
		JSONObject services=new JSONObject();
		services.put("inbound", Collections.EMPTY_LIST);
		services.put("outbound", Collections.EMPTY_LIST);
		root.put("services", services);
		JSONObject meta=new JSONObject();
		JSONArray caps=new JSONArray();
		caps.put("friendRequests");
		meta.put("capabilities", caps);
		root.put("metadata", meta);
		return root;
	}

	public static Object likeObject(Request req, Response resp) throws SQLException{
		int id=parseIntOrDefault(req.params(":likeID"), 0);
		if(id==0)
			throw new ObjectNotFoundException();
		Like l=LikeStorage.getByID(id);
		if(l==null)
			throw new ObjectNotFoundException();
		resp.type(ActivityPub.CONTENT_TYPE);
		return l.asRootActivityPubObject();
	}

	public static Object undoLikeObject(Request req, Response resp) throws SQLException{
		int id=parseIntOrDefault(req.params(":likeID"), 0);
		if(id==0)
			throw new ObjectNotFoundException();
		Undo undo=ActivityPubCache.getUndoneLike(id);
		if(undo==null)
			throw new ObjectNotFoundException();
		return undo.asRootActivityPubObject();
	}

	private static Object inbox(Request req, Response resp, Actor owner) throws SQLException{
		if(req.headers("digest")!=null){
			if(!verifyHttpDigest(req.headers("digest"), req.bodyAsBytes())){
				throw new BadRequestException("Digest verification failed");
			}
		}
		String body=req.body();
		System.out.println(body);
		JSONObject rawActivity=new JSONObject(body);
		JSONObject obj=JLDProcessor.convertToLocalContext(rawActivity);

		Activity activity;
		ActivityPubObject o;
		try{
			o=ActivityPubObject.parse(obj);
		}catch(Exception e){
			throw new BadRequestException("Failed to parse object: "+e.toString());
		}
		if(o instanceof Activity)
			activity=(Activity)o;
		else
			throw new BadRequestException("Unsupported object type '"+o.getType()+"'");

		if(Config.isLocal(activity.actor.link))
			throw new BadRequestException("User domain must be different from this server");

		ForeignUser user=UserStorage.getForeignUserByActivityPubID(activity.actor.link);
		if(user==null || user.lastUpdated==null || System.currentTimeMillis()-user.lastUpdated.getTime()>24L*60*60*1000){
			// special case: when users delete themselves but are not in local database, ignore that
			if(activity instanceof Delete && activity.object.link!=null && activity.object.link.equals(activity.actor.link))
				return "";

			try{
				ActivityPubObject userObj=ActivityPub.fetchRemoteObject(activity.actor.link.toString());
				if(!(userObj instanceof ForeignUser)){
					resp.status(400);
					return "Actor object must have Person type";
				}
				user=(ForeignUser) userObj;
				UserStorage.putOrUpdateForeignUser(user);
			}catch(SQLException x){
				throw new SQLException(x);
			}catch(Exception x){
				if(user==null)
					throw new BadRequestException(x.toString());
			}
		}

		ForeignUser httpSigOwner;
		try{
			httpSigOwner=verifyHttpSignature(req, user);
		}catch(Exception x){
			x.printStackTrace();
			resp.status(400);
			return x.toString();
		}

		// if the activity has an LD-signature, verify that and allow any (cached) user to sign the HTTP signature
		// if it does not, the owner of the HTTP signature must match the actor
		boolean hasValidLDSignature=false;
		if(rawActivity.has("signature")){
			JSONObject sig=rawActivity.getJSONObject("signature");
			try{
				URI keyID=URI.create(sig.getString("creator"));
				URI userID=Utils.userIdFromKeyId(keyID);
				if(!userID.equals(user.activityPubID)){
					resp.status(400);
					return "LD-signature creator is not activity actor";
				}
				if(!LinkedDataSignatures.verify(rawActivity, user.publicKey)){
					resp.status(400);
					return "LD-signature verification failed";
				}
				System.out.println("verified LD signature by "+userID);
				hasValidLDSignature=true;
			}catch(Exception x){
				x.printStackTrace();
				resp.status(400);
				return x.toString();
			}
		}else{
			if(!user.equals(httpSigOwner)){
				resp.status(400);
				return "In the absence of an LD-signature, HTTP signature must be made by the activity actor";
			}
			System.out.println("verified HTTP signature by "+httpSigOwner.activityPubID);
		}
		// parse again to make sure the actor is set everywhere
		try{
			ActivityPubObject _o=ActivityPubObject.parse(obj);
			if(_o instanceof Activity)
				activity=(Activity) _o;
		}catch(Exception ignore){}

		ActivityHandlerContext context=new ActivityHandlerContext(body, hasValidLDSignature ? user : null, httpSigOwner);

		try{
			ActivityPubObject aobj;
			if(activity.object.object!=null){
				aobj=activity.object.object;
				// special case: Mastodon sends Delete{Tombstone} for post deletions
				if(aobj instanceof Tombstone){
					try{
						aobj=ObjectLinkResolver.resolve(aobj.activityPubID);
					}catch(ObjectNotFoundException x){
						// Fail silently. We didn't have that object anyway, there's nothing to delete.
						return "";
					}
				}
			}else{
				// special case: fetch the object Announce{Note}
				aobj=ObjectLinkResolver.resolve(activity.object.link, ActivityPubObject.class, activity instanceof Announce, false);
			}
			for(ActivityTypeHandlerRecord r : typeHandlers){
				if(r.actorClass.isInstance(user)){
					if(r.activityClass.isInstance(activity)){
						if(r.nestedActivityClass!=null && aobj instanceof Activity && r.nestedActivityClass.isInstance(aobj)){
							Activity nestedActivity=(Activity)aobj;
							ActivityPubObject nestedObject;
							if(nestedActivity.object.object!=null)
								nestedObject=nestedActivity.object.object;
							else
								nestedObject=ObjectLinkResolver.resolve(nestedActivity.object.link);

							if(r.doublyNestedActivityClass!=null && nestedObject instanceof Activity && r.doublyNestedActivityClass.isInstance(nestedObject)){
								Activity doublyNestedActivity=(Activity)nestedObject;
								ActivityPubObject doublyNestedObject;
								if(doublyNestedActivity.object.object!=null)
									doublyNestedObject=nestedActivity.object.object;
								else
									doublyNestedObject=ObjectLinkResolver.resolve(nestedActivity.object.link);

								if(r.objectClass.isInstance(doublyNestedObject)){
									System.out.println("Found match: "+r.handler.getClass().getName());
									((DoublyNestedActivityTypeHandler)r.handler).handle(context, user, activity, nestedActivity, doublyNestedActivity, doublyNestedObject);
									return "";
								}
							}else if(r.objectClass.isInstance(nestedObject)){
								System.out.println("Found match: "+r.handler.getClass().getName());
								((NestedActivityTypeHandler)r.handler).handle(context, user, activity, nestedActivity, nestedObject);
								return "";
							}
						}else if(r.objectClass.isInstance(aobj)){
							System.out.println("Found match: "+r.handler.getClass().getName());
							r.handler.handle(context, user, activity, aobj);
							return "";
						}
					}
				}
			}
		}catch(Exception x){
			x.printStackTrace();
			throw new BadRequestException(x.toString());
		}
		throw new BadRequestException("No handler found for activity type: "+getActivityType(activity));
	}

	private static String getActivityType(ActivityPubObject obj){
		String r=obj.getType();
		if(obj instanceof Activity){
			Activity a=(Activity)obj;
			r+="{";
			if(a.object.object!=null){
				r+=getActivityType(a.object.object);
			}else{
				r+=a.object.link;
			}
			r+="}";
		}
		return r;
	}

	private static ActivityPubCollectionPageResponse followersOrFollowing(Request req, Response resp, boolean isFollowers, int offset, int count) throws SQLException{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		User user=UserStorage.getById(id);
		if(user==null || user instanceof ForeignUser){
			throw new ObjectNotFoundException();
		}
		int[] _total={0};
		List<URI> followers=UserStorage.getUserFollowerURIs(user.id, isFollowers, offset, count, _total);
		return ActivityPubCollectionPageResponse.forLinks(followers, _total[0]);
	}

	public static ActivityPubCollectionPageResponse userFollowers(Request req, Response resp, int offset, int count) throws SQLException{
		return followersOrFollowing(req, resp, true, offset, count);
	}

	public static ActivityPubCollectionPageResponse userFollowing(Request req, Response resp, int offset, int count) throws SQLException{
		return followersOrFollowing(req, resp, false, offset, count);
	}

	public static ActivityPubCollectionPageResponse groupFollowers(Request req, Response resp, int offset, int count) throws SQLException{
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		Group group=GroupStorage.getByID(id);
		if(group==null || group instanceof ForeignGroup){
			throw new ObjectNotFoundException();
		}
		int[] _total={0};
		List<URI> followers=GroupStorage.getGroupMemberURIs(group.id, false, offset, count, _total);
		return ActivityPubCollectionPageResponse.forLinks(followers, _total[0]);
	}



	private static ForeignUser verifyHttpSignature(Request req, ForeignUser userHint) throws ParseException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, SQLException{
		String sigHeader=req.headers("Signature");
		if(sigHeader==null)
			throw new IllegalArgumentException("Request is missing Signature header");
		String[] parts=sigHeader.split(",");
		String keyId=null;
		byte[] signature=null;
		List<String> headers=null;
		for(String part:parts){
			String[] kv=part.split("=", 2);
			String key=kv[0];
			String value=kv[1];
			if(value.charAt(0)=='"'){
				value=value.substring(1, value.length()-1);
			}
			switch(key){
				case "keyId":
					keyId=value;
					break;
				case "signature":
					signature=Base64.getDecoder().decode(value);
					break;
				case "headers":
					headers=Arrays.asList(value.split(" "));
					break;
			}
		}
		if(keyId==null)
			throw new IllegalArgumentException("Signature header is missing keyId field");
		if(signature==null)
			throw new IllegalArgumentException("Signature header is missing signature field");
		if(headers==null)
			throw new IllegalArgumentException("Signature header is missing headers field");
		if(!headers.contains("(request-target)"))
			throw new IllegalArgumentException("(request-target) is not in signed headers");
		if(!headers.contains("date"))
			throw new IllegalArgumentException("date is not in signed headers");
		if(!headers.contains("host"))
			throw new IllegalArgumentException("host is not in signed headers");

		SimpleDateFormat dateFormat=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		long unixtime=dateFormat.parse(req.headers("date")).getTime();
		long now=System.currentTimeMillis();
		long diff=now-unixtime;
		if(diff>30000L)
			throw new IllegalArgumentException("Date is too far in the future (difference: "+diff+"ms)");
		if(diff<-30000L)
			throw new IllegalArgumentException("Date is too far in the past (difference: "+diff+"ms)");

		URI userID=Utils.userIdFromKeyId(URI.create(keyId));
		ForeignUser user;
		if(userHint.activityPubID.equals(userID))
			user=userHint;
		else
			user=(ForeignUser)UserStorage.getUserByActivityPubID(userID);
		if(user==null)
			throw new IllegalArgumentException("Request signed by unknown user: "+userID);

		ArrayList<String> sigParts=new ArrayList<>();
		for(String header:headers){
			String value;
			if(header.equals("(request-target)")){
				value=req.requestMethod().toLowerCase()+" "+req.pathInfo();
			}else{
				value=req.headers(header);
			}
			sigParts.add(header+": "+value);
		}
		String sigStr=String.join("\n", sigParts);
		Signature sig=Signature.getInstance("SHA256withRSA");
		sig.initVerify(user.publicKey);
		sig.update(sigStr.getBytes(StandardCharsets.UTF_8));
		if(!sig.verify(signature)){
			System.out.println("Failed sig: "+sigHeader);
			System.out.println("Failed sig string: '"+sigStr+"'");
			throw new IllegalArgumentException("Signature failed to verify");
		}
		return user;
	}

	private static boolean verifyHttpDigest(String digestHeader, byte[] bodyData){
		String[] parts=digestHeader.split(",");
		for(String part:parts){
			String[] kv=part.split("=", 2);
			if(kv[0].equalsIgnoreCase("sha-256")){
				byte[] hash=Base64.getDecoder().decode(kv[1]);
				try{
					MessageDigest md=MessageDigest.getInstance("SHA-256");
					md.update(bodyData);
					byte[] myHash=md.digest();
					return Arrays.equals(hash, myHash);
				}catch(Exception x){
					return false;
				}
			}
		}
		return true;
	}

	private static class ActivityTypeHandlerRecord<A extends Actor, T extends Activity, N extends Activity, NN extends Activity, O extends ActivityPubObject>{
		@NotNull
		final Class<A> actorClass;
		@NotNull
		final Class<T> activityClass;
		@Nullable
		final Class<N> nestedActivityClass;
		@Nullable
		final Class<NN> doublyNestedActivityClass;
		@NotNull
		final Class<O> objectClass;
		@NotNull
		final ActivityTypeHandler<A, T, O> handler;

		public ActivityTypeHandlerRecord(@NotNull Class<A> actorClass, @NotNull Class<T> activityClass, @Nullable Class<N> nestedActivityClass, @Nullable Class<NN> doublyNestedActivityClass, @NotNull Class<O> objectClass, @NotNull ActivityTypeHandler<A, T, O> handler){
			this.actorClass=actorClass;
			this.activityClass=activityClass;
			this.nestedActivityClass=nestedActivityClass;
			this.doublyNestedActivityClass=doublyNestedActivityClass;
			this.objectClass=objectClass;
			this.handler=handler;
		}
	}
}
