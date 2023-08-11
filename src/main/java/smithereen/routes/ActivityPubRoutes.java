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
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.BuildInfo;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityPubCache;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.DoublyNestedActivityTypeHandler;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.handlers.AcceptFollowGroupHandler;
import smithereen.activitypub.handlers.AcceptFollowPersonHandler;
import smithereen.activitypub.handlers.AddGroupHandler;
import smithereen.activitypub.handlers.AddNoteHandler;
import smithereen.activitypub.handlers.AnnounceNoteHandler;
import smithereen.activitypub.handlers.CreateNoteHandler;
import smithereen.activitypub.handlers.DeleteNoteHandler;
import smithereen.activitypub.handlers.DeletePersonHandler;
import smithereen.activitypub.handlers.FlagHandler;
import smithereen.activitypub.handlers.FollowGroupHandler;
import smithereen.activitypub.handlers.FollowPersonHandler;
import smithereen.activitypub.handlers.GroupAddPersonHandler;
import smithereen.activitypub.handlers.GroupBlockPersonHandler;
import smithereen.activitypub.handlers.GroupRemovePersonHandler;
import smithereen.activitypub.handlers.GroupUndoBlockPersonHandler;
import smithereen.activitypub.handlers.InviteGroupHandler;
import smithereen.activitypub.handlers.LeaveGroupHandler;
import smithereen.activitypub.handlers.LikeNoteHandler;
import smithereen.activitypub.handlers.OfferFollowPersonHandler;
import smithereen.activitypub.handlers.PersonAddPersonHandler;
import smithereen.activitypub.handlers.PersonBlockPersonHandler;
import smithereen.activitypub.handlers.PersonRemovePersonHandler;
import smithereen.activitypub.handlers.PersonUndoBlockPersonHandler;
import smithereen.activitypub.handlers.RejectAddNoteHandler;
import smithereen.activitypub.handlers.RejectFollowGroupHandler;
import smithereen.activitypub.handlers.RejectFollowPersonHandler;
import smithereen.activitypub.handlers.RejectInviteGroupHandler;
import smithereen.activitypub.handlers.RejectOfferFollowPersonHandler;
import smithereen.activitypub.handlers.RemoveGroupHandler;
import smithereen.activitypub.handlers.UndoAcceptFollowGroupHandler;
import smithereen.activitypub.handlers.UndoAcceptFollowPersonHandler;
import smithereen.activitypub.handlers.UndoAnnounceNoteHandler;
import smithereen.activitypub.handlers.UndoFollowGroupHandler;
import smithereen.activitypub.handlers.UndoFollowPersonHandler;
import smithereen.activitypub.handlers.UndoInviteGroupHandler;
import smithereen.activitypub.handlers.UndoLikeNoteHandler;
import smithereen.activitypub.handlers.UpdateGroupHandler;
import smithereen.activitypub.handlers.UpdateNoteHandler;
import smithereen.activitypub.handlers.UpdatePersonHandler;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.CollectionQueryResult;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.ServiceActor;
import smithereen.activitypub.objects.Tombstone;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Add;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.activitypub.objects.activities.Block;
import smithereen.activitypub.objects.activities.Create;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.activitypub.objects.activities.Flag;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Invite;
import smithereen.activitypub.objects.activities.Leave;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Offer;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.activitypub.objects.activities.Update;
import smithereen.data.Account;
import smithereen.data.FederationRestriction;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.FriendshipStatus;
import smithereen.data.Group;
import smithereen.data.NodeInfo;
import smithereen.data.OwnedContentObject;
import smithereen.data.OwnerAndAuthor;
import smithereen.data.PaginatedList;
import smithereen.data.Poll;
import smithereen.data.PollOption;
import smithereen.data.PollVote;
import smithereen.data.Post;
import smithereen.data.Server;
import smithereen.data.StatsType;
import smithereen.data.UriBuilder;
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

import static smithereen.Utils.*;

public class ActivityPubRoutes{

	private static final Logger LOG=LoggerFactory.getLogger(ActivityPubRoutes.class);

	/**
	 * Handlers that match on (actor type, activity type, object type) + up to 2 levels of activity nesting
 	 */
	private static final ArrayList<ActivityTypeHandlerRecord<?, ?, ?, ?, ?>> typeHandlers=new ArrayList<>();

	/**
	 * Handlers that match on the activity type only, disregarding actor/object types
	 */
	private static final ArrayList<ActivityTypeOnlyHandlerRecord<?>> typeOnlyHandlers=new ArrayList<>();

	public static void registerActivityHandlers(){
		registerActivityHandler(ForeignUser.class, Create.class, NoteOrQuestion.class, new CreateNoteHandler());
		registerActivityHandler(ForeignUser.class, Like.class, NoteOrQuestion.class, new LikeNoteHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Like.class, NoteOrQuestion.class, new UndoLikeNoteHandler());
		registerActivityHandler(ForeignUser.class, Announce.class, NoteOrQuestion.class, new AnnounceNoteHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Announce.class, NoteOrQuestion.class, new UndoAnnounceNoteHandler());
		registerActivityHandler(ForeignUser.class, Update.class, NoteOrQuestion.class, new UpdateNoteHandler());
		registerActivityHandler(ForeignUser.class, Delete.class, NoteOrQuestion.class, new DeleteNoteHandler());
		registerActivityHandler(Actor.class, Reject.class, Add.class, NoteOrQuestion.class, new RejectAddNoteHandler());

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
		registerActivityHandler(ForeignUser.class, Add.class, User.class, new PersonAddPersonHandler());
		registerActivityHandler(ForeignUser.class, Remove.class, User.class, new PersonRemovePersonHandler());
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
		registerActivityHandler(ForeignUser.class, Invite.class, Group.class, new InviteGroupHandler());
		registerActivityHandler(ForeignUser.class, Reject.class, Invite.class, Group.class, new RejectInviteGroupHandler());
		registerActivityHandler(ForeignGroup.class, Undo.class, Invite.class, ForeignGroup.class, new UndoInviteGroupHandler());
		registerActivityHandler(ForeignGroup.class, Add.class, User.class, new GroupAddPersonHandler());
		registerActivityHandler(ForeignGroup.class, Remove.class, User.class, new GroupRemovePersonHandler());

		registerActivityHandler(Actor.class, Add.class, NoteOrQuestion.class, new AddNoteHandler());

		registerActivityHandler(Flag.class, new FlagHandler());
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

	private static <T extends Activity> void registerActivityHandler(@NotNull Class<T> activityClass, @NotNull ActivityTypeHandler<?, T, ?> handler){
		typeOnlyHandlers.add(new ActivityTypeOnlyHandlerRecord<>(activityClass, handler));
	}

	public static Object userActor(Request req, Response resp) throws SQLException{
		String username=req.params(":username");
		Actor actor;
		if(username!=null){
			actor=UserStorage.getByUsername(username);
			if(actor==null){
				return groupActor(req, resp);
			}
		}else{
			actor=UserStorage.getById(Utils.parseIntOrDefault(req.params(":id"), 0));
		}
		if(actor!=null && !(actor instanceof ForeignUser)){
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
			if(group.accessType==Group.AccessType.PRIVATE){
				Actor requester;
				try{
					requester=ActivityPub.verifyHttpSignature(req, null);
				}catch(Exception x){
					throw new UserActionNotAllowedException("This is a private group. Valid HTTP signature of a group member or invitee is required.", x);
				}
				if(!GroupStorage.areThereGroupMembersWithDomain(group.id, requester.domain) && !GroupStorage.areThereGroupInvitationsWithDomain(group.id, requester.domain))
					throw new UserActionNotAllowedException("This is a private group and there are no "+requester.domain+" members or invitees in it.");
			}
			group.adminsForActivityPub=GroupStorage.getGroupAdmins(group.id);
			resp.type(ActivityPub.CONTENT_TYPE);
			return group;
		}
		throw new ObjectNotFoundException();
	}

	public static Object post(Request req, Response resp) throws SQLException{
		ApplicationContext ctx=context(req);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		Post post=PostStorage.getPostByID(postID, true);
		if(post==null || !post.isLocal())
			throw new ObjectNotFoundException();
		ctx.getPrivacyController().enforceContentPrivacyForActivityPub(req, post);
		resp.type(ActivityPub.CONTENT_TYPE);
		return NoteOrQuestion.fromNativePost(post, ctx);
	}

	public static Object postCreateActivity(Request req, Response resp){
		ApplicationContext ctx=context(req);
		int postID=safeParseInt(req.params(":postID"));
		Post post=ctx.getWallController().getLocalPostOrThrow(postID);
		ctx.getPrivacyController().enforceContentPrivacyForActivityPub(req, post);
		OwnerAndAuthor oaa=ctx.getWallController().getContentAuthorAndOwner(post);
		resp.type(ActivityPub.CONTENT_TYPE);

		NoteOrQuestion apPost=NoteOrQuestion.fromNativePost(post, ctx);
		Create create=new Create();
		create.object=new LinkOrObject(apPost);
		create.actor=new LinkOrObject(oaa.author().activityPubID);
		create.to=apPost.to;
		create.cc=apPost.cc;
		create.published=apPost.published;
		create.activityPubID=new UriBuilder(post.getActivityPubID()).appendPath("activityCreate").build();
		return create;
	}

	public static ActivityPubCollectionPageResponse postReplies(Request req, Response resp, int offset, int count) throws SQLException{
		ApplicationContext ctx=context(req);
		int postID=Utils.parseIntOrDefault(req.params(":postID"), 0);
		Post post=PostStorage.getPostOrThrow(postID, true);
		ctx.getPrivacyController().enforceContentPrivacyForActivityPub(req, post);
		int[] _total={0};
		List<URI> ids=PostStorage.getImmediateReplyActivityPubIDs(post.getReplyKeyForReplies(), offset, count, _total);
		return ActivityPubCollectionPageResponse.forLinks(ids, _total[0]);
	}

	public static ActivityPubCollectionPageResponse postLikes(Request req, Response resp, int offset, int count) throws SQLException{
		ApplicationContext ctx=context(req);
		Post post=PostStorage.getPostOrThrow(parseIntOrDefault(req.params(":postID"), 0), true);
		ctx.getPrivacyController().enforceContentPrivacyForActivityPub(req, post);
		PaginatedList<Like> likes=LikeStorage.getLikes(post.id, post.getActivityPubID(), Like.ObjectType.POST, offset, count);
		return ActivityPubCollectionPageResponse.forObjects(likes).ordered();
	}

	public static ActivityPubCollectionPageResponse pollVoters(Request req, Response resp, int offset, int count) throws SQLException{
		ApplicationContext ctx=context(req);
		Poll poll=PostStorage.getPoll(parseIntOrDefault(req.params(":pollID"), 0), null);
		int optionID=parseIntOrDefault(req.params(":optionID"), 0);

		if(poll==null)
			throw new ObjectNotFoundException();
		if(poll.activityPubID!=null && !Config.isLocal(poll.activityPubID))
			throw new ObjectNotFoundException();
		if(poll.anonymous)
			throw new UserActionNotAllowedException();
		ctx.getPrivacyController().enforceContentPrivacyForActivityPub(req, poll);

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
		return ActivityPubCollectionPageResponse.forObjects(votes, option.numVotes).ordered();
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
		ApplicationContext ctx=context(req);
		int id=Utils.parseIntOrDefault(req.params(":id"), 0);
		User user=ctx.getUsersController().getLocalUserOrThrow(id);
		resp.type(ActivityPub.CONTENT_TYPE);
		int _minID=Utils.parseIntOrDefault(req.queryParams("min_id"), -1);
		int _maxID=Utils.parseIntOrDefault(req.queryParams("max_id"), -1);
		int minID=Math.max(0, _minID);
		int maxID=Math.max(0, _maxID);
		int[] _total={0};
		List<Post> posts=PostStorage.getWallPosts(user.id, false, minID, maxID, 0, 25, _total, true);
		int total=_total[0];
		CollectionPage page=new CollectionPage(true);
		page.totalItems=total;
		page.items=new ArrayList<>();
		for(Post post:posts){
			NoteOrQuestion apPost=NoteOrQuestion.fromNativePost(post, ctx);

			Create activity=new Create();
			activity.object=new LinkOrObject(apPost);
			activity.published=apPost.published;
			activity.to=apPost.to;
			activity.cc=apPost.cc;
			activity.actor=new LinkOrObject(apPost.attributedTo);
			activity.activityPubID=new UriBuilder(post.getActivityPubID()).appendPath("/activityCreate").build();
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
		context(req).getPrivacyController().enforceGroupContentAccess(req, group);
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

	public static Object externalInteraction(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
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
			remoteObj=ctx.getObjectLinkResolver().resolve(uri, ActivityPubObject.class, true, false, true);
			if(remoteObj.activityPubID.getHost()==null || uri.getHost()==null || !remoteObj.activityPubID.getHost().equals(uri.getHost())){
				return "Object ID host doesn't match URI host";
			}
		}catch(IOException|JsonParseException|URISyntaxException x){
			return x.getMessage();
		}
		if(remoteObj instanceof ForeignUser foreignUser){
			try{
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
		}else if(remoteObj instanceof ForeignGroup group){
			group.storeDependencies(ctx);
			GroupStorage.putOrUpdateForeignGroup(group);
			resp.redirect(Config.localURI("/"+group.getFullUsername()).toString());
			return "";
		}else if(remoteObj instanceof NoteOrQuestion post){
			try{
				Post topLevelPost;
				if(post.inReplyTo!=null){
					Post parent=PostStorage.getPostByID(post.inReplyTo);
					if(parent==null){
						List<Post> thread=ctx.getActivityPubWorker().fetchReplyThread(post).get(30, TimeUnit.SECONDS);
						topLevelPost=thread.get(0);
					}else{
						Post nativePost=post.asNativePost(ctx);
						ctx.getWallController().loadAndPreprocessRemotePostMentions(nativePost, post);
						PostStorage.putForeignWallPost(nativePost);
						topLevelPost=PostStorage.getPostByID(parent.getReplyChainElement(0), false);
						if(topLevelPost==null)
							throw new BadRequestException("Top-level post is not available");
					}
				}else{
					Post nativePost=post.asNativePost(ctx);
					ctx.getWallController().loadAndPreprocessRemotePostMentions(nativePost, post);
					topLevelPost=nativePost;
					PostStorage.putForeignWallPost(nativePost);
				}
				ctx.getActivityPubWorker().fetchAllReplies(topLevelPost);
				resp.redirect("/posts/"+topLevelPost.id);
				return "";
			}catch(Exception x){
				x.printStackTrace();
				return x.toString();
			}
		}
		return "Referer: "+Utils.sanitizeHTML(ref)+"<hr/>URL: "+Utils.sanitizeHTML(req.queryParams("uri"))+"<hr/>Object:<br/><pre>"+Utils.sanitizeHTML(remoteObj.toString())+"</pre>";
	}

	public static Object remoteFollow(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		String username=req.params(":username");
		User _user=UserStorage.getByUsername(username);
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
				UserStorage.followUser(self.user.id, user.id, false, false);
			}
			Follow follow=new Follow();
			follow.actor=new LinkOrObject(self.user.activityPubID);
			follow.object=new LinkOrObject(user.activityPubID);
			follow.activityPubID=new URI(self.user.activityPubID.getScheme(), self.user.activityPubID.getSchemeSpecificPart(), "follow"+user.id);
			ActivityPub.postActivity(user.inbox, follow, self.user, ctx, false);
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
				ActivityPub.postActivity(user.inbox, offer, self.user, ctx, false);
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
		ApplicationContext ctx=context(req);
		int id=parseIntOrDefault(req.params(":likeID"), 0);
		if(id==0)
			throw new ObjectNotFoundException();
		Like l=LikeStorage.getByID(id);
		if(l==null)
			throw new ObjectNotFoundException();
		Object likedObject=context(req).getObjectLinkResolver().resolveNative(l.object.link, Object.class, false, false, false, (JsonObject) null, true);
		ctx.getPrivacyController().enforceContentPrivacyForActivityPub(req, (OwnedContentObject) likedObject);
		resp.type(ActivityPub.CONTENT_TYPE);
		return l;
	}

	public static Object undoLikeObject(Request req, Response resp){
		ApplicationContext ctx=context(req);
		int id=parseIntOrDefault(req.params(":likeID"), 0);
		if(id==0)
			throw new ObjectNotFoundException();
		Undo undo=ActivityPubCache.getUndoneLike(id);
		if(undo==null)
			throw new ObjectNotFoundException();
		Object likedObject=context(req).getObjectLinkResolver().resolveNative(((Like)undo.object.object).object.link, Object.class, false, false, false, (JsonObject) null, true);
		ctx.getPrivacyController().enforceContentPrivacyForActivityPub(req, (OwnedContentObject) likedObject);
		resp.type(ActivityPub.CONTENT_TYPE);
		return undo;
	}

	private static Object inbox(Request req, Response resp, Actor owner) throws SQLException{
		ApplicationContext ctx=context(req);
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
			throw new BadRequestException("Failed to parse object: "+e, e);
		}
		if(o instanceof Activity act)
			activity=act;
		else if(o!=null)
			throw new BadRequestException("Unsupported object type '"+o.getType()+"'");
		else
			throw new BadRequestException("Unsupported object type");

		if(Config.isLocal(activity.actor.link))
			throw new BadRequestException("User domain must be different from this server");

		// Enforce federation blocks before doing something potentially expensive like fetching linked objects.
		// This may help if the server in question was blocked because of DoS concerns.
		Server server=ctx.getModerationController().getOrAddServer(activity.actor.link.getAuthority());
		FederationRestriction restriction=server.restriction();
		if(restriction!=null){
			if(restriction.type==FederationRestriction.RestrictionType.SUSPENSION){
				resp.status(403);
				return "Federation with "+server.host()+" is blocked by this server's policies";
			}
		}

		Actor actor;
		boolean canUpdate=true;
		// special case: when users delete themselves but are not in local database, ignore that
		if(activity instanceof Delete && activity.actor.link.equals(activity.object.link)){
			try{
				actor=ctx.getObjectLinkResolver().resolve(activity.actor.link, Actor.class, false, false, false);
			}catch(ObjectNotFoundException x){
				return "";
			}
			canUpdate=false;
		}else{
			actor=ctx.getObjectLinkResolver().resolve(activity.actor.link, Actor.class, true, true, false);
		}
		if(!(actor instanceof ForeignActor fa))
			throw new BadRequestException("Actor is local");
		if(fa.needUpdate() && canUpdate){
			try{
				actor=ctx.getObjectLinkResolver().resolve(activity.actor.link, Actor.class, true, true, true);
			}catch(ObjectNotFoundException x){
				LOG.warn("Exception while refreshing remote actor", x);
			}
		}

		Actor httpSigOwner;
		try{
			httpSigOwner=ActivityPub.verifyHttpSignature(req, actor);
		}catch(Exception x){
			LOG.warn("Exception while verifying HTTP signature", x);
			throw new UserActionNotAllowedException(x);
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
				LOG.info("Exception while verifying LD-signature", x);
			}
		}
		if(!hasValidLDSignature){
			if(!actor.equals(httpSigOwner)){
				throw new BadRequestException("In the absence of a valid LD-signature, HTTP signature must be made by the activity actor");
			}
			LOG.info("verified HTTP signature by {}", httpSigOwner.activityPubID);
		}
		// parse again to make sure the actor is set everywhere
		try{
			ActivityPubObject _o=ActivityPubObject.parse(obj);
			if(_o instanceof Activity act1)
				activity=act1;
		}catch(Exception ignore){}

		ActivityHandlerContext context=new ActivityHandlerContext(ctx, body, hasValidLDSignature ? actor : null, httpSigOwner);

		ctx.getStatsController().incrementDaily(StatsType.SERVER_ACTIVITIES_RECEIVED, server.id());
		if(server.getAvailability()!=Server.Availability.UP){
			ctx.getModerationController().resetServerAvailability(server);
		}

		try{
			// First, try matching by activity type only
			for(ActivityTypeOnlyHandlerRecord r:typeOnlyHandlers){
				if(r.activityClass.isInstance(activity)){
					r.handler.handle(context, actor, activity, null);
					return "";
				}
			}

			// Match more thoroughly
			ActivityPubObject aobj;
			if(activity.object.object!=null){
				aobj=activity.object.object;
				// special case: Mastodon sends Delete{Tombstone} for post deletions
				if(aobj instanceof Tombstone){
					try{
						aobj=ctx.getObjectLinkResolver().resolve(aobj.activityPubID);
					}catch(ObjectNotFoundException x){
						LOG.warn("Activity object not found for {}: {}", getActivityType(activity), aobj.activityPubID);
						// Fail silently. We didn't have that object anyway, there's nothing to delete.
						return "";
					}
				}
			}else{
				if(activity instanceof Like || activity instanceof Delete){
					try{
						aobj=ctx.getObjectLinkResolver().resolve(activity.object.link, ActivityPubObject.class, false, false, false);
					}catch(ObjectNotFoundException x){
						// Fail silently. Pleroma sends all likes to followers, including for objects they may not have.
						LOG.info("Activity object not known for {}: {}", activity.getType(), activity.object.link);
						return "";
					}
				}else{
					// special case: fetch the object of Announce{Note}, Add{...}, or Invite{Group}
					Actor collectionOwner;
					if(activity instanceof Add add && add.target!=null && add.target.object instanceof ActivityPubCollection target && target.attributedTo!=null){
						collectionOwner=ctx.getObjectLinkResolver().resolve(target.attributedTo, Actor.class, false, false, false);
					}else{
						collectionOwner=null;
					}
					aobj=ctx.getObjectLinkResolver().resolve(activity.object.link, ActivityPubObject.class, activity instanceof Announce || activity instanceof Add || activity instanceof Invite, false, false, collectionOwner, true);
				}
			}
			for(ActivityTypeHandlerRecord r:typeHandlers){
				if(r.actorClass.isInstance(actor)){
					if(r.activityClass.isInstance(activity)){
						if(r.nestedActivityClass!=null && aobj instanceof Activity nestedActivity && r.nestedActivityClass.isInstance(aobj)){
							ActivityPubObject nestedObject;
							if(nestedActivity.object.object!=null)
								nestedObject=nestedActivity.object.object;
							else
								nestedObject=ctx.getObjectLinkResolver().resolve(nestedActivity.object.link);

							if(r.doublyNestedActivityClass!=null && nestedObject instanceof Activity doublyNestedActivity && r.doublyNestedActivityClass.isInstance(nestedObject)){
								ActivityPubObject doublyNestedObject;
								if(doublyNestedActivity.object.object!=null)
									doublyNestedObject=nestedActivity.object.object;
								else
									doublyNestedObject=ctx.getObjectLinkResolver().resolve(nestedActivity.object.link);

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
		}catch(UserActionNotAllowedException x){
			if(Config.DEBUG){
				LOG.warn("Rejected incoming {}: {}", getActivityType(activity), x.toString());
			}
			resp.status(403);
			return escapeHTML(x.getMessage());
		}catch(BadRequestException x){
			LOG.warn("Bad request", x);
			resp.status(400);
			return escapeHTML(x.getMessage());
		}catch(Exception x){
			LOG.warn("Exception while processing an incoming activity", x);
			throw new BadRequestException(x.toString());
		}
		if(Config.DEBUG)
			throw new BadRequestException("No handler found for activity type: "+getActivityType(activity));
		else
			LOG.error("Received and ignored an activity of an unsupported type {}", getActivityType(activity));
		return "";
	}

	private static String getActivityType(ActivityPubObject obj){
		String r=obj.getType();
		if(obj instanceof Activity a){
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

	private static ActivityPubCollectionPageResponse groupMembers(Request req, Response resp, int offset, int count, boolean tentative) throws SQLException{
		int id=safeParseInt(req.params(":id"));
		Group group=context(req).getGroupsController().getLocalGroupOrThrow(id);
		context(req).getPrivacyController().enforceGroupContentAccess(req, group);
		PaginatedList<URI> followers=GroupStorage.getGroupMemberURIs(group.id, tentative, offset, count);
		return ActivityPubCollectionPageResponse.forLinks(followers);
	}

	public static ActivityPubCollectionPageResponse groupMembers(Request req, Response resp, int offset, int count) throws SQLException{
		return groupMembers(req, resp, offset, count, false);
	}

	public static ActivityPubCollectionPageResponse groupTentativeMembers(Request req, Response resp, int offset, int count) throws SQLException{
		return groupMembers(req, resp, offset, count, true);
	}

	public static Object serviceActor(Request req, Response resp){
		resp.type(ActivityPub.CONTENT_TYPE);
		return ServiceActor.getInstance().asRootActivityPubObject(context(req), (String)null);
	}

	public static Object groupActorToken(Request req, Response resp){
		Group group=context(req).getGroupsController().getLocalGroupOrThrow(safeParseInt(req.params(":id")));
		if(group.accessType==Group.AccessType.OPEN)
			throw new ObjectNotFoundException();
		Actor signer;
		try{
			signer=ActivityPub.verifyHttpSignature(req, null);
		}catch(Exception x){
			throw new UserActionNotAllowedException("Valid member HTTP signature is required.");
		}
		if(!(signer instanceof ForeignUser user))
			throw new UserActionNotAllowedException("HTTP signature is valid but actor has wrong type: "+signer.getType());
		resp.type("application/json");
		return ActivityPub.generateActorToken(context(req), user, group);
	}

	public static Object userCollectionQuery(Request req, Response resp){
		User user=context(req).getUsersController().getLocalUserOrThrow(safeParseInt(req.params(":id")));
		return collectionQuery(user, req, resp);
	}

	public static Object groupCollectionQuery(Request req, Response resp){
		Group group=context(req).getGroupsController().getLocalGroupOrThrow(safeParseInt(req.params(":id")));
		context(req).getPrivacyController().enforceGroupContentAccess(req, group);
		return collectionQuery(group, req, resp);
	}

	private static Object collectionQuery(Actor owner, Request req, Response resp){
		URI collectionID;
		try{
			String _id=req.queryParams("collection");
			if(StringUtils.isEmpty(_id)){
				throw new BadRequestException("Collection ID (`collection` parameter) is required but was empty");
			}
			collectionID=new URI(_id);
		}catch(URISyntaxException x){
			throw new BadRequestException("Malformed collection ID", x);
		}
		if(!Config.isLocal(collectionID))
			throw new BadRequestException("Collection ID has wrong hostname, expected "+Config.domain);
		String path=collectionID.getPath();
		String actorPrefix=owner.getTypeAndIdForURL();
		if(!path.startsWith(actorPrefix))
			throw new BadRequestException("Collection path must start with actor prefix ("+actorPrefix+")");

		if(req.queryParams("item")==null)
			throw new BadRequestException("At least one `item` is required");

		List<URI> items=Arrays.stream(req.queryMap("item").values()).map(s->{
			try{
				URI uri=new URI(s);
				if(!"https".equals(uri.getScheme()) && !"http".equals(uri.getScheme()))
					throw new BadRequestException("Invalid URL scheme: '"+s+"'");
				return uri;
			}catch(URISyntaxException x){
				throw new BadRequestException("Invalid URL '"+s+"'", x);
			}
		}).limit(100).toList();

		String collectionPath=path.substring(actorPrefix.length());
		Collection<URI> filteredItems=switch(collectionPath){
			case "/wall" -> queryWallCollection(req, owner, items);
			case "/friends" -> {
				if(owner instanceof User u){
					yield queryUserFriendsCollection(req, u, items);
				}else{
					throw new BadRequestException("Unknown collection ID");
				}
			}
			case "/groups" -> {
				if(owner instanceof User u){
					yield queryUserGroupsCollection(req, u, items);
				}else{
					throw new BadRequestException("Unknown collection ID");
				}
			}
			case "/members", "/tentativeMembers" -> {
				if(owner instanceof Group g){
					yield queryGroupMembersCollection(req, g, items, "/tentativeMembers".equals(collectionPath));
				}else{
					throw new BadRequestException("Unknown collection ID");
				}
			}

			case "/following", "/followers" -> throw new BadRequestException("Querying this collection is not supported");
			default -> throw new BadRequestException("Unknown collection ID");
		};

		resp.type(ActivityPub.CONTENT_TYPE);
		CollectionQueryResult res=new CollectionQueryResult();
		res.partOf=collectionID;
		res.items=filteredItems.stream().map(LinkOrObject::new).toList();
		return res;
	}

	private static Collection<URI> queryWallCollection(Request req, Actor owner, List<URI> query){
		return context(req).getWallController().getPostLocalIDsByActivityPubIDs(query, owner).keySet();
	}

	private static Collection<URI> queryUserFriendsCollection(Request req, User owner, List<URI> query){
		return context(req).getFriendsController().getFriendsByActivityPubIDs(owner, query).keySet();
	}

	private static Collection<URI> queryGroupMembersCollection(Request req, Group owner, List<URI> query, boolean tentative){
		if(tentative && !owner.isEvent())
			throw new BadRequestException("Unknown collection ID");
		return context(req).getGroupsController().getMembersByActivityPubIDs(owner, query, tentative).keySet();
	}

	private static Collection<URI> queryUserGroupsCollection(Request req, User owner, List<URI> query){
		return context(req).getGroupsController().getUserGroupsByActivityPubIDs(owner, query).keySet();
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

	private record ActivityTypeHandlerRecord<A extends Actor, T extends Activity, N extends Activity, NN extends Activity, O extends ActivityPubObject>
			(@NotNull Class<A> actorClass, @NotNull Class<T> activityClass, @Nullable Class<N> nestedActivityClass,
				@Nullable Class<NN> doublyNestedActivityClass, @NotNull Class<O> objectClass, @NotNull ActivityTypeHandler<A, T, O> handler){
	}

	private record ActivityTypeOnlyHandlerRecord<T extends Activity>(@NotNull Class<T> activityClass, @NotNull ActivityTypeHandler<?, T, ?> handler){
	}
}
