package smithereen.routes;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import smithereen.activitypub.handlers.AddPhotoHandler;
import smithereen.activitypub.handlers.AnnounceNoteHandler;
import smithereen.activitypub.handlers.CreateNoteHandler;
import smithereen.activitypub.handlers.CreatePhotoAlbumHandler;
import smithereen.activitypub.handlers.CreatePhotoHandler;
import smithereen.activitypub.handlers.DeleteNoteHandler;
import smithereen.activitypub.handlers.DeletePersonHandler;
import smithereen.activitypub.handlers.DeletePhotoAlbumHandler;
import smithereen.activitypub.handlers.DeletePhotoHandler;
import smithereen.activitypub.handlers.FlagHandler;
import smithereen.activitypub.handlers.FollowGroupHandler;
import smithereen.activitypub.handlers.FollowPersonHandler;
import smithereen.activitypub.handlers.GroupAddPersonHandler;
import smithereen.activitypub.handlers.GroupBlockPersonHandler;
import smithereen.activitypub.handlers.GroupRemovePersonHandler;
import smithereen.activitypub.handlers.GroupUndoBlockPersonHandler;
import smithereen.activitypub.handlers.InviteGroupHandler;
import smithereen.activitypub.handlers.LeaveGroupHandler;
import smithereen.activitypub.handlers.LikeObjectHandler;
import smithereen.activitypub.handlers.OfferFollowPersonHandler;
import smithereen.activitypub.handlers.PersonAddPersonHandler;
import smithereen.activitypub.handlers.PersonBlockPersonHandler;
import smithereen.activitypub.handlers.PersonMovePersonHandler;
import smithereen.activitypub.handlers.PersonRemovePersonHandler;
import smithereen.activitypub.handlers.PersonUndoBlockPersonHandler;
import smithereen.activitypub.handlers.ReadNoteHandler;
import smithereen.activitypub.handlers.RejectAddNoteHandler;
import smithereen.activitypub.handlers.RejectFollowGroupHandler;
import smithereen.activitypub.handlers.RejectFollowPersonHandler;
import smithereen.activitypub.handlers.RejectInviteGroupHandler;
import smithereen.activitypub.handlers.RejectOfferFollowPersonHandler;
import smithereen.activitypub.handlers.RejectPhotoHandler;
import smithereen.activitypub.handlers.RemoveGroupHandler;
import smithereen.activitypub.handlers.RemoveNoteHandler;
import smithereen.activitypub.handlers.GroupRemovePhotoHandler;
import smithereen.activitypub.handlers.UndoAcceptFollowGroupHandler;
import smithereen.activitypub.handlers.UndoAcceptFollowPersonHandler;
import smithereen.activitypub.handlers.UndoAnnounceNoteHandler;
import smithereen.activitypub.handlers.UndoFollowGroupHandler;
import smithereen.activitypub.handlers.UndoFollowPersonHandler;
import smithereen.activitypub.handlers.UndoInviteGroupHandler;
import smithereen.activitypub.handlers.UndoLikeObjectHandler;
import smithereen.activitypub.handlers.UpdateGroupHandler;
import smithereen.activitypub.handlers.UpdateNoteHandler;
import smithereen.activitypub.handlers.UpdatePersonHandler;
import smithereen.activitypub.handlers.UpdatePhotoAlbumHandler;
import smithereen.activitypub.handlers.UpdatePhotoHandler;
import smithereen.activitypub.handlers.UserRemovePhotoHandler;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
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
import smithereen.activitypub.objects.activities.Move;
import smithereen.activitypub.objects.activities.Offer;
import smithereen.activitypub.objects.activities.Read;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.activitypub.objects.activities.Update;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.exceptions.RemoteObjectFetchException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.FederationRestriction;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.FriendshipStatus;
import smithereen.model.Group;
import smithereen.model.NodeInfo;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.OwnedContentObject;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.PaginatedList;
import smithereen.model.Poll;
import smithereen.model.PollOption;
import smithereen.model.PollVote;
import smithereen.model.Post;
import smithereen.model.Server;
import smithereen.model.StatsType;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.text.TextProcessor;
import smithereen.util.UriBuilder;
import smithereen.model.User;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.jsonld.JLDProcessor;
import smithereen.jsonld.LinkedDataSignatures;
import smithereen.model.UserBanStatus;
import smithereen.sparkext.ActivityPubCollectionPageResponse;
import smithereen.storage.GroupStorage;
import smithereen.storage.LikeStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.util.XTEA;
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
		registerActivityHandler(ForeignUser.class, Announce.class, NoteOrQuestion.class, new AnnounceNoteHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Announce.class, NoteOrQuestion.class, new UndoAnnounceNoteHandler());
		registerActivityHandler(ForeignUser.class, Update.class, NoteOrQuestion.class, new UpdateNoteHandler());
		registerActivityHandler(ForeignUser.class, Delete.class, NoteOrQuestion.class, new DeleteNoteHandler());
		registerActivityHandler(Actor.class, Reject.class, Add.class, NoteOrQuestion.class, new RejectAddNoteHandler());
		registerActivityHandler(ForeignUser.class, Read.class, NoteOrQuestion.class, new ReadNoteHandler());

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
		registerActivityHandler(ForeignUser.class, Move.class, ForeignUser.class, new PersonMovePersonHandler());

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
		registerActivityHandler(Actor.class, Remove.class, NoteOrQuestion.class, new RemoveNoteHandler());

		registerActivityHandler(Flag.class, new FlagHandler());

		// Photo albums
		registerActivityHandler(Actor.class, Create.class, ActivityPubPhotoAlbum.class, new CreatePhotoAlbumHandler());
		registerActivityHandler(Actor.class, Delete.class, ActivityPubPhotoAlbum.class, new DeletePhotoAlbumHandler());
		registerActivityHandler(Actor.class, Update.class, ActivityPubPhotoAlbum.class, new UpdatePhotoAlbumHandler());
		registerActivityHandler(Actor.class, Add.class, ActivityPubPhoto.class, new AddPhotoHandler());
		registerActivityHandler(ForeignUser.class, Create.class, ActivityPubPhoto.class, new CreatePhotoHandler());
		registerActivityHandler(ForeignUser.class, Update.class, ActivityPubPhoto.class, new UpdatePhotoHandler());
		registerActivityHandler(ForeignUser.class, Delete.class, ActivityPubPhoto.class, new DeletePhotoHandler());
		registerActivityHandler(ForeignGroup.class, Remove.class, ActivityPubPhoto.class, new GroupRemovePhotoHandler());
		registerActivityHandler(ForeignUser.class, Remove.class, ActivityPubPhoto.class, new UserRemovePhotoHandler());
		registerActivityHandler(ForeignUser.class, Reject.class, ActivityPubPhoto.class, new RejectPhotoHandler());

		// More general handlers at the end so they match last
		registerActivityHandler(ForeignUser.class, Like.class, ActivityPubObject.class, new LikeObjectHandler());
		registerActivityHandler(ForeignUser.class, Undo.class, Like.class, ActivityPubObject.class, new UndoLikeObjectHandler());
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
		List<Post> posts=PostStorage.getWallPosts(user.id, false, minID, maxID, 0, 25, _total, true, EnumSet.of(Post.Privacy.PUBLIC));
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

	public static ActivityPubCollectionPageResponse userAlbums(Request req, Response resp, int offset, int count){
		ApplicationContext ctx=context(req);
		User user=ctx.getUsersController().getLocalUserOrThrow(parseIntOrDefault(req.params(":id"), 0));
		List<PhotoAlbum> albums=ctx.getPhotosController().getAllAlbumsForActivityPub(user, req);
		return ActivityPubCollectionPageResponse.forObjects(albums.stream().map(a->ActivityPubPhotoAlbum.fromNativeAlbum(a, ctx)).toList(), albums.size());
	}

	public static ActivityPubCollectionPageResponse groupAlbums(Request req, Response resp, int offset, int count){
		ApplicationContext ctx=context(req);
		Group group=ctx.getGroupsController().getLocalGroupOrThrow(parseIntOrDefault(req.params(":id"), 0));
		List<PhotoAlbum> albums=ctx.getPhotosController().getAllAlbumsForActivityPub(group, req);
		return ActivityPubCollectionPageResponse.forObjects(albums.stream().map(a->ActivityPubPhotoAlbum.fromNativeAlbum(a, ctx)).toList(), albums.size());
	}

	public static ActivityPubCollectionPageResponse photoAlbum(Request req, Response resp, int offset, int count){
		ApplicationContext ctx=context(req);
		PhotoAlbum album=ctx.getPhotosController().getAlbumForActivityPub(XTEA.deobfuscateObjectID(Utils.decodeLong(req.params(":id")), ObfuscatedObjectIDType.PHOTO_ALBUM), req);
		PaginatedList<Photo> photos=ctx.getPhotosController().getAlbumPhotos(null, album, offset, count, false);
		return ActivityPubCollectionPageResponse.forLinksOrObjects(photos.list.stream().map(p->p.apID==null ? new LinkOrObject(ActivityPubPhoto.fromNativePhoto(p, album, ctx)) : new LinkOrObject(p.apID)).toList(), album.numPhotos)
				.withCustomObject(ActivityPubPhotoAlbum.fromNativeAlbum(album, ctx));
	}

	public static Object photo(Request req, Response resp){
		ApplicationContext ctx=context(req);
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.deobfuscateObjectID(decodeLong(req.params(":id")), ObfuscatedObjectIDType.PHOTO));
		PhotoAlbum album=ctx.getPhotosController().getAlbumForActivityPub(photo.albumID, req);
		return ActivityPubPhoto.fromNativePhoto(photo, album, ctx);
	}

	public static Object comment(Request req, Response resp){
		ApplicationContext ctx=context(req);
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(req.params(":id"), ObfuscatedObjectIDType.COMMENT));
		ctx.getPrivacyController().enforceContentPrivacyForActivityPub(req, comment);
		return NoteOrQuestion.fromNativeComment(comment, ctx);
	}

	public static ActivityPubCollectionPageResponse commentReplies(Request req, Response resp, int offset, int count){
		ApplicationContext ctx=context(req);
		Comment comment=ctx.getCommentsController().getCommentIgnoringPrivacy(XTEA.decodeObjectID(req.params(":id"), ObfuscatedObjectIDType.COMMENT));
		ctx.getPrivacyController().enforceContentPrivacyForActivityPub(req, comment);
		PaginatedList<Comment> comments=ctx.getCommentsController().getCommentReplies(
				ctx.getCommentsController().getCommentParentIgnoringPrivacy(comment),
				comment, offset, count
		);
		return ActivityPubCollectionPageResponse.forLinksOrObjects(comments.list.stream().map(c->c.isLocal() ? new LinkOrObject(NoteOrQuestion.fromNativeComment(c, ctx)) : new LinkOrObject(c.getActivityPubID())).toList(), comments.total);
	}

	public static ActivityPubCollectionPageResponse photoComments(Request req, Response resp, int offset, int count){
		ApplicationContext ctx=context(req);
		Photo photo=ctx.getPhotosController().getPhotoIgnoringPrivacy(XTEA.decodeObjectID(req.params(":id"), ObfuscatedObjectIDType.PHOTO));
		ctx.getPrivacyController().enforceContentPrivacyForActivityPub(req, photo);
		return objectComments(req, resp, photo, offset, count);
	}

	private static ActivityPubCollectionPageResponse objectComments(Request req, Response resp, CommentableContentObject obj, int offset, int count){
		ApplicationContext ctx=context(req);
		PaginatedList<Comment> comments=ctx.getCommentsController().getCommentReplies(obj, null, offset, count);
		return ActivityPubCollectionPageResponse.forLinksOrObjects(comments.list.stream().map(c->c.isLocal() ? new LinkOrObject(NoteOrQuestion.fromNativeComment(c, ctx)) : new LinkOrObject(c.getActivityPubID())).toList(), comments.total);
	}

	public static ActivityPubCollectionPageResponse photoAlbumComments(Request req, Response resp, int offset, int count){
		ApplicationContext ctx=context(req);
		PhotoAlbum album=ctx.getPhotosController().getAlbumForActivityPub(XTEA.deobfuscateObjectID(Utils.decodeLong(req.params(":id")), ObfuscatedObjectIDType.PHOTO_ALBUM), req);
		PaginatedList<Comment> comments=ctx.getCommentsController().getPhotoAlbumComments(album, offset, count);
		return ActivityPubCollectionPageResponse.forLinksOrObjects(comments.list.stream().map(c->c.isLocal() ? new LinkOrObject(NoteOrQuestion.fromNativeComment(c, ctx)) : new LinkOrObject(c.getActivityPubID())).toList(), comments.total);
	}

	public static Object externalInteraction(Request req, Response resp, Account self, ApplicationContext ctx){
		// ?type=reblog
		// ?type=favourite
		// ?type=reply
		// user/remote_follow
		requireQueryParams(req, "uri");
		String uri=req.queryParams("uri");
//		String ref=req.headers("referer");

		Lang l=lang(req);
		try{
			resp.redirect(switch(ctx.getSearchController().loadRemoteObject(self.user, uri)){
				case Post post when post.getReplyLevel()>0 -> Config.localURI("/posts/"+post.replyKey.getFirst()+"#comment"+post.id).toString();
				case Post post -> post.getInternalURL().toString();
				case Actor actor -> actor.getProfileURL();
				case PhotoAlbum album -> album.getURL();
				case Photo photo -> photo.getURL();
				case Comment comment -> ctx.getCommentsController().getCommentParent(self.user, comment).getURL();
				default -> throw new RemoteObjectFetchException(RemoteObjectFetchException.ErrorType.UNSUPPORTED_OBJECT_TYPE, null);
			});
			return "";
		}catch(RemoteObjectFetchException x){
			Object err=switch(x.error){
				case UNSUPPORTED_OBJECT_TYPE -> wrapError(req, resp, "unsupported_remote_object_type");
				case TIMEOUT -> {
					if(x.uri!=null)
						yield wrapError(req, resp, "remote_object_timeout", Map.of("server", x.uri.getHost()));
					else
						yield wrapError(req, resp, "remote_object_loading_error");
				}
				case NETWORK_ERROR -> wrapError(req, resp, "remote_object_network_error");
				case NOT_FOUND -> wrapError(req, resp, "remote_object_not_found");
				case OTHER_ERROR -> wrapError(req, resp, "remote_object_loading_error");
			};
			if(err instanceof RenderedTemplateResponse model){
				if(x.getCause()!=null){
					model.with("details", x.getCause().getMessage());
				}else if(StringUtils.isNotEmpty(x.getMessage())){
					model.with("details", x.getMessage());
				}
			}
			return err;
		}
	}

	public static Object remoteFollow(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		String username=req.params(":username");
		if(!(ctx.getUsersController().getUserByUsernameOrThrow(username) instanceof ForeignUser user)){
			return Utils.wrapError(req, resp, "err_user_not_found");
		}
		FriendshipStatus status=UserStorage.getFriendshipStatus(self.user.id, user.id);
		if(status==FriendshipStatus.REQUEST_SENT){
			return Utils.wrapError(req, resp, "err_friend_req_already_sent");
		}else if(status==FriendshipStatus.FOLLOWING){
			return Utils.wrapError(req, resp, "err_already_following");
		}else if(status==FriendshipStatus.FRIENDS){
			return Utils.wrapError(req, resp, "err_already_friends");
		}
		String msg=req.queryParams("message");
		if(user.supportsFriendRequests()){
			ctx.getFriendsController().sendFriendRequest(self.user, user, msg);
		}else{
			ctx.getFriendsController().followUser(self.user, user);
		}
		resp.redirect(user.getProfileURL());
		return "Success";
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
		nodeInfo.metadata=Map.of(
				"nodeName", Objects.requireNonNullElse(Config.serverDisplayName, Config.domain),
				"nodeDescription", TextProcessor.stripHTML(Objects.requireNonNull(Config.serverShortDescription, ""), true),
				"faspBaseUrl", Config.localURI("/api/fasp").toString()
		);

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
		LOG.debug("Incoming activity: {}", body);
		JsonObject rawActivity;
		try{
			rawActivity=JsonParser.parseString(body).getAsJsonObject();
		}catch(Exception x){
			throw new BadRequestException("Failed to parse request body as JSON", x);
		}
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
		if(activity instanceof Delete && (activity.actor.link.equals(activity.object.link) || (activity.object.object!=null && activity.actor.link.equals(activity.object.object.activityPubID)))){
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
		if(actor instanceof ForeignUser fu && fu.banStatus==UserBanStatus.SUSPENDED){
			resp.status(403);
			return "This actor is suspended from this server";
		}
		if(fa.needUpdate() && canUpdate){
			try{
				actor=ctx.getObjectLinkResolver().resolve(activity.actor.link, Actor.class, true, true, true);
			}catch(ObjectNotFoundException x){
				LOG.debug("Exception while refreshing remote actor {}", activity.actor.link, x);
			}
		}

		Actor httpSigOwner;
		try{
			httpSigOwner=ActivityPub.verifyHttpSignature(req, actor);
		}catch(Exception x){
			LOG.debug("Exception while verifying HTTP signature", x);
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
				LOG.debug("verified LD signature by {}", userID);
				hasValidLDSignature=true;
			}catch(Exception x){
				LOG.debug("Exception while verifying LD-signature", x);
			}
		}
		if(!hasValidLDSignature){
			if(!actor.equals(httpSigOwner)){
				throw new BadRequestException("In the absence of a valid LD-signature, HTTP signature must be made by the activity actor");
			}
			LOG.debug("verified HTTP signature by {}", httpSigOwner.activityPubID);
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
			if(activity.object==null){
				// Something unsupported that doesn't have an object/link
				if(Config.DEBUG)
					throw new BadRequestException("No handler found for activity type: "+getActivityType(activity));
				else
					LOG.error("Received and ignored an activity of an unsupported type {}", getActivityType(activity));
				return "";
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
						LOG.debug("Activity object not found for {}: {}", getActivityType(activity), aobj.activityPubID);
						// Fail silently. We didn't have that object anyway, there's nothing to delete.
						return "";
					}
				}
			}else{
				if(activity instanceof Like || activity instanceof Delete){
					try{
						Object localObject=ctx.getObjectLinkResolver().resolveLocally(activity.object.link, Object.class);
						aobj=ctx.getObjectLinkResolver().convertToActivityPubObject(localObject, ActivityPubObject.class);
					}catch(ObjectNotFoundException x){
						// Fail silently. Pleroma sends all likes to followers, including for objects they may not have.
						LOG.debug("Activity object not known for {}: {}", activity.getType(), activity.object.link);
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
									LOG.debug("Found match: {}", r.handler.getClass().getName());
									((DoublyNestedActivityTypeHandler)r.handler).handle(context, actor, activity, nestedActivity, doublyNestedActivity, doublyNestedObject);
									return "";
								}
							}else if(r.objectClass.isInstance(nestedObject)){
								LOG.debug("Found match: {}", r.handler.getClass().getName());
								((NestedActivityTypeHandler)r.handler).handle(context, actor, activity, nestedActivity, nestedObject);
								return "";
							}
						}else if(r.objectClass.isInstance(aobj)){
							LOG.debug("Found match: {}", r.handler.getClass().getName());
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
			return TextProcessor.escapeHTML(x.getMessage());
		}catch(BadRequestException x){
			LOG.debug("Bad request", x);
			resp.status(400);
			return TextProcessor.escapeHTML(x.getMessage());
		}/*catch(Exception x){
			LOG.warn("Exception while processing an incoming activity", x);
			throw new BadRequestException(x.toString());
		}*/
		if(Config.DEBUG)
			throw new BadRequestException("No handler found for activity type: "+getActivityType(activity));
		else
			LOG.error("Received and ignored an activity of an unsupported type {}", getActivityType(activity));
		return "";
	}

	private static String getActivityType(ActivityPubObject obj){
		String r=obj.getType();
		if(obj instanceof Activity a && a.object!=null){
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

	public static ActivityPubCollectionPageResponse userTaggedPhotos(Request req, Response resp, int offset, int count){
		ApplicationContext ctx=context(req);
		User user=ctx.getUsersController().getLocalUserOrThrow(safeParseInt(req.params(":id")));
		ctx.getPrivacyController().enforceUserPrivacyForRemoteServer(req, user, user.getPrivacySetting(UserPrivacySettingKey.PHOTO_TAG_LIST));
		PaginatedList<Photo> photos=ctx.getPhotosController().getUserTaggedPhotosIgnoringPrivacy(user, offset, count);
		Map<Long, PhotoAlbum> albums=ctx.getPhotosController().getAlbumsIgnoringPrivacy(photos.list.stream().map(p->p.albumID).collect(Collectors.toSet()));
		return ActivityPubCollectionPageResponse.forLinksOrObjects(photos.list.stream().map(p->p.apID==null ? new LinkOrObject(ActivityPubPhoto.fromNativePhoto(p, albums.get(p.albumID), ctx)) : new LinkOrObject(p.apID)).toList(), photos.total);
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
		ApplicationContext ctx=context(req);
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

		Collection<URI> filteredItems;
		if(path.startsWith(actorPrefix)){
			String collectionPath=path.substring(actorPrefix.length());
			filteredItems=switch(collectionPath){
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
				case "/tagged" -> {
					if(owner instanceof User u){
						yield queryUserTaggedPhotosCollection(req, u, items);
					}else{
						throw new BadRequestException("Unknown collection ID");
					}
				}

				case "/following", "/followers" -> throw new BadRequestException("Querying this collection is not supported");
				default -> throw new BadRequestException("Unknown collection ID");
			};
		}else{
			if(path.endsWith("/comments")){
				URI parentObjectID=new UriBuilder(collectionID).rawPath(path.substring(0, path.lastIndexOf('/')).substring(1)).build();
				if(ObjectLinkResolver.getObjectIdFromLocalURL(parentObjectID) instanceof ObjectLinkResolver.ObjectTypeAndID(ObjectLinkResolver.ObjectType type, long id)){
					filteredItems=switch(type){
						case PHOTO_ALBUM -> {
							PhotoAlbum album=ctx.getPhotosController().getAlbumForActivityPub(id, req);
							if(album.activityPubID!=null)
								throw new ObjectNotFoundException();
							if(album.ownerID!=owner.getOwnerID())
								throw new BadRequestException("This photo album is not owned by this actor");
							yield ctx.getCommentsController().getPhotoAlbumCommentIDs(album, items);
						}
						default -> throw new BadRequestException("Unknown collection ID");
					};
				}else{
					throw new BadRequestException("Unknown collection ID");
				}
			}else if(ObjectLinkResolver.getObjectIdFromLocalURL(collectionID) instanceof ObjectLinkResolver.ObjectTypeAndID(ObjectLinkResolver.ObjectType type, long id)){
				if(type==ObjectLinkResolver.ObjectType.PHOTO_ALBUM){
					if(!(owner instanceof Group))
						throw new BadRequestException("Querying this collection is not supported");
					PhotoAlbum album=ctx.getPhotosController().getAlbumForActivityPub(id, req);
					if(album.activityPubID!=null)
						throw new ObjectNotFoundException();
					if(album.ownerID!=owner.getOwnerID())
						throw new BadRequestException("This photo album is not owned by this actor");
					filteredItems=ctx.getPhotosController().getAlbumPhotosActivityPubIDs(album, items);
				}else{
					throw new BadRequestException("Unknown collection ID");
				}
			}else{
				throw new BadRequestException("Unknown collection ID");
			}
		}

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

	private static Collection<URI> queryUserTaggedPhotosCollection(Request req, User owner, List<URI> query){
		return context(req).getPhotosController().getUserTaggedPhotosActivityPubIDs(owner, query);
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
