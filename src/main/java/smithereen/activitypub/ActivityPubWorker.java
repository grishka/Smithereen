package smithereen.activitypub;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.ServiceActor;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Add;
import smithereen.activitypub.objects.activities.Block;
import smithereen.activitypub.objects.activities.Create;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.activitypub.objects.activities.Flag;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Invite;
import smithereen.activitypub.objects.activities.Join;
import smithereen.activitypub.objects.activities.Leave;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Offer;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.activitypub.objects.activities.Update;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.OwnerAndAuthor;
import smithereen.data.Poll;
import smithereen.data.PollOption;
import smithereen.data.PollVote;
import smithereen.data.Post;
import smithereen.data.UriBuilder;
import smithereen.data.User;
import smithereen.data.notifications.NotificationUtils;
import smithereen.exceptions.FederationException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.GroupStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.utils.StringUtils;

public class ActivityPubWorker{
	private static final Logger LOG=LoggerFactory.getLogger(ActivityPubWorker.class);
	/**
	 * Will not fetch more than this many comments
	 */
	private static final int MAX_COMMENTS=1000;
	/**
	 * Will not fetch more than this many friends (or followers/following if there's no friends collection)
	 */
	private static final int MAX_FRIENDS=10_000;
	private static final int ACTORS_BATCH_SIZE=25;

	private final ForkJoinPool executor;
	private final ScheduledExecutorService retryExecutor;
	private final Random rand=new Random();

	// These must be accessed from synchronized(this)
	private HashMap<URI, Future<List<Post>>> fetchingReplyThreads=new HashMap<>();
	private HashMap<URI, List<Consumer<List<Post>>>> afterFetchReplyThreadActions=new HashMap<>();
	private HashMap<URI, Future<Post>> fetchingAllReplies=new HashMap<>();
	private HashSet<URI> fetchingRelationshipCollectionsActors=new HashSet<>();
	private HashSet<URI> fetchingContentCollectionsActors=new HashSet<>();

	private final ApplicationContext context;

	public ActivityPubWorker(ApplicationContext context){
		this.context=context;
		executor=new ForkJoinPool(Runtime.getRuntime().availableProcessors()*2);
		retryExecutor=Executors.newSingleThreadScheduledExecutor();
	}

	public void shutDown(){
		LOG.info("Stopping thread pool");
		Utils.stopExecutorBlocking(executor, LOG);
		LOG.info("Stopped");
	}

	private URI actorInbox(ForeignUser actor){
		return actor.sharedInbox!=null ? actor.sharedInbox : actor.inbox;
	}

	private URI actorInbox(ForeignGroup actor){
		return actor.sharedInbox!=null ? actor.sharedInbox : actor.inbox;
	}

	private URI actorInbox(Actor actor){
		if(actor instanceof ForeignUser fu)
			return actorInbox(fu);
		else if(actor instanceof ForeignGroup fg)
			return actorInbox(fg);
		throw new IllegalArgumentException("Must be a foreign actor");
	}

	private long rand(){
		return Math.abs(rand.nextLong());
	}

	public void forwardActivity(String json, User signer, Collection<URI> inboxes, String originatingDomain){
		for(URI inbox:inboxes){
			if(inbox.getHost().equalsIgnoreCase(originatingDomain))
				continue;
			executor.submit(new ForwardOneActivityRunnable(json, inbox, signer));
		}
	}

	private Set<URI> getInboxesForPost(Post post) throws SQLException{
		Set<URI> inboxes=new HashSet<>();
		OwnerAndAuthor oaa=context.getWallController().getPostAuthorAndOwner(post);
		if(oaa.owner() instanceof User user){
			boolean sendToFollowers=user.id==post.authorID;
			if(oaa.owner() instanceof ForeignUser foreignUser){
				inboxes.add(actorInbox(foreignUser));
			}else if(sendToFollowers && post.getReplyLevel()==0){
				inboxes.addAll(UserStorage.getFollowerInboxes(user.id));
			}else{
				inboxes.addAll(PostStorage.getInboxesForPostInteractionForwarding(post));
			}
		}else if(oaa.owner() instanceof Group group){
			if(oaa.owner() instanceof ForeignGroup foreignGroup){
				inboxes.add(actorInbox(foreignGroup));
			}else if(post.getReplyLevel()==0){
				inboxes.addAll(GroupStorage.getGroupMemberInboxes(group.id));
			}else{
				inboxes.addAll(PostStorage.getInboxesForPostInteractionForwarding(post));
			}
		}
		if(!post.mentionedUserIDs.isEmpty()){
			for(User user: context.getUsersController().getUsers(post.mentionedUserIDs).values()){
				if(user instanceof ForeignUser foreignUser){
					URI inbox=actorInbox(foreignUser);
					inboxes.add(inbox);
				}
			}
		}
		return inboxes;
	}

	private void sendActivityForPost(Post post, Activity activity, Actor actor){
		try{
			Set<URI> inboxes=getInboxesForPost(post);
			LOG.info("Inboxes: {}", inboxes);
			for(URI inbox:inboxes){
				executor.submit(new SendOneActivityRunnable(activity, inbox, actor));
			}
		}catch(SQLException x){
			LOG.error("Exception while sending activity for post {}", post.getActivityPubID(), x);
		}
	}

	public void sendCreatePostActivity(final Post post){
		executor.submit(()->{
			NoteOrQuestion note=NoteOrQuestion.fromNativePost(post, context);
			User author=context.getUsersController().getUserOrThrow(post.authorID);

			Create create=new Create();
			create.object=new LinkOrObject(note);
			create.actor=new LinkOrObject(author.activityPubID);
			create.to=note.to;
			create.cc=note.cc;
			create.published=note.published;
			create.activityPubID=Config.localURI(note.activityPubID.getPath()+"/activityCreate");
			sendActivityForPost(post, create, author);
		});
	}

	public void sendUpdatePostActivity(final Post post){
		executor.submit(()->{
			NoteOrQuestion note=NoteOrQuestion.fromNativePost(post, context);
			User author=context.getUsersController().getUserOrThrow(post.authorID);

			Update update=new Update();
			update.object=new LinkOrObject(note);
			update.actor=new LinkOrObject(author.activityPubID);
			update.to=note.to;
			update.cc=note.cc;
			update.published=note.updated;
			update.activityPubID=Config.localURI(note.activityPubID.getPath()+"#update_"+rand());
			sendActivityForPost(post, update, author);
		});
	}

	public void sendAddPostToWallActivity(final Post post){
		executor.submit(()->{
			try{
				NoteOrQuestion note=NoteOrQuestion.fromNativePost(post, context);
				OwnerAndAuthor oaa=context.getWallController().getPostAuthorAndOwner(post);

				Add add=new Add();
				add.activityPubID=UriBuilder.local().path("posts", String.valueOf(post.id), "activityAdd").build();
				add.object=new LinkOrObject(post.getActivityPubID());
				add.actor=new LinkOrObject(oaa.owner().activityPubID);
				add.to=List.of(new LinkOrObject(ActivityPub.AS_PUBLIC), new LinkOrObject(oaa.owner().getFollowersURL()), new LinkOrObject(oaa.author().activityPubID));
//				if(!post.mentionedUsers.isEmpty()){
//					ArrayList<LinkOrObject> cc=new ArrayList<>();
//					for(User user : post.mentionedUsers){
//						cc.add(new LinkOrObject(user.activityPubID));
//					}
//					add.cc=cc;
//				}
				add.cc=note.cc;
				ActivityPubCollection target=new ActivityPubCollection(false);
				target.activityPubID=oaa.owner().getWallURL();
				target.attributedTo=oaa.owner().activityPubID;
				add.target=new LinkOrObject(target);

				HashSet<URI> inboxes=new HashSet<>();
				if(oaa.owner() instanceof User user)
					inboxes.addAll(UserStorage.getFollowerInboxes(user.id));
				else if(oaa.owner() instanceof Group group)
					inboxes.addAll(GroupStorage.getGroupMemberInboxes(group.id));

				for(User user:context.getUsersController().getUsers(post.mentionedUserIDs).values()){
					if(user instanceof ForeignUser){
						URI inbox=actorInbox((ForeignUser) user);
						inboxes.add(inbox);
					}
				}
				if(oaa.author() instanceof ForeignUser fu){
					URI inbox=actorInbox(fu);
					inboxes.add(inbox);
				}

				for(URI inbox:inboxes){
					executor.submit(new SendOneActivityRunnable(add, inbox, oaa.owner()));
				}
			}catch(SQLException x){
				LOG.error("Exception while sending wall post {}", post.getActivityPubID(), x);
			}
		});
	}

	public void sendDeletePostActivity(final Post post, final User actualActor){
		executor.submit(()->{
			Actor actor;
			Delete delete=new Delete();
			delete.object=new LinkOrObject(post.getActivityPubID());
			if(post.authorID==actualActor.id)
				actor=actualActor;
			else if(!post.isGroupOwner() && post.ownerID==actualActor.id)
				actor=actualActor;
			else if(post.isGroupOwner())
				actor=context.getWallController().getPostAuthorAndOwner(post).owner();
			else{
				LOG.error("Shouldn't happen: post {} actor for delete can't be chosen", post.id);
				return;
			}
			if(actor instanceof ForeignGroup || actor instanceof ForeignUser){
				LOG.error("Shouldn't happen: {} actor for delete is a foreign actor", post.id);
				return;
			}

			NoteOrQuestion note=NoteOrQuestion.fromNativePost(post, context);

			delete.actor=new LinkOrObject(actor.activityPubID);
			delete.to=note.to;
			delete.cc=note.cc;
			delete.published=Instant.now();
			delete.activityPubID=new UriBuilder(post.getActivityPubID()).appendPath("delete").build();
			sendActivityForPost(post, delete, actor);
		});
	}

	public void sendUnfriendActivity(User self, User target){
		if(!(target instanceof ForeignUser))
			return;
		Undo undo=new Undo();
		undo.activityPubID=new UriBuilder(self.activityPubID).fragment("unfollowUser"+target.id+"_"+rand()).build();
		undo.actor=new LinkOrObject(self.activityPubID);

		Follow follow=new Follow();
		follow.actor=new LinkOrObject(self.activityPubID);
		follow.object=new LinkOrObject(target.activityPubID);
		follow.activityPubID=new UriBuilder(self.activityPubID).fragment("followUser"+target.id+"_"+rand()).build();
		undo.object=new LinkOrObject(follow);

		executor.submit(new SendOneActivityRunnable(undo, ((ForeignUser) target).inbox, self));
	}

	public void sendRemoveFromFriendsCollectionActivity(User self, User exFriend){
		Remove remove=new Remove();
		remove.activityPubID=new UriBuilder(self.activityPubID).fragment("unfriendUserCollection"+exFriend.id+"_"+rand()).build();
		remove.actor=new LinkOrObject(self.activityPubID);
		remove.object=new LinkOrObject(exFriend.activityPubID);
		remove.target=new LinkOrObject(self.getFriendsURL());

		try{
			List<URI> inboxes=UserStorage.getFollowerInboxes(self.id);
			for(URI inbox:inboxes){
				executor.submit(new SendOneActivityRunnable(remove, inbox, self));
			}
		}catch(SQLException x){
			LOG.error("Exception while sending remove from friends collection activity ({} removed {})", self.activityPubID, exFriend.activityPubID, x);
		}
	}

	public void sendAddToFriendsCollectionActivity(User self, User friend){
		Add add=new Add();
		add.activityPubID=new UriBuilder(self.activityPubID).fragment("addFriendUserCollection"+friend.id+"_"+rand()).build();
		add.actor=new LinkOrObject(self.activityPubID);
		add.object=new LinkOrObject(friend.activityPubID);
		add.target=new LinkOrObject(self.getFriendsURL());

		try{
			List<URI> inboxes=UserStorage.getFollowerInboxes(self.id);
			for(URI inbox:inboxes){
				executor.submit(new SendOneActivityRunnable(add, inbox, self));
			}
		}catch(SQLException x){
			LOG.error("Exception while sending add to friends collection activity ({} added {})", self.activityPubID, friend.activityPubID, x);
		}
	}

	public void sendAddToGroupsCollectionActivity(User self, Group group, boolean tentative){
		Add add=new Add();
		add.activityPubID=new UriBuilder(self.activityPubID).fragment("addGroupCollection"+group.id+"_"+rand()).build();
		add.actor=new LinkOrObject(self.activityPubID);
		add.object=new LinkOrObject(group.activityPubID);
		add.target=new LinkOrObject(self.getGroupsURL());
		add.tentative=tentative;

		try{
			List<URI> inboxes=UserStorage.getFollowerInboxes(self.id);
			for(URI inbox:inboxes){
				executor.submit(new SendOneActivityRunnable(add, inbox, self));
			}
		}catch(SQLException x){
			LOG.error("Exception while sending add to groups collection activity ({} joined {})", self.activityPubID, group.activityPubID, x);
		}
	}

	public void sendRemoveFromGroupsCollectionActivity(User self, Group group){
		Remove remove=new Remove();
		remove.activityPubID=new UriBuilder(self.activityPubID).fragment("removeGroupCollection"+group.id+"_"+rand()).build();
		remove.actor=new LinkOrObject(self.activityPubID);
		remove.object=new LinkOrObject(group.activityPubID);
		remove.target=new LinkOrObject(self.getGroupsURL());

		try{
			List<URI> inboxes=UserStorage.getFollowerInboxes(self.id);
			for(URI inbox:inboxes){
				executor.submit(new SendOneActivityRunnable(remove, inbox, self));
			}
		}catch(SQLException x){
			LOG.error("Exception while sending remove from groups collection activity ({} left {})", self.activityPubID, group.activityPubID, x);
		}
	}

	public void sendFollowUserActivity(User self, ForeignUser target){
		Follow follow=new Follow();
		follow.actor=new LinkOrObject(self.activityPubID);
		follow.object=new LinkOrObject(target.activityPubID);
		follow.activityPubID=new UriBuilder(self.activityPubID).fragment("followUser"+target.id+"_"+rand()).build();
		executor.submit(new SendOneActivityRunnable(follow, target.inbox, self));
	}

	public void sendJoinGroupActivity(User self, ForeignGroup target, boolean tentative){
		Activity follow;
//		if(target.hasCapability(ForeignGroup.Capability.JOIN_LEAVE_ACTIVITIES))
			follow=new Join(tentative);
//		else
//			follow=new Follow();
		follow.actor=new LinkOrObject(self.activityPubID);
		follow.object=new LinkOrObject(target.activityPubID);
		follow.activityPubID=new UriBuilder(self.activityPubID).fragment("joinGroup"+target.id+"_"+rand()).build();
		executor.submit(new SendOneActivityRunnable(follow, target.inbox, self));
	}

	public void sendLeaveGroupActivity(User self, ForeignGroup target){
		Activity undo;
//		if(target.hasCapability(ForeignGroup.Capability.JOIN_LEAVE_ACTIVITIES)){
			undo=new Leave();
			undo.object=new LinkOrObject(target.activityPubID);
//		}else{
//			undo=new Undo();
//			Follow follow=new Follow();
//			follow.actor=new LinkOrObject(self.activityPubID);
//			follow.object=new LinkOrObject(target.activityPubID);
//			follow.activityPubID=new UriBuilder(self.activityPubID).fragment("joinGroup"+target.id+"_"+rand()).build();
//			undo.object=new LinkOrObject(follow);
//		}
		undo.activityPubID=new UriBuilder(self.activityPubID).fragment("leaveGroup"+target.id+"_"+rand()).build();
		undo.actor=new LinkOrObject(self.activityPubID);

		executor.submit(new SendOneActivityRunnable(undo, target.inbox, self));
	}

	public void sendFriendRequestActivity(User self, ForeignUser target, String message){
		Follow follow=new Follow();
		follow.actor=new LinkOrObject(self.activityPubID);
		follow.object=new LinkOrObject(target.activityPubID);
		follow.activityPubID=URI.create(self.activityPubID+"#follow"+target.id+"_"+rand());
		if(target.supportsFriendRequests()){
			Offer offer=new Offer();
			offer.actor=new LinkOrObject(self.activityPubID);
			offer.activityPubID=URI.create(self.activityPubID+"#friend_request"+target.id+"_"+rand());
			if(StringUtils.isNotEmpty(message)){
				offer.content=message;
			}
			Follow revFollow=new Follow();
			revFollow.actor=new LinkOrObject(target.activityPubID);
			revFollow.object=new LinkOrObject(self.activityPubID);
			offer.object=new LinkOrObject(revFollow);
			executor.submit(new SendActivitySequenceRunnable(Arrays.asList(follow, offer), target.inbox, self));
		}else{
			executor.submit(new SendOneActivityRunnable(follow, target.inbox, self));
		}
	}

	public void sendAcceptFollowActivity(ForeignUser actor, Actor self, Follow follow){
		self.ensureLocal();
		Accept accept=new Accept();
		accept.actor=new LinkOrObject(self.activityPubID);
		accept.object=new LinkOrObject(follow);
		accept.activityPubID=UriBuilder.local().rawPath(self.getTypeAndIdForURL()).fragment("acceptFollow"+actor.id).build();
		executor.submit(new SendOneActivityRunnable(accept, actor.inbox, self));
	}

	public void sendRejectFriendRequestActivity(User self, ForeignUser target){
		Follow follow=new Follow();
		follow.actor=new LinkOrObject(self.activityPubID);
		follow.object=new LinkOrObject(target.activityPubID);
		Offer offer=new Offer();
		offer.object=new LinkOrObject(follow);
		offer.actor=new LinkOrObject(target.activityPubID);
		Reject reject=new Reject();
		reject.object=new LinkOrObject(offer);
		reject.actor=new LinkOrObject(self.activityPubID);
		reject.activityPubID=URI.create(self.activityPubID+"#rejectFriendReq"+target.id);
		executor.submit(new SendOneActivityRunnable(reject, target.inbox, self));
	}

	public void sendUpdateUserActivity(User user){
		Update update=new Update();
		update.to=Collections.singletonList(new LinkOrObject(ActivityPub.AS_PUBLIC));
		update.activityPubID=URI.create(user.activityPubID+"#updateProfile"+System.currentTimeMillis());
		update.object=new LinkOrObject(user);
		update.actor=new LinkOrObject(user.activityPubID);

		try{
			List<URI> inboxes=UserStorage.getFollowerInboxes(user.id);
			for(URI inbox:inboxes){
				executor.submit(new SendOneActivityRunnable(update, inbox, user));
			}
		}catch(SQLException x){
			LOG.error("Exception while sending Update{Person} for {}", user.activityPubID, x);
		}
	}

	public void sendUpdateGroupActivity(Group group){
		Update update=new Update();
		update.to=Collections.singletonList(new LinkOrObject(ActivityPub.AS_PUBLIC));
		update.activityPubID=URI.create(group.activityPubID+"#updateProfile"+System.currentTimeMillis());
		update.object=new LinkOrObject(group);
		update.actor=new LinkOrObject(group.activityPubID);

		try{
			List<URI> inboxes=GroupStorage.getGroupMemberInboxes(group.id);
			for(URI inbox:inboxes){
				executor.submit(new SendOneActivityRunnable(update, inbox, group));
			}
		}catch(SQLException x){
			LOG.error("Exception while sending Update{Group} for {}", group.activityPubID);
		}
	}

	public void sendLikeActivity(Post post, User user, int likeID) throws SQLException{
		Like like=new Like();
		like.activityPubID=Config.localURI("/activitypub/objects/likes/"+likeID);
		like.actor=new LinkOrObject(user.activityPubID);
		like.object=new LinkOrObject(post.getActivityPubID());
		Set<URI> inboxes=PostStorage.getInboxesForPostInteractionForwarding(post);
		LOG.info("Inboxes: {}", inboxes);
		for(URI inbox:inboxes){
			executor.submit(new SendOneActivityRunnable(like, inbox, user));
		}
	}

	public void sendUndoLikeActivity(Post post, User user, int likeID) throws SQLException{
		Like like=new Like();
		like.activityPubID=Config.localURI("/activitypub/objects/likes/"+likeID);
		like.actor=new LinkOrObject(user.activityPubID);
		like.object=new LinkOrObject(post.getActivityPubID());
		Undo undo=new Undo();
		undo.activityPubID=Config.localURI("/activitypub/objects/likes/"+likeID+"/undo");
		undo.object=new LinkOrObject(like);
		undo.actor=new LinkOrObject(user.activityPubID);
		ActivityPubCache.putUndoneLike(likeID, undo);
		Set<URI> inboxes=PostStorage.getInboxesForPostInteractionForwarding(post);
		for(URI inbox:inboxes){
			executor.submit(new SendOneActivityRunnable(undo, inbox, user));
		}
	}

	public void sendBlockActivity(Actor self, ForeignUser target){
		Block block=new Block();
		block.activityPubID=new UriBuilder(self.activityPubID).fragment("blockUser"+target.id+"_"+System.currentTimeMillis()).build();
		block.actor=new LinkOrObject(self.activityPubID);
		block.object=new LinkOrObject(target.activityPubID);
		executor.submit(new SendOneActivityRunnable(block, target.inbox, self));
	}

	public void sendUndoBlockActivity(Actor self, ForeignUser target){
		Block block=new Block();
		block.activityPubID=new UriBuilder(self.activityPubID).fragment("blockUser"+target.id+"_"+System.currentTimeMillis()).build();
		block.actor=new LinkOrObject(self.activityPubID);
		block.object=new LinkOrObject(target.activityPubID);
		Undo undo=new Undo();
		undo.activityPubID=new UriBuilder(self.activityPubID).fragment("undoBlockUser"+target.id+"_"+System.currentTimeMillis()).build();
		undo.actor=new LinkOrObject(self.activityPubID);
		undo.object=new LinkOrObject(block);
		executor.submit(new SendOneActivityRunnable(undo, target.inbox, self));
	}

	public void sendPollVotes(User self, Poll poll, Actor pollOwner, List<PollOption> chosenOptions, int[] voteIDs){
		if(Config.isLocal(pollOwner.activityPubID))
			return;

		for(int i=0;i<voteIDs.length;i++){
			PollOption opt=chosenOptions.get(i);
			int voteID=voteIDs[i];

			PollVote vote=new PollVote();
			vote.inReplyTo=poll.activityPubID;
			vote.name=opt.text;
			if(opt.activityPubID!=null)
				vote.context=opt.activityPubID;
			vote.activityPubID=new UriBuilder(self.activityPubID).fragment("pollVotes/"+voteID).build();
			vote.attributedTo=self.activityPubID;
			if(poll.anonymous)
				vote.to=Collections.singletonList(new LinkOrObject(pollOwner.activityPubID));
			else
				vote.to=List.of(LinkOrObject.PUBLIC, new LinkOrObject(pollOwner.activityPubID));
			vote.cc=Collections.emptyList();
			Create create=new Create();
			create.activityPubID=new UriBuilder(self.activityPubID).fragment("pollVotes/"+voteID+"/activity").build();
			create.to=vote.to;
			create.cc=vote.cc;
			create.actor=new LinkOrObject(self.activityPubID);
			create.object=new LinkOrObject(vote);
			create.published=Instant.now();

			executor.submit(new SendOneActivityRunnable(create, pollOwner.inbox, self));
		}
	}

	public void sendGroupInvite(int inviteID, User self, Group group, User target){
		if(Config.isLocal(group.activityPubID) && Config.isLocal(target.activityPubID))
			return;

		Invite invite=new Invite();
		invite.activityPubID=Config.localURI("/activitypub/objects/groupInvites/"+inviteID);
		invite.to=List.of(new LinkOrObject(target.activityPubID));
		invite.cc=List.of(new LinkOrObject(group.activityPubID));
		invite.actor=new LinkOrObject(self.activityPubID);
		invite.object=new LinkOrObject(group.activityPubID);

		if(!Objects.equals(group.sharedInbox, target.sharedInbox)){
			if(group instanceof ForeignGroup fg)
				executor.submit(new SendOneActivityRunnable(invite, actorInbox(fg), self));
			if(target instanceof ForeignUser fu)
				executor.submit(new SendOneActivityRunnable(invite, actorInbox(fu), self));
		}else{
			executor.submit(new SendOneActivityRunnable(invite, target.sharedInbox, self));
		}
	}

	public void sendRejectGroupInvite(User self, ForeignGroup group, int invitationLocalID, URI invitationID){
		Invite invite=new Invite();
		invite.activityPubID=invitationID;
		invite.to=List.of(new LinkOrObject(self.activityPubID));
		invite.cc=List.of(new LinkOrObject(group.activityPubID));
		invite.object=new LinkOrObject(group.activityPubID);

		Reject reject=new Reject();
		reject.activityPubID=new UriBuilder(self.activityPubID).fragment("rejectGroupInvite"+invitationLocalID).build();
		reject.to=List.of(new LinkOrObject(group.activityPubID));
		reject.actor=new LinkOrObject(self.activityPubID);
		reject.object=new LinkOrObject(invite);

		executor.submit(new SendOneActivityRunnable(reject, actorInbox(group), self));
	}

	public void sendRejectFollowGroup(ForeignUser user, Group group, boolean tentative){
		Join join=new Join(tentative);
		join.actor=new LinkOrObject(user.activityPubID);
		join.object=new LinkOrObject(group.activityPubID);

		Reject reject=new Reject();
		reject.activityPubID=new UriBuilder(group.activityPubID).fragment("rejectJoin"+user.id+"_"+rand()).build();
		reject.to=List.of(new LinkOrObject(user.activityPubID));
		reject.actor=new LinkOrObject(group.activityPubID);
		reject.object=new LinkOrObject(join);

		executor.submit(new SendOneActivityRunnable(reject, actorInbox(user), group));
	}

	public void sendUndoGroupInvite(ForeignUser user, Group group, int invitationLocalID, URI invitationID){
		Invite invite=new Invite();
		invite.activityPubID=invitationID;
		invite.to=List.of(new LinkOrObject(user.activityPubID));
		invite.cc=List.of(new LinkOrObject(group.activityPubID));
		invite.object=new LinkOrObject(group.activityPubID);

		Undo undo=new Undo();
		undo.activityPubID=new UriBuilder(group.activityPubID).fragment("undoGroupInvite"+invitationLocalID).build();
		undo.to=List.of(new LinkOrObject(user.activityPubID));
		undo.actor=new LinkOrObject(group.activityPubID);
		undo.object=new LinkOrObject(invite);

		executor.submit(new SendOneActivityRunnable(undo, actorInbox(user), group));
	}

	public void sendAddUserToGroupActivity(User user, Group group, boolean tentative){
		group.ensureLocal();
		Add add=new Add();
		add.activityPubID=Config.localURI("/groups/"+group.id+"#addUser"+user.id+"_"+rand());
		ActivityPubCollection target=new ActivityPubCollection(false);
		target.activityPubID=Config.localURI("/groups/"+group.id+"/"+(tentative ? "tentativeMembers" : "members"));
		target.attributedTo=group.activityPubID;
		add.target=new LinkOrObject(target);
		if(group.isEvent())
			add.to=List.of(new LinkOrObject(Config.localURI("/groups/"+group.id+"/members")),
					new LinkOrObject(Config.localURI("/groups/"+group.id+"/tentativeMembers")));
		else
			add.to=List.of(new LinkOrObject(Config.localURI("/groups/"+group.id+"/members")));
		add.object=new LinkOrObject(user.activityPubID);

		try{
			for(URI inbox:GroupStorage.getGroupMemberInboxes(group.id)){
				executor.submit(new SendOneActivityRunnable(add, inbox, group));
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void sendRemoveUserFromGroupActivity(User user, Group group, boolean tentative){
		group.ensureLocal();
		Remove remove=new Remove();
		remove.activityPubID=Config.localURI("/groups/"+group.id+"#removeUser"+user.id+"_"+rand());
		ActivityPubCollection target=new ActivityPubCollection(false);
		target.activityPubID=Config.localURI("/groups/"+group.id+"/"+(tentative ? "tentativeMembers" : "members"));
		target.attributedTo=group.activityPubID;
		remove.target=new LinkOrObject(target);
		if(group.isEvent())
			remove.to=List.of(new LinkOrObject(Config.localURI("/groups/"+group.id+"/members")),
					new LinkOrObject(Config.localURI("/groups/"+group.id+"/tentativeMembers")));
		else
			remove.to=List.of(new LinkOrObject(Config.localURI("/groups/"+group.id+"/members")));
		remove.object=new LinkOrObject(user.activityPubID);

		try{
			for(URI inbox:GroupStorage.getGroupMemberInboxes(group.id)){
				executor.submit(new SendOneActivityRunnable(remove, inbox, group));
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void sendViolationReport(int reportID, String comment, List<URI> objectIDs, Actor targetActor){
		Flag flag=new Flag();
		flag.activityPubID=Config.localURI("/activitypub/objects/reports/"+reportID);
		flag.actor=new LinkOrObject(ServiceActor.getInstance().activityPubID);
		flag.object=objectIDs;
		flag.content=comment;
		executor.submit(new SendOneActivityRunnable(flag, actorInbox(targetActor), ServiceActor.getInstance()));
	}

	public synchronized Future<List<Post>> fetchReplyThread(NoteOrQuestion post){
		return fetchingReplyThreads.computeIfAbsent(post.activityPubID, (uri)->executor.submit(new FetchReplyThreadRunnable(post)));
	}

	public synchronized Future<List<Post>> fetchReplyThreadAndThen(NoteOrQuestion post, Consumer<List<Post>> action){
		afterFetchReplyThreadActions.computeIfAbsent(post.activityPubID, (uri)->new ArrayList<>()).add(action);
		return fetchReplyThread(post);
	}

	public synchronized Future<Post> fetchAllReplies(Post post){
		return fetchingAllReplies.computeIfAbsent(post.getActivityPubID(), (uri)->executor.submit(new FetchAllRepliesTask(post)));
	}

	/**
	 * Fetch and store actor's collections that are related to relationships.
	 * For users, that's friends. For groups, that's members and tentative members.
	 * @param actor the remote actor
	 */
	public synchronized void fetchActorRelationshipCollections(Actor actor){
		LOG.info("Fetching relationship collections for actor {}", actor.activityPubID);
		actor.ensureRemote();
		if(fetchingRelationshipCollectionsActors.contains(actor.activityPubID)){
			LOG.trace("Another fetch is already in progress for {}", actor.activityPubID);
			return;
		}
		fetchingRelationshipCollectionsActors.add(actor.activityPubID);
		executor.submit(new FetchActorRelationshipCollectionsTask(actor));
	}

	/**
	 * Fetch and store actor's collections that are related to its content.
	 * Currently, that's only the wall with all comments.
	 * @param actor the remote actor
	 */
	public synchronized void fetchActorContentCollections(Actor actor){
		LOG.info("Fetching content collections for actor {}", actor.activityPubID);
		actor.ensureRemote();
		if(fetchingContentCollectionsActors.contains(actor.activityPubID)){
			LOG.trace("Another fetch is already in progress for {}", actor.activityPubID);
			return;
		}
		fetchingContentCollectionsActors.add(actor.activityPubID);
		executor.submit(new FetchActorContentCollectionsTask(actor));
	}

	private class SendOneActivityRunnable implements Runnable{
		private Activity activity;
		private URI destination;
		private Actor actor;
		private int retryAttempt;

		public SendOneActivityRunnable(Activity activity, URI destination, Actor actor){
			this.activity=activity;
			this.destination=destination;
			this.actor=actor;
		}

		public SendOneActivityRunnable(Activity activity, URI destination, Actor actor, int retryAttempt){
			this(activity, destination, actor);
			this.retryAttempt=retryAttempt;
		}

		@Override
		public void run(){
			try{
				ActivityPub.postActivity(destination, activity, actor, context, retryAttempt>0);
			}catch(Exception x){
				LOG.error("Exception while sending activity", x);
				if(!(x instanceof FederationException)){
					ActivityDeliveryRetry retry=new ActivityDeliveryRetry(activity, destination, actor, retryAttempt+1);
					if(retry.needMoreAttempts()){
						retryExecutor.schedule(new RetryActivityRunnable(retry), retry.getDelayForThisAttempt(), TimeUnit.MILLISECONDS);
					}
				}
			}
		}
	}

	private class SendActivitySequenceRunnable implements Runnable{
		private List<Activity> activities;
		private URI destination;
		private User user;

		public SendActivitySequenceRunnable(List<Activity> activities, URI destination, User user){
			this.activities=activities;
			this.destination=destination;
			this.user=user;
		}

		@Override
		public void run(){
			for(Activity activity:activities){
				try{
					ActivityPub.postActivity(destination, activity, user, context, false);
				}catch(Exception x){
					LOG.error("Exception while sending activity", x);
					if(!(x instanceof FederationException)){
						ActivityDeliveryRetry retry=new ActivityDeliveryRetry(activity, destination, user, 1);
						if(retry.needMoreAttempts()){
							retryExecutor.schedule(new RetryActivityRunnable(retry), retry.getDelayForThisAttempt(), TimeUnit.MILLISECONDS);
						}
					}
				}
			}
		}
	}

	private class ForwardOneActivityRunnable implements Runnable{
		private String activity;
		private URI destination;
		private User user;

		public ForwardOneActivityRunnable(String activity, URI destination, User user){
			this.activity=activity;
			this.destination=destination;
			this.user=user;
		}

		@Override
		public void run(){
			try{
				ActivityPub.postActivity(destination, activity, user, context);
			}catch(Exception x){
				LOG.error("Exception while forwarding activity", x);
			}
		}
	}

	private class FetchReplyThreadRunnable implements Callable<List<Post>>{
		private final LinkedList<NoteOrQuestion> thread=new LinkedList<>();
		private final Set<URI> seenPosts=new HashSet<>();
		private final NoteOrQuestion initialPost;

		public FetchReplyThreadRunnable(NoteOrQuestion post){
			thread.add(post);
			initialPost=post;
		}

		@Override
		public List<Post> call() throws Exception{
			LOG.debug("Started fetching parent thread for post {}", initialPost.activityPubID);
			seenPosts.add(initialPost.activityPubID);
			while(thread.get(0).inReplyTo!=null){
				NoteOrQuestion post=context.getObjectLinkResolver().resolve(thread.get(0).inReplyTo, NoteOrQuestion.class, true, false, false, (JsonObject) null, true);
				if(seenPosts.contains(post.activityPubID)){
					LOG.warn("Already seen post {} while fetching parent thread for {}", post.activityPubID, initialPost.activityPubID);
					throw new IllegalStateException("Reply thread contains a loop of links");
				}
				seenPosts.add(post.activityPubID);
				thread.add(0, post);
			}
			NoteOrQuestion topLevel=thread.get(0);
			final ArrayList<Post> realThread=new ArrayList<>();
			Post parent=null;
			for(NoteOrQuestion noq:thread){
				Post p=noq.asNativePost(context);

				if(p.id!=0){
					realThread.add(p);
					parent=p;
					continue;
				}
				context.getWallController().loadAndPreprocessRemotePostMentions(p, noq);
				PostStorage.putForeignWallPost(p);
				NotificationUtils.putNotificationsForPost(p, parent);
				realThread.add(p);
				parent=p;
			}
			LOG.info("Done fetching parent thread for post {}", topLevel.activityPubID);
			synchronized(ActivityPubWorker.this){
				fetchingReplyThreads.remove(initialPost.activityPubID);
				List<Consumer<List<Post>>> actions=afterFetchReplyThreadActions.remove(initialPost.activityPubID);
				if(actions!=null){
					for(Consumer<List<Post>> action:actions){
						executor.submit(()->action.accept(realThread));
					}
				}
				return realThread;
			}
		}
	}

	private class FetchAllRepliesTask extends RecursiveTask<Post>{
		protected Post post;
		/**
		 * This keeps track of all the posts we've seen in this comment thread, to prevent a DoS via infinite recursion.
		 * NB: used from multiple threads simultaneously
		 */
		protected final Set<URI> seenPosts;

		public FetchAllRepliesTask(Post post, Set<URI> seenPosts){
			this.post=post;
			this.seenPosts=seenPosts;
		}

		public FetchAllRepliesTask(Post post){
			this(post, new HashSet<>());
			if(post.getReplyLevel()>0)
				throw new IllegalArgumentException("This constructor is only for top-level posts");
		}

		@Override
		protected Post compute(){
			LOG.debug("Started fetching full reply tree for post {}", post.getActivityPubID());
			try{
				if(post.activityPubReplies==null){
					if(post.isLocal()){
//						post.repliesObjects=PostStorage.getRepliesExact(post.getReplyKeyForReplies(), Integer.MAX_VALUE, 1000).list;
					}else{
						return post;
					}
				}else{
					Actor owner=context.getWallController().getPostAuthorAndOwner(post).owner();

					ActivityPubCollection collection;
//					if(post.replies.link!=null){
						collection=context.getObjectLinkResolver().resolve(post.activityPubReplies, ActivityPubCollection.class, true, false, false, owner, true);
						collection.validate(post.getActivityPubID(), "replies");
//					}else if(post.replies.object instanceof ActivityPubCollection){
//						collection=(ActivityPubCollection) post.replies.object;
//					}else{
//						LOG.warn("Post {} doesn't have a replies collection", post.activityPubID);
//						return post;
//					}
					LOG.trace("collection: {}", collection);
					if(collection.first==null){
						LOG.warn("Post {} doesn't have replies.first", post.getActivityPubID());
						return post;
					}
					CollectionPage page;
					if(collection.first.link!=null){
						page=context.getObjectLinkResolver().resolve(collection.first.link, CollectionPage.class, true, false, false, owner, false);
						page.validate(post.getActivityPubID(), "replies.first");
					}else if(collection.first.object instanceof CollectionPage){
						page=(CollectionPage) collection.first.object;
					}else{
						LOG.warn("Post {} doesn't have a correct CollectionPage in replies.first", post.getActivityPubID());
						return post;
					}
					LOG.trace("first page: {}", page);
					if(page.items!=null && !page.items.isEmpty()){
						doOneCollectionPage(page.items);
					}
					while(page.next!=null){
						LOG.trace("getting next page: {}", page.next);
						try{
							page=context.getObjectLinkResolver().resolve(page.next, CollectionPage.class, true, false, false, owner, false);
							if(page.items==null){ // you're supposed to not return the "next" field when there are no more pages, but mastodon still does...
								LOG.debug("done fetching replies because page.items is empty");
								break;
							}
							doOneCollectionPage(page.items);
						}catch(ObjectNotFoundException x){
							LOG.warn("Failed to get replies collection page for post {}", post.getActivityPubID());
							return post;
						}
					}
				}
			}catch(Exception x){
				completeExceptionally(x);
			}
			if(post.getReplyLevel()==0){
				synchronized(ActivityPubWorker.this){
					fetchingAllReplies.remove(post.getActivityPubID());
					return post;
				}
			}
			return post;
		}

		private void doOneCollectionPage(List<LinkOrObject> page) throws Exception{
			ArrayList<FetchAllRepliesTask> subtasks=new ArrayList<>();
			for(LinkOrObject item:page){
				Post post;
				if(item.link!=null){
					synchronized(seenPosts){
						if(seenPosts.contains(item.link)){
							LOG.warn("Already seen post {}", item.link);
							continue;
						}
						if(seenPosts.size()>=MAX_COMMENTS){
							LOG.warn("Reached limit of {} on comment thread length. Stopping.", MAX_COMMENTS);
							return;
						}
						seenPosts.add(item.link);
					}
					FetchPostAndRepliesTask subtask=new FetchPostAndRepliesTask(item.link, this.post, seenPosts);
					subtasks.add(subtask);
					subtask.fork();
				}else if(item.object instanceof NoteOrQuestion noq){
					synchronized(seenPosts){
						if(seenPosts.contains(item.object.activityPubID)){
							LOG.warn("Already seen post {}", item.object.activityPubID);
							continue;
						}
						if(seenPosts.size()>=MAX_COMMENTS){
							LOG.warn("Reached limit of {} on comment thread length. Stopping.", MAX_COMMENTS);
							return;
						}
						seenPosts.add(item.object.activityPubID);
					}
					post=noq.asNativePost(context);
					PostStorage.putForeignWallPost(post);
					LOG.trace("got post: {}", post);
					FetchAllRepliesTask subtask=new FetchAllRepliesTask(post, seenPosts);
					subtasks.add(subtask);
					subtask.fork();
				}else{
					LOG.warn("reply object isn't a post: {}", item.object);
				}
			}
			for(FetchAllRepliesTask task:subtasks){
				try{
//					post.repliesObjects.add(task.join());
					task.join();
				}catch(Exception x){
					LOG.warn("error fetching reply", x);
				}
			}
		}
	}

	private class FetchPostAndRepliesTask extends FetchAllRepliesTask{
		private URI postID;
		private Post parentPost;

		public FetchPostAndRepliesTask(URI postID, Post parentPost, Set<URI> seenPosts){
			super(null, seenPosts);
			this.postID=postID;
			this.parentPost=parentPost;
		}

		@Override
		protected Post compute(){
			try{
				LOG.trace("Fetching remote reply from {}", postID);
				post=context.getObjectLinkResolver().resolveNative(postID, Post.class, true, false, false, context.getWallController().getPostAuthorAndOwner(parentPost).owner(), true);
//				post.setParent(parentPost);
//				post.storeDependencies(context);
				PostStorage.putForeignWallPost(post);
			}catch(Exception x){
				completeExceptionally(x);
			}
			return super.compute();
		}
	}

	private class FetchActorRelationshipCollectionsTask extends RecursiveAction{
		private final Actor actor;

		private FetchActorRelationshipCollectionsTask(Actor actor){
			this.actor=actor;
		}

		@Override
		protected void compute(){
			List<ForkJoinTask<?>> tasks=new ArrayList<>();
			// TODO also sync removed items
			if(actor instanceof ForeignUser user){
				if(user.getFriendsURL()!=null){
					tasks.add(new FetchUserFriendsDirectlyTask(user));
					if(user.getGroupsURL()!=null){
						tasks.add(new FetchUserGroupsDirectlyTask(user));
					}
				}else{
					tasks.add(new FetchUserFriendsAndGroupsViaFollowsTask(user));
				}
			}else if(actor instanceof ForeignGroup group){
				tasks.add(new FetchGroupMembersTask(group, false));
				if(group.isEvent() && group.tentativeMembers!=null)
					tasks.add(new FetchGroupMembersTask(group, true));
			}
			try{
				invokeAll(tasks);
			}catch(Throwable x){
				LOG.warn("Error fetching relationship collections for {}", actor.activityPubID, x);
			}
			synchronized(ActivityPubWorker.this){
				fetchingRelationshipCollectionsActors.remove(actor.activityPubID);
			}
			LOG.info("Done fetching relationship collections for {}", actor.activityPubID);
		}
	}

	private class FetchActorContentCollectionsTask extends RecursiveAction{
		private final Actor actor;

		private FetchActorContentCollectionsTask(Actor actor){
			this.actor=actor;
		}

		@Override
		protected void compute(){
			List<ForkJoinTask<?>> tasks=new ArrayList<>();
			if(actor.hasWall()){
				tasks.add(new FetchActorWallTask(actor));
			}
			try{
				invokeAll(tasks);
			}catch(Throwable x){
				LOG.warn("Error fetching content collections for {}", actor.activityPubID, x);
			}
			synchronized(ActivityPubWorker.this){
				fetchingContentCollectionsActors.remove(actor.activityPubID);
			}
			LOG.info("Done fetching content collections for {}", actor.activityPubID);
		}
	}

	/**
	 * Base class for tasks that deal with collections. Handles paginating through a collection.
	 */
	private abstract class ForwardPaginatingCollectionTask extends RecursiveAction{
		protected final URI collectionID;
		protected ActivityPubCollection collection;
		protected int totalItems, processedItems;
		protected int maxItems=Integer.MAX_VALUE;

		private ForwardPaginatingCollectionTask(URI collectionID){
			this.collectionID=Objects.requireNonNull(collectionID);
		}

		private ForwardPaginatingCollectionTask(ActivityPubCollection collection){
			this.collection=collection;
			collectionID=collection.activityPubID;
		}

		@Override
		protected void compute(){
			if(collection==null){
				LOG.trace("Fetching collection {}", collectionID);
				collection=context.getObjectLinkResolver().resolve(collectionID, ActivityPubCollection.class, true, false, false);
			}
			totalItems=Math.min(collection.totalItems, maxItems);
			onCollectionLoaded();
			if(collection.first==null)
				throw new FederationException("collection.first is not present");
			if(collection.first.object!=null)
				processCollectionPage(collection.first.requireObject());
			else
				loadNextCollectionPage(collection.first.link);
		}

		private void loadNextCollectionPage(URI id){
			LOG.trace("Fetching page {} for collection {}", id, collectionID);
			CollectionPage page=context.getObjectLinkResolver().resolve(id, CollectionPage.class, true, false, false);
			processCollectionPage(page);
		}

		private void processCollectionPage(CollectionPage page){
			if(page.items==null || page.items.isEmpty()){
				LOG.trace("Finished processing collection {} because items array was null or empty", collectionID);
				return;
			}
			doOneCollectionPage(page);
			processedItems+=page.items.size();
			if(totalItems>=0 && processedItems>=totalItems){
				LOG.trace("Finished processing collection {} because item count limit {} was reached", collectionID, totalItems);
				return;
			}
			if(page.next!=null){
				loadNextCollectionPage(page.next);
			}else{
				LOG.trace("Finished processing collection {} because there are no next pages", collectionID);
			}
		}

		protected abstract void doOneCollectionPage(CollectionPage page);

		protected void onCollectionLoaded(){}
	}

	/**
	 * Fetches the user's friends collection directly, assuming there is one
	 */
	private class FetchUserFriendsDirectlyTask extends ForwardPaginatingCollectionTask{
		private final ForeignUser user;

		private FetchUserFriendsDirectlyTask(ForeignUser user){
			super(Objects.requireNonNull(user.getFriendsURL(), "user must have a friends collection"));
			this.user=user;
			maxItems=MAX_FRIENDS;
		}

		protected void doOneCollectionPage(CollectionPage page){
			invokeAll(page.items.stream()
					.filter(lo->lo.link!=null && !Config.isLocal(lo.link))
					.map(lo->new FetchAndStoreOneUserFolloweeTask(user, lo.link, ForeignUser.class))
					.toList());
		}
	}

	/**
	 * Fetches the user's friends collection directly, assuming there is one
	 */
	private class FetchUserGroupsDirectlyTask extends ForwardPaginatingCollectionTask{
		private final ForeignUser user;

		private FetchUserGroupsDirectlyTask(ForeignUser user){
			super(Objects.requireNonNull(user.getGroupsURL(), "user must have a groups collection"));
			this.user=user;
			maxItems=MAX_FRIENDS;
		}

		protected void doOneCollectionPage(CollectionPage page){
			invokeAll(page.items.stream()
					.filter(lo->lo.link!=null && !Config.isLocal(lo.link))
					.map(lo->new FetchAndStoreOneUserFolloweeTask(user, lo.link, ForeignGroup.class))
					.toList());
		}
	}

	/**
	 * Fetches the user's friends and groups by intersecting followers and following
	 */
	private class FetchUserFriendsAndGroupsViaFollowsTask extends RecursiveAction{
		private final ForeignUser user;

		private FetchUserFriendsAndGroupsViaFollowsTask(ForeignUser user){
			this.user=user;
		}

		@Override
		protected void compute(){
			if(user.followers==null || user.following==null)
				throw new FederationException("The user must have followers and following collections");

			ActivityPubCollection followers=context.getObjectLinkResolver().resolve(user.followers, ActivityPubCollection.class, true, false, false);
			ActivityPubCollection following=context.getObjectLinkResolver().resolve(user.following, ActivityPubCollection.class, true, false, false);
			LOG.trace("Fetch followers/following: collection sizes: {} followers, {} following", followers.totalItems, following.totalItems);

			if(followers.totalItems<=0 || following.totalItems<=0){
				LOG.debug("Can't proceed because collection sizes are not known");
				return;
			}
			if(followers.totalItems>MAX_FRIENDS && following.totalItems>MAX_FRIENDS){
				LOG.debug("Can't proceed because both followers and following exceed the limit of {}", MAX_FRIENDS);
				return;
			}

			Set<URI> first=new HashSet<>();
			new FetchCollectionIntoSetTask(followers.totalItems>following.totalItems ? following : followers, first).fork().join();
			Set<URI> mutualFollows=new HashSet<>();
			new FilterCollectionAgainstSetTask(followers.totalItems>following.totalItems ? followers : following, first, mutualFollows).fork().join();
			List<ForkJoinTask<?>> tasks=new ArrayList<>();
			for(URI uri:mutualFollows){
				if(!Config.isLocal(uri)){
					tasks.add(new FetchAndStoreOneUserFolloweeTask(user, uri, Actor.class));
				}
				if(tasks.size()==ACTORS_BATCH_SIZE){
					invokeAll(tasks);
					tasks.clear();
				}
			}
			if(!tasks.isEmpty())
				invokeAll(tasks);
		}
	}

	private class FetchGroupMembersTask extends ForwardPaginatingCollectionTask{
		private final ForeignGroup group;
		private final boolean tentative;

		private FetchGroupMembersTask(ForeignGroup group, boolean tentative){
			super(tentative ? group.tentativeMembers : group.members);
			this.group=group;
			this.tentative=tentative;
		}

		@Override
		protected void doOneCollectionPage(CollectionPage page){
			invokeAll(page.items.stream()
					.filter(lo->lo.link!=null && !Config.isLocal(lo.link))
					.map(lo->new FetchAndStoreOneGroupMemberTask(group, lo.link, tentative))
					.toList());
		}

		@Override
		protected void onCollectionLoaded(){
			try{
				GroupStorage.setMemberCount(group, totalItems, tentative);
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
		}
	}

	private class FetchCollectionIntoSetTask extends ForwardPaginatingCollectionTask{
		private final Set<URI> set;

		private FetchCollectionIntoSetTask(ActivityPubCollection collection, Set<URI> set){
			super(collection);
			this.set=set;
		}

		@Override
		protected void doOneCollectionPage(CollectionPage page){
			for(LinkOrObject lo:page.items){
				if(lo.link!=null)
					set.add(lo.link);
			}
		}
	}

	private class FilterCollectionAgainstSetTask extends ForwardPaginatingCollectionTask{
		private final Set<URI> filter;
		private final Set<URI> result;

		private FilterCollectionAgainstSetTask(ActivityPubCollection collection, Set<URI> filter, Set<URI> result){
			super(collection);
			this.filter=filter;
			this.result=result;
		}

		@Override
		protected void doOneCollectionPage(CollectionPage page){
			for(LinkOrObject lo:page.items){
				if(lo.link!=null && filter.contains(lo.link))
					result.add(lo.link);
			}
		}
	}

	private class FetchAndStoreOneUserFolloweeTask extends RecursiveAction{
		private final ForeignUser user;
		private final URI targetActorID;
		private final Class<? extends Actor> type;

		private FetchAndStoreOneUserFolloweeTask(ForeignUser user, URI targetActorID, Class<? extends Actor> type){
			this.user=user;
			this.targetActorID=targetActorID;
			this.type=type;
		}

		@Override
		protected void compute(){
			try{
				Actor target=context.getObjectLinkResolver().resolve(targetActorID, type, true, false, false);
				if(target instanceof ForeignUser targetUser){
					if(targetUser.getFriendsURL()!=null)
						context.getObjectLinkResolver().ensureObjectIsInCollection(targetUser, targetUser.getFriendsURL(), user.activityPubID);
					if(targetUser.id==0)
						context.getObjectLinkResolver().storeOrUpdateRemoteObject(targetUser);
					context.getFriendsController().storeFriendship(user, targetUser);
				}else if(target instanceof ForeignGroup targetGroup){
					if(targetGroup.isEvent())
						return;
					if(targetGroup.members!=null)
						context.getObjectLinkResolver().ensureObjectIsInCollection(targetGroup, targetGroup.members, user.activityPubID);
					if(targetGroup.id==0)
						context.getObjectLinkResolver().storeOrUpdateRemoteObject(targetGroup);
					context.getGroupsController().joinGroup(targetGroup, user, false, true);
				}
			}catch(Exception x){
				LOG.debug("Error fetching remote actor {}", targetActorID, x);
			}
		}
	}

	private class FetchAndStoreOneGroupMemberTask extends RecursiveAction{
		private final ForeignGroup group;
		private final URI userID;
		private final boolean tentative;

		private FetchAndStoreOneGroupMemberTask(ForeignGroup group, URI userID, boolean tentative){
			this.group=group;
			this.userID=userID;
			this.tentative=tentative;
		}

		@Override
		protected void compute(){
			try{
				ForeignUser user=context.getObjectLinkResolver().resolve(userID, ForeignUser.class, true, false, false);
				if(user.getGroupsURL()!=null)
					context.getObjectLinkResolver().ensureObjectIsInCollection(user, user.getGroupsURL(), group.activityPubID);
				if(user.id==0)
					context.getObjectLinkResolver().storeOrUpdateRemoteObject(user);
				context.getGroupsController().joinGroup(group, user, tentative, true);
			}catch(Exception x){
				LOG.debug("Error fetching remote user {}", userID, x);
			}
		}
	}

	private class FetchActorWallTask extends ForwardPaginatingCollectionTask{
		private final Actor actor;

		private FetchActorWallTask(Actor actor){
			super(actor.getWallURL());
			this.actor=actor;
			maxItems=MAX_COMMENTS;
		}

		@Override
		protected void doOneCollectionPage(CollectionPage page){
			ArrayList<ProcessWallPostTask> tasks=new ArrayList<>();
			for(LinkOrObject lo:page.items){
				try{
					if(lo.object instanceof NoteOrQuestion post){
						if(post.inReplyTo==null)
							  tasks.add(new ProcessWallPostTask(post, actor));
					}else if(lo.link!=null){
						tasks.add(new FetchAndProcessWallPostTask(actor, lo.link));
					}
				}catch(Exception x){
					LOG.debug("Error processing post {}", lo);
				}
			}
			invokeAll(tasks);
		}
	}

	private class ProcessWallPostTask extends RecursiveAction{
		protected NoteOrQuestion post;
		protected final Actor owner;

		private ProcessWallPostTask(NoteOrQuestion post, Actor owner){
			this.post=post;
			this.owner=owner;
		}

		private ProcessWallPostTask(Actor owner){
			this.owner=owner;
		}

		@Override
		protected void compute(){
			try{
				Post nativePost=post.asNativePost(context);
				context.getObjectLinkResolver().storeOrUpdateRemoteObject(nativePost);
				new FetchAllRepliesTask(nativePost).fork().join();
			}catch(Exception x){
				LOG.debug("Error processing post {}", post.activityPubID, x);
			}
		}
	}

	private class FetchAndProcessWallPostTask extends ProcessWallPostTask{
		private final URI postID;

		private FetchAndProcessWallPostTask(Actor owner, URI postID){
			super(owner);
			this.postID=postID;
		}

		@Override
		protected void compute(){
			try{
				post=context.getObjectLinkResolver().resolve(postID, NoteOrQuestion.class, true, false, false, owner, false);
				if(post.inReplyTo!=null)
					return;
			}catch(Exception x){
				LOG.debug("Error fetching post {}", postID, x);
			}
			super.compute();
		}
	}

	private class RetryActivityRunnable implements Runnable{
		private final ActivityDeliveryRetry retry;

		private RetryActivityRunnable(ActivityDeliveryRetry retry){
			this.retry=retry;
		}

		@Override
		public void run(){
			LOG.info("Retrying activity delivery to {}, attempt {}", retry.inbox, retry.attemptNumber);
			executor.submit(new SendOneActivityRunnable(retry.activity, retry.inbox, retry.actor, retry.attemptNumber));
		}
	}

	private record ActivityDeliveryRetry(Activity activity, URI inbox, Actor actor, int attemptNumber){
		public long getDelayForThisAttempt(){
			return switch(attemptNumber){
				case 1 -> 30_000; // 30 seconds
				case 2 -> 60_000; // 1 minute
				case 3 -> 5*60_000; // 5 minutes
				case 4 -> 600_000; // 10 minutes
				case 5 -> 3*600_000; // 30 minutes
				case 6 -> 3600_000; // 1 hour
				case 7 -> 3*3600_000; // 3 hours
				case 8 -> 6*3600_000; // 6 hours
				case 9 -> 12*3600_000; // 12 hours
				default -> throw new IllegalStateException();
			};
		}

		public boolean needMoreAttempts(){
			return attemptNumber<10;
		}
	}
}
