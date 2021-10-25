package smithereen.routes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import smithereen.BuildInfo;
import smithereen.Config;
import smithereen.ObjectLinkResolver;
import smithereen.Utils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityPubCache;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.DoublyNestedActivityTypeHandler;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.handlers.AcceptFollowGroupHandler;
import smithereen.activitypub.handlers.AcceptFollowPersonHandler;
import smithereen.activitypub.handlers.AddGroupHandler;
import smithereen.activitypub.handlers.AddNoteHandler;
import smithereen.activitypub.handlers.AddPersonHandler;
import smithereen.activitypub.handlers.AnnounceNoteHandler;
import smithereen.activitypub.handlers.CreateNoteHandler;
import smithereen.activitypub.handlers.DeleteNoteHandler;
import smithereen.activitypub.handlers.DeletePersonHandler;
import smithereen.activitypub.handlers.FollowGroupHandler;
import smithereen.activitypub.handlers.FollowPersonHandler;
import smithereen.activitypub.handlers.GroupBlockPersonHandler;
import smithereen.activitypub.handlers.GroupUndoBlockPersonHandler;
import smithereen.activitypub.handlers.LeaveGroupHandler;
import smithereen.activitypub.handlers.LikeNoteHandler;
import smithereen.activitypub.handlers.OfferFollowPersonHandler;
import smithereen.activitypub.handlers.PersonBlockPersonHandler;
import smithereen.activitypub.handlers.PersonUndoBlockPersonHandler;
import smithereen.activitypub.handlers.RejectAddNoteHandler;
import smithereen.activitypub.handlers.RejectFollowGroupHandler;
import smithereen.activitypub.handlers.RejectFollowPersonHandler;
import smithereen.activitypub.handlers.RejectOfferFollowPersonHandler;
import smithereen.activitypub.handlers.RemoveGroupHandler;
import smithereen.activitypub.handlers.RemovePersonHandler;
import smithereen.activitypub.handlers.UndoAcceptFollowGroupHandler;
import smithereen.activitypub.handlers.UndoAcceptFollowPersonHandler;
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
import smithereen.activitypub.objects.ForeignActor;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.ServiceActor;
import smithereen.activitypub.objects.Tombstone;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Add;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.activitypub.objects.activities.Block;
import smithereen.activitypub.objects.activities.Create;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Leave;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Offer;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.activitypub.objects.activities.Update;
import smithereen.data.Account;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.Group;
import smithereen.data.ListAndTotal;
import smithereen.data.NodeInfo;
import smithereen.data.Poll;
import smithereen.data.PollOption;
import smithereen.data.PollVote;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
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

import static smithereen.Utils.gson;
import static smithereen.Utils.parseIntOrDefault;
import static smithereen.Utils.parseSignatureHeader;

public class ActivityPubRoutes{

	private static final Logger LOG=LoggerFactory.getLogger(ActivityPubRoutes.class);

	private static ArrayList<ActivityTypeHandlerRecord<?, ?, ?, ?, ?>> typeHandlers=new ArrayList<>();

	public static void registerActivityHandlers(){
		registerActivityHandler(ForeignUser.class, Create.class, Post.class, new CreateNoteHandler());
		registerActivityHandler(ForeignUser.class, Like.class, Post.class, new LikeNoteHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Like.class, Post.class, new UndoLikeNoteHandler());
		registerActivityHandler(ForeignUser.class, Announce.class, Post.class, new AnnounceNoteHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Announce.class, Post.class, new UndoAnnounceNoteHandler());
		registerActivityHandler(ForeignUser.class, Update.class, Post.class, new UpdateNoteHandler());
		registerActivityHandler(ForeignUser.class, Delete.class, Post.class, new DeleteNoteHandler());
		registerActivityHandler(Actor.class, Reject.class, Add.class, Post.class, new RejectAddNoteHandler());

		registerActivityHandler(ForeignUser.class, Follow.class, User.class, new FollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Follow.class, User.class, new UndoFollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Accept.class, Follow.class, User.class, new AcceptFollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Reject.class, Follow.class, User.class, new RejectFollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Offer.class, Follow.class, User.class, new OfferFollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Accept.class, Follow.class, User.class, new UndoAcceptFollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Reject.class, Offer.class, Follow.class, User.class, new RejectOfferFollowPersonHandler());
		registerActivityHandler(ForeignUser.class, Update.class, ForeignUser.class, new UpdatePersonHandler());
		registerActivityHandler(ForeignUser.class, Delete.class, ForeignUser.class, new DeletePersonHandler());
		registerActivityHandler(ForeignUser.class, Block.class, User.class, new PersonBlockPersonHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Block.class, User.class, new PersonUndoBlockPersonHandler());
		registerActivityHandler(ForeignUser.class, Add.class, User.class, new AddPersonHandler());
		registerActivityHandler(ForeignUser.class, Remove.class, User.class, new RemovePersonHandler());
		registerActivityHandler(ForeignUser.class, Add.class, Group.class, new AddGroupHandler());
		registerActivityHandler(ForeignUser.class, Remove.class, Group.class, new RemoveGroupHandler());

		registerActivityHandler(ForeignGroup.class, Update.class, ForeignGroup.class, new UpdateGroupHandler());
		registerActivityHandler(ForeignUser.class, Follow.class, Group.class, new FollowGroupHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Follow.class, Group.class, new UndoFollowGroupHandler());
		registerActivityHandler(ForeignGroup.class, Undo.class, Accept.class, Follow.class, Group.class, new UndoAcceptFollowGroupHandler());
		registerActivityHandler(ForeignUser.class, Leave.class, Group.class, new LeaveGroupHandler());
		registerActivityHandler(ForeignGroup.class, Accept.class, Follow.class, ForeignGroup.class, new AcceptFollowGroupHandler());
		registerActivityHandler(ForeignGroup.class, Reject.class, Follow.class, ForeignGroup.class, new RejectFollowGroupHandler());
		registerActivityHandler(ForeignGroup.class, Block.class, User.class, new GroupBlockPersonHandler());
		registerActivityHandler(ForeignGroup.class, Undo.class, Block.class, User.class, new GroupUndoBlockPersonHandler());

		registerActivityHandler(Actor.class, Add.class, Post.class, new AddNoteHandler());
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
			return actor;
		}
		throw new ObjectNotFoundException();
	}

	public static Object groupActor(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		Group group;
		if(username!=null)
			group=GroupStorage.getByUsername(username);
		else
			group=GroupStorage.getById(Utils.parseIntOrDefault(req.params(":id"), 0));
		if(group!=null && !(group instanceof ForeignGroup)){
			group.adminsForActivityPub=GroupStorage.getGroupAdmins(group.id);
			resp.type(ActivityPub.CONTENT_TYPE);
			return group;
		}
		throw new ObjectNotFoundException();
	}

	public static Object post(Request req, Response resp) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		Post post=PostStorage.getPostOrThrow(postID, true);
		resp.type(ActivityPub.CONTENT_TYPE);
		return post;
	}

	public static ActivityPubCollectionPageResponse postReplies(Request req, Response resp, int offset, int count) throws SQLException{
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		Post post=PostStorage.getPostOrThrow(postID, true);
		int[] _total={0};
		List<URI> ids=PostStorage.getImmediateReplyActivityPubIDs(post.getReplyKeyForReplies(), offset, count, _total);
		return ActivityPubCollectionPageResponse.forLinks(ids, _total[0]);
	}

	public static ActivityPubCollectionPageResponse postLikes(Request req, Response resp, int offset, int count) throws SQLException{
		Post post=PostStorage.getPostOrThrow(parseIntOrDefault(req.params(":postID"), 0), true);
		ListAndTotal<Like> likes=LikeStorage.getLikes(post.id, post.activityPubID, Like.ObjectType.POST, offset, count);
		return ActivityPubCollectionPageResponse.forObjects(likes).ordered();
	}

	public static ActivityPubCollectionPageResponse pollVoters(Request req, Response resp, int offset, int count) throws SQLException{
		Poll poll=PostStorage.getPoll(parseIntOrDefault(req.params(":pollID"), 0), null);
		int optionID=parseIntOrDefault(req.params(":optionID"), 0);

		if(poll==null)
			throw new ObjectNotFoundException();
		if(poll.activityPubID!=null && !Config.isLocal(poll.activityPubID))
			throw new ObjectNotFoundException();
		if(poll.anonymous)
			throw new UserActionNotAllowedException();

		PollOption option=null;
		for(PollOption opt:poll.options){
			if(opt.id==optionID){
				option=opt;
				break;
			}
		}
		if(option==null)
			throw new ObjectNotFoundException();

		List<PollVote> votes=PostStorage.getPollOptionVotersApIDs(optionID, offset, count).stream().map(id->{
			PollVote vote=new PollVote();
			vote.attributedTo=id;
			return vote;
		}).collect(Collectors.toList());
		return ActivityPubCollectionPageResponse.forObjects(votes, option.getNumVotes()).ordered();
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
		Group group=GroupStorage.getById(id);
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
				return collection;
			}
		}catch(URISyntaxException ignore){}
		resp.type(ActivityPub.CONTENT_TYPE);
		return page;
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
		Group group=GroupStorage.getById(id);
		if(group==null || group instanceof ForeignGroup)
			throw new ObjectNotFoundException();
		return actorWall(req, resp, offset, count, group);
	}

	private static ActivityPubCollectionPageResponse actorWall(Request req, Response resp, int offset, int count, Actor actor) throws SQLException{
		int[] total={0};
		List<URI> posts=PostStorage.getWallPostActivityPubIDs(actor.getLocalID(), actor instanceof Group, offset, count, total);
		return ActivityPubCollectionPageResponse.forLinks(posts, total[0]);
	}

	public static ActivityPubCollectionPageResponse userFriends(Request req, Response resp, int offset, int count) throws SQLException{
		int id=parseIntOrDefault(req.params(":id"), 0);
		User user=UserStorage.getById(id);
		if(user==null || user instanceof ForeignUser)
			throw new ObjectNotFoundException();
		return ActivityPubCollectionPageResponse.forLinks(UserStorage.getActivityPubFriendList(id, offset, count), UserStorage.getUserFriendsCount(id));
	}

	public static ActivityPubCollectionPageResponse userGroups(Request req, Response resp, int offset, int count) throws SQLException{
		int id=parseIntOrDefault(req.params(":id"), 0);
		User user=UserStorage.getById(id);
		if(user==null || user instanceof ForeignUser)
			throw new ObjectNotFoundException();
		return ActivityPubCollectionPageResponse.forLinks(GroupStorage.getUserGroupIDs(id, offset, count));
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
			remoteObj=ObjectLinkResolver.resolve(uri, ActivityPubObject.class, true, false, true);
			if(remoteObj==null){
				return "Error fetching remote object";
			}
			if(remoteObj.activityPubID.getHost()==null || uri.getHost()==null || !remoteObj.activityPubID.getHost().equals(uri.getHost())){
				return "Object ID host doesn't match URI host";
			}
		}catch(IOException|JsonParseException|URISyntaxException x){
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
				return new RenderedTemplateResponse("remote_follow", req).with("user", foreignUser).with("title", Config.serverDisplayName);
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
				Post post=(Post) remoteObj;
				Post topLevelPost;
				Utils.loadAndPreprocessRemotePostMentions(post);
				if(post.inReplyTo!=null){
					Post parent=PostStorage.getPostByID(post.inReplyTo);
					if(parent==null){
						List<Post> thread=ActivityPubWorker.getInstance().fetchReplyThread(post).get(30, TimeUnit.SECONDS);
						topLevelPost=thread.get(0);
					}else{
						post.setParent(parent);
						post.storeDependencies();
						PostStorage.putForeignWallPost(post);
						topLevelPost=PostStorage.getPostByID(parent.getReplyChainElement(0), false);
						if(topLevelPost==null)
							throw new BadRequestException("Top-level post is not available");
					}
				}else{
					topLevelPost=post;
					post.storeDependencies();
					PostStorage.putForeignWallPost(post);
				}
				ActivityPubWorker.getInstance().fetchAllReplies(topLevelPost);
				resp.redirect("/posts/"+topLevelPost.id);
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
		String ver=req.pathInfo().substring(req.pathInfo().length()-3);
		resp.type("application/json; profile=\"http://nodeinfo.diaspora.software/ns/schema/"+ver+"#\"");

		NodeInfo nodeInfo=new NodeInfo();
		nodeInfo.version=ver;
		nodeInfo.protocols=List.of("activitypub");
		nodeInfo.openRegistrations=Config.signupMode==Config.SignupMode.OPEN;
		nodeInfo.software=new NodeInfo.Software();
		nodeInfo.software.name="smithereen";
		nodeInfo.software.version=BuildInfo.VERSION;
		if(ver.equals("2.1")){
			nodeInfo.software.repository="https://github.com/grishka/Smithereen";
			nodeInfo.software.homepage="https://smithereen.software";
		}
		nodeInfo.usage=new NodeInfo.Usage();
		nodeInfo.usage.localPosts=PostStorage.getLocalPostCount(false);
		nodeInfo.usage.localComments=PostStorage.getLocalPostCount(true);
		nodeInfo.usage.users=new NodeInfo.Usage.Users();
		nodeInfo.usage.users.total=UserStorage.getLocalUserCount();
		nodeInfo.usage.users.activeMonth=UserStorage.getActiveLocalUserCount(30*24*60*60*1000L);
		nodeInfo.usage.users.activeHalfyear=UserStorage.getActiveLocalUserCount(180*24*60*60*1000L);
		nodeInfo.metadata=Map.of();

		return gson.toJson(nodeInfo);
	}

	public static Object likeObject(Request req, Response resp) throws SQLException{
		int id=parseIntOrDefault(req.params(":likeID"), 0);
		if(id==0)
			throw new ObjectNotFoundException();
		Like l=LikeStorage.getByID(id);
		if(l==null)
			throw new ObjectNotFoundException();
		resp.type(ActivityPub.CONTENT_TYPE);
		return l;
	}

	public static Object undoLikeObject(Request req, Response resp) throws SQLException{
		int id=parseIntOrDefault(req.params(":likeID"), 0);
		if(id==0)
			throw new ObjectNotFoundException();
		Undo undo=ActivityPubCache.getUndoneLike(id);
		if(undo==null)
			throw new ObjectNotFoundException();
		return undo;
	}

	private static Object inbox(Request req, Response resp, Actor owner) throws SQLException{
		if(req.headers("digest")!=null){
			if(!verifyHttpDigest(req.headers("digest"), req.bodyAsBytes())){
				throw new BadRequestException("Digest verification failed");
			}
		}
		String body=req.body();
		LOG.info("Incoming activity: {}", body);
		JsonObject rawActivity=JsonParser.parseString(body).getAsJsonObject();
		JsonObject obj=JLDProcessor.convertToLocalContext(rawActivity);

		Activity activity;
		ActivityPubObject o;
		try{
			o=ActivityPubObject.parse(obj);
		}catch(Exception e){
			throw new BadRequestException("Failed to parse object: "+e.toString(), e);
		}
		if(o instanceof Activity)
			activity=(Activity)o;
		else if(o!=null)
			throw new BadRequestException("Unsupported object type '"+o.getType()+"'");
		else
			throw new BadRequestException("Unsupported object type");

		if(Config.isLocal(activity.actor.link))
			throw new BadRequestException("User domain must be different from this server");

		Actor actor;
		boolean canUpdate=true;
		// special case: when users delete themselves but are not in local database, ignore that
		if(activity instanceof Delete && activity.actor.link.equals(activity.object.link)){
			try{
				actor=ObjectLinkResolver.resolve(activity.actor.link, Actor.class, false, false, false);
			}catch(ObjectNotFoundException x){
				return "";
			}
			canUpdate=false;
		}else{
			actor=ObjectLinkResolver.resolve(activity.actor.link, Actor.class, true, true, false);
		}
		if(!(actor instanceof ForeignActor))
			throw new BadRequestException("Actor is local");
		if(((ForeignActor) actor).needUpdate() && canUpdate){
			try{
				actor=ObjectLinkResolver.resolve(activity.actor.link, Actor.class, true, true, true);
			}catch(ObjectNotFoundException x){
				LOG.warn("Exception while refreshing remote actor", x);
			}
		}

		Actor httpSigOwner;
		try{
			httpSigOwner=verifyHttpSignature(req, actor);
		}catch(Exception x){
			LOG.warn("Exception while verifying HTTP signature", x);
			throw new BadRequestException(x);
		}

		// if the activity has an LD-signature, verify that and allow any (cached) user to sign the HTTP signature
		// if it does not, the owner of the HTTP signature must match the actor
		boolean hasValidLDSignature=false;
		if(rawActivity.has("signature")){
			JsonObject sig=rawActivity.getAsJsonObject("signature");
			try{
				URI keyID=URI.create(sig.get("creator").getAsString());
				URI userID=Utils.userIdFromKeyId(keyID);
				if(!userID.equals(actor.activityPubID)){
					throw new BadRequestException("LD-signature creator is not activity actor");
				}
				if(!LinkedDataSignatures.verify(rawActivity, actor.publicKey)){
					throw new BadRequestException("LD-signature verification failed");
				}
				LOG.info("verified LD signature by {}", userID);
				hasValidLDSignature=true;
			}catch(Exception x){
				LOG.warn("Exception while verifying LD-signature", x);
				throw new BadRequestException(x);
			}
		}else{
			if(!actor.equals(httpSigOwner)){
				throw new BadRequestException("In the absence of an LD-signature, HTTP signature must be made by the activity actor");
			}
			LOG.info("verified HTTP signature by {}", httpSigOwner.activityPubID);
		}
		// parse again to make sure the actor is set everywhere
		try{
			ActivityPubObject _o=ActivityPubObject.parse(obj);
			if(_o instanceof Activity)
				activity=(Activity) _o;
		}catch(Exception ignore){}

		ActivityHandlerContext context=new ActivityHandlerContext(body, hasValidLDSignature ? actor : null, httpSigOwner);

		try{
			ActivityPubObject aobj;
			if(activity.object.object!=null){
				aobj=activity.object.object;
				// special case: Mastodon sends Delete{Tombstone} for post deletions
				if(aobj instanceof Tombstone){
					try{
						aobj=ObjectLinkResolver.resolve(aobj.activityPubID);
					}catch(ObjectNotFoundException x){
						LOG.warn("Activity object not found for {}: {}", getActivityType(activity), aobj.activityPubID);
						// Fail silently. We didn't have that object anyway, there's nothing to delete.
						return "";
					}
				}
			}else{
				if(activity instanceof Like || activity instanceof Delete){
					try{
						aobj=ObjectLinkResolver.resolve(activity.object.link, ActivityPubObject.class, false, false, false);
					}catch(ObjectNotFoundException x){
						// Fail silently. Pleroma sends all likes to followers, including for objects they may not have.
						LOG.info("Activity object not known for {}: {}", activity.getType(), activity.object.link);
						return "";
					}
				}else{
					// special case: fetch the object of Announce{Note} or Add{...}
					aobj=ObjectLinkResolver.resolve(activity.object.link, ActivityPubObject.class, activity instanceof Announce || activity instanceof Add, false, false);
				}
			}
			for(ActivityTypeHandlerRecord r : typeHandlers){
				if(r.actorClass.isInstance(actor)){
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
									LOG.info("Found match: {}", r.handler.getClass().getName());
									((DoublyNestedActivityTypeHandler)r.handler).handle(context, actor, activity, nestedActivity, doublyNestedActivity, doublyNestedObject);
									return "";
								}
							}else if(r.objectClass.isInstance(nestedObject)){
								LOG.info("Found match: {}", r.handler.getClass().getName());
								((NestedActivityTypeHandler)r.handler).handle(context, actor, activity, nestedActivity, nestedObject);
								return "";
							}
						}else if(r.objectClass.isInstance(aobj)){
							LOG.info("Found match: {}", r.handler.getClass().getName());
							r.handler.handle(context, actor, activity, aobj);
							return "";
						}
					}
				}
			}
		}catch(Exception x){
			LOG.warn("Exception while processing an incoming activity", x);
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
		Group group=GroupStorage.getById(id);
		if(group==null || group instanceof ForeignGroup){
			throw new ObjectNotFoundException();
		}
		int[] _total={0};
		List<URI> followers=GroupStorage.getGroupMemberURIs(group.id, false, offset, count, _total);
		return ActivityPubCollectionPageResponse.forLinks(followers, _total[0]);
	}

	public static Object serviceActor(Request req, Response resp) throws SQLException{
		resp.type(ActivityPub.CONTENT_TYPE);
		return ServiceActor.getInstance().asRootActivityPubObject();
	}



	private static Actor verifyHttpSignature(Request req, Actor userHint) throws ParseException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, SQLException{
		String sigHeader=req.headers("Signature");
		if(sigHeader==null)
			throw new BadRequestException("Request is missing Signature header");
		List<Map<String, String>> values=parseSignatureHeader(sigHeader);
		if(values.isEmpty())
			throw new BadRequestException("Signature header has invalid format");
		Map<String, String> supportedSig=null;
		for(Map<String, String> sig:values){
			if("rsa-sha256".equalsIgnoreCase(sig.get("algorithm"))){
				supportedSig=sig;
				break;
			}
		}
		if(supportedSig==null)
			throw new BadRequestException("Unsupported signature algorithm \""+values.get(0).get("algorithm")+"\", expected \"rsa-sha256\"");

		if(!supportedSig.containsKey("keyId"))
			throw new BadRequestException("Signature header is missing keyId field");
		if(!supportedSig.containsKey("signature"))
			throw new BadRequestException("Signature header is missing signature field");
		if(!supportedSig.containsKey("headers"))
			throw new BadRequestException("Signature header is missing headers field");

		String keyId=supportedSig.get("keyId");
		byte[] signature=Base64.getDecoder().decode(supportedSig.get("signature"));
		List<String> headers=Arrays.asList(supportedSig.get("headers").split(" "));

		if(!headers.contains("(request-target)"))
			throw new BadRequestException("(request-target) is not in signed headers");
		if(!headers.contains("date"))
			throw new BadRequestException("date is not in signed headers");
		if(!headers.contains("host"))
			throw new BadRequestException("host is not in signed headers");

		SimpleDateFormat dateFormat=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		long unixtime=dateFormat.parse(req.headers("date")).getTime();
		long now=System.currentTimeMillis();
		long diff=now-unixtime;
		if(diff>30000L)
			throw new BadRequestException("Date is too far in the future (difference: "+diff+"ms)");
		if(diff<-30000L)
			throw new BadRequestException("Date is too far in the past (difference: "+diff+"ms)");

		URI userID=Utils.userIdFromKeyId(URI.create(keyId));
		Actor user;
		if(userHint.activityPubID.equals(userID))
			user=userHint;
		else
			user=ObjectLinkResolver.resolve(userID, Actor.class, false, true, false);
		if(user==null)
			throw new BadRequestException("Request signed by unknown user: "+userID);

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
			LOG.info("Failed signature header: {}", sigHeader);
			LOG.info("Failed signature string: '{}'", sigStr);
			throw new BadRequestException("Signature failed to verify");
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
