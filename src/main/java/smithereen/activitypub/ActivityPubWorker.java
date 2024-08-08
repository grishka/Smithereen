package smithereen.activitypub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.Note;
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
import smithereen.activitypub.objects.activities.Read;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.activitypub.objects.activities.Update;
import smithereen.activitypub.tasks.FetchActorContentCollectionsTask;
import smithereen.activitypub.tasks.FetchActorRelationshipCollectionsTask;
import smithereen.activitypub.tasks.FetchAllRepliesTask;
import smithereen.activitypub.tasks.FetchReplyThreadRunnable;
import smithereen.activitypub.tasks.FetchRepostChainTask;
import smithereen.activitypub.tasks.ForwardOneActivityRunnable;
import smithereen.activitypub.tasks.RetryActivityRunnable;
import smithereen.activitypub.tasks.SendActivitySequenceRunnable;
import smithereen.activitypub.tasks.SendOneActivityRunnable;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.MailMessage;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.Poll;
import smithereen.model.PollOption;
import smithereen.model.PollVote;
import smithereen.model.Post;
import smithereen.model.PrivacySetting;
import smithereen.model.Server;
import smithereen.model.User;
import smithereen.model.UserPrivacySettingKey;
import smithereen.storage.GroupStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import smithereen.util.UriBuilder;
import spark.utils.StringUtils;

public class ActivityPubWorker{
	private static final Logger LOG=LoggerFactory.getLogger(ActivityPubWorker.class);
	/**
	 * Will not fetch more than this many comments
	 */
	public static final int MAX_COMMENTS=1000;
	/**
	 * Will not fetch more than this many friends (or followers/following if there's no friends collection)
	 */
	public static final int MAX_FRIENDS=10_000;
	public static final int ACTORS_BATCH_SIZE=25;
	public static final int MAX_REPOST_DEPTH=20;

	private final ExecutorService executor=Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("ActivityPubWorker-", 0).factory());
	private final ScheduledExecutorService retryExecutor=Executors.newSingleThreadScheduledExecutor();
	private final Random rand=new Random();

	// These must be accessed from synchronized(this)
	private final HashMap<URI, Future<List<Post>>> fetchingReplyThreads=new HashMap<>();
	private final HashMap<URI, List<Consumer<List<Post>>>> afterFetchReplyThreadActions=new HashMap<>();
	private final HashMap<URI, Future<Post>> fetchingAllReplies=new HashMap<>();
	private final HashSet<URI> fetchingRelationshipCollectionsActors=new HashSet<>();
	private final HashSet<URI> fetchingContentCollectionsActors=new HashSet<>();
	private final HashMap<URI, Future<List<Post>>> fetchingRepostChains=new HashMap<>();

	private final ApplicationContext context;

	public ActivityPubWorker(ApplicationContext context){
		this.context=context;
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

	private void getInboxesWithPrivacy(Set<URI> inboxes, User owner, PrivacySetting setting) throws SQLException{
		switch(setting.baseRule){
			case EVERYONE -> inboxes.addAll(UserStorage.getFollowerInboxes(owner.id, setting.exceptUsers));
			case FRIENDS, FRIENDS_OF_FRIENDS -> inboxes.addAll(UserStorage.getFriendInboxes(owner.id, setting.exceptUsers));
			case NONE -> context.getUsersController().getUsers(setting.allowUsers).values().stream().map(this::actorInbox).forEach(inboxes::add);
		}
	}

	private long rand(){
		return Math.abs(rand.nextLong());
	}

	public void forwardActivity(String json, User signer, Collection<URI> inboxes, String originatingDomain){
		for(URI inbox:inboxes){
			if(inbox.getHost().equalsIgnoreCase(originatingDomain))
				continue;
			executor.submit(new ForwardOneActivityRunnable(context, json, inbox, signer));
		}
	}

	private Set<URI> getInboxesForPost(Post post) throws SQLException{
		Set<URI> inboxes=new HashSet<>();
		OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(post);
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
			LOG.trace("Inboxes: {}", inboxes);
			for(URI inbox:inboxes){
				SendOneActivityRunnable r=new SendOneActivityRunnable(this, context, activity, inbox, actor);
				if(post.getReplyLevel()==0 && post.authorID!=post.ownerID)
					r.requireFeature(Server.Feature.WALL_POSTS);
				executor.submit(r);
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
				OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(post);

				Add add=new Add();
				add.activityPubID=UriBuilder.local().path("posts", String.valueOf(post.id), "activityAdd").build();
				add.object=new LinkOrObject(post.getActivityPubID());
				add.actor=new LinkOrObject(oaa.owner().activityPubID);
				add.to=List.of(new LinkOrObject(ActivityPub.AS_PUBLIC), new LinkOrObject(oaa.owner().getFollowersURL()), new LinkOrObject(oaa.author().activityPubID));
				add.cc=note.cc;
				ActivityPubCollection target=new ActivityPubCollection(false);
				target.activityPubID=oaa.owner().getWallURL();
				target.attributedTo=oaa.owner().activityPubID;
				add.target=new LinkOrObject(target);

				HashSet<URI> inboxes=new HashSet<>();
				if(oaa.owner() instanceof User user){
					PrivacySetting setting=user.privacySettings.getOrDefault(UserPrivacySettingKey.WALL_OTHERS_POSTS, new PrivacySetting());
					getInboxesWithPrivacy(inboxes, user, setting);
				}else if(oaa.owner() instanceof Group group){
					inboxes.addAll(GroupStorage.getGroupMemberInboxes(group.id));
				}

				if(oaa.author() instanceof ForeignUser fu){
					URI inbox=actorInbox(fu);
					inboxes.add(inbox);
				}

				for(URI inbox:inboxes){
					executor.submit(new SendOneActivityRunnable(this, context, add, inbox, oaa.owner()).requireFeature(Server.Feature.WALL_POSTS));
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
				actor=context.getWallController().getContentAuthorAndOwner(post).owner();
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

		submitActivity(undo, self, target.inbox);
	}

	public void sendRemoveFromFriendsCollectionActivity(User self, User exFriend){
		Remove remove=new Remove();
		remove.activityPubID=new UriBuilder(self.activityPubID).fragment("unfriendUserCollection"+exFriend.id+"_"+rand()).build();
		remove.actor=new LinkOrObject(self.activityPubID);
		remove.object=new LinkOrObject(exFriend.activityPubID);
		remove.target=new LinkOrObject(self.getFriendsURL());
		submitActivityForFollowers(remove, self);
	}

	public void sendAddToFriendsCollectionActivity(User self, User friend){
		Add add=new Add();
		add.activityPubID=new UriBuilder(self.activityPubID).fragment("addFriendUserCollection"+friend.id+"_"+rand()).build();
		add.actor=new LinkOrObject(self.activityPubID);
		add.object=new LinkOrObject(friend.activityPubID);
		add.target=new LinkOrObject(self.getFriendsURL());
		submitActivityForFollowers(add, self);
	}

	public void sendAddToGroupsCollectionActivity(User self, Group group, boolean tentative){
		Add add=new Add();
		add.activityPubID=new UriBuilder(self.activityPubID).fragment("addGroupCollection"+group.id+"_"+rand()).build();
		add.actor=new LinkOrObject(self.activityPubID);
		add.object=new LinkOrObject(group.activityPubID);
		add.target=new LinkOrObject(self.getGroupsURL());
		add.tentative=tentative;
		submitActivityForFollowers(add, self);
	}

	public void sendRemoveFromGroupsCollectionActivity(User self, Group group){
		Remove remove=new Remove();
		remove.activityPubID=new UriBuilder(self.activityPubID).fragment("removeGroupCollection"+group.id+"_"+rand()).build();
		remove.actor=new LinkOrObject(self.activityPubID);
		remove.object=new LinkOrObject(group.activityPubID);
		remove.target=new LinkOrObject(self.getGroupsURL());
		submitActivityForFollowers(remove, self);
	}

	public void sendFollowUserActivity(User self, ForeignUser target){
		Follow follow=new Follow();
		follow.actor=new LinkOrObject(self.activityPubID);
		follow.object=new LinkOrObject(target.activityPubID);
		follow.activityPubID=new UriBuilder(self.activityPubID).fragment("followUser"+target.id+"_"+rand()).build();
		submitActivity(follow, self, target.inbox);
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
		submitActivity(follow, self, target.inbox);
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
		submitActivity(undo, self, target.inbox);
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
			executor.submit(new SendActivitySequenceRunnable(this, context, List.of(follow, offer), target.inbox, self));
		}else{
			submitActivity(follow, self, target.inbox);
		}
	}

	public void sendAcceptFollowActivity(ForeignUser actor, Actor self, Follow follow){
		self.ensureLocal();
		Accept accept=new Accept();
		accept.actor=new LinkOrObject(self.activityPubID);
		accept.object=new LinkOrObject(follow);
		accept.activityPubID=UriBuilder.local().rawPath(self.getTypeAndIdForURL()).fragment("acceptFollow"+actor.id).build();
		submitActivity(accept, self, actor.inbox);
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
		submitActivity(reject, self, target.inbox);
	}

	public void sendUpdateUserActivity(User user){
		Update update=new Update();
		update.to=Collections.singletonList(new LinkOrObject(ActivityPub.AS_PUBLIC));
		update.activityPubID=URI.create(user.activityPubID+"#updateProfile"+System.currentTimeMillis());
		update.object=new LinkOrObject(user);
		update.actor=new LinkOrObject(user.activityPubID);
		submitActivityForFollowers(update, user);
	}

	public void sendUpdateGroupActivity(Group group){
		Update update=new Update();
		update.to=Collections.singletonList(new LinkOrObject(ActivityPub.AS_PUBLIC));
		update.activityPubID=URI.create(group.activityPubID+"#updateProfile"+System.currentTimeMillis());
		update.object=new LinkOrObject(group);
		update.actor=new LinkOrObject(group.activityPubID);
		submitActivityForMembers(update, group);
	}

	public void sendLikeActivity(Post post, User user, int likeID) throws SQLException{
		Like like=new Like();
		like.activityPubID=Config.localURI("/activitypub/objects/likes/"+likeID);
		like.actor=new LinkOrObject(user.activityPubID);
		like.object=new LinkOrObject(post.getActivityPubID());
		submitActivity(like, user, PostStorage.getInboxesForPostInteractionForwarding(post));
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
		submitActivity(undo, user, PostStorage.getInboxesForPostInteractionForwarding(post));
	}

	public void sendBlockActivity(Actor self, ForeignUser target){
		Block block=new Block();
		block.activityPubID=new UriBuilder(self.activityPubID).fragment("blockUser"+target.id+"_"+System.currentTimeMillis()).build();
		block.actor=new LinkOrObject(self.activityPubID);
		block.object=new LinkOrObject(target.activityPubID);
		submitActivity(block, self, target.inbox);
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
		submitActivity(undo, self, target.inbox);
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

			submitActivity(create, self, pollOwner.inbox);
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
				submitActivity(invite, self, fg.inbox);
			if(target instanceof ForeignUser fu)
				submitActivity(invite, self, fu.inbox);
		}else{
			submitActivity(invite, self, actorInbox(target));
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

		submitActivity(reject, self, group.inbox);
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

		submitActivity(reject, group, user.inbox);
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

		submitActivity(undo, group, user.inbox);
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

		submitActivityForMembers(add, group);
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

		submitActivityForMembers(remove, group);
	}

	public void sendViolationReport(int reportID, String comment, List<URI> objectIDs, Actor targetActor){
		Flag flag=new Flag();
		flag.activityPubID=Config.localURI("/activitypub/objects/reports/"+reportID);
		flag.actor=new LinkOrObject(ServiceActor.getInstance().activityPubID);
		flag.object=objectIDs;
		flag.content=comment;
		submitActivity(flag, ServiceActor.getInstance(), targetActor.inbox);
	}

	public void sendDirectMessage(User self, MailMessage msg){
		HashSet<Integer> needUsers=new HashSet<>();
		needUsers.addAll(msg.to);
		needUsers.addAll(msg.cc);
		Map<Integer, User> users=context.getUsersController().getUsers(needUsers);

		Note note=NoteOrQuestion.fromNativeMessage(msg, context);
		Create create=new Create();
		create.actor=new LinkOrObject(self.activityPubID);
		create.object=new LinkOrObject(note);
		create.to=note.to;
		create.cc=note.cc;
		create.activityPubID=new UriBuilder(note.activityPubID).fragment("create").build();
		Set<URI> inboxes=users.values().stream().filter(u->u instanceof ForeignUser).map(this::actorInbox).collect(Collectors.toSet());
		submitActivity(create, self, inboxes);
	}

	public void sendDeleteMessageActivity(User self, MailMessage msg){
		HashSet<Integer> needUsers=new HashSet<>(msg.to);
		if(msg.cc!=null)
			needUsers.addAll(msg.cc);
		Map<Integer, User> users=context.getUsersController().getUsers(needUsers);

		Delete delete=new Delete();
		delete.actor=new LinkOrObject(self.activityPubID);
		delete.object=new LinkOrObject(msg.getActivityPubID());
		delete.to=msg.to.stream().map(id->new LinkOrObject(users.get(id).activityPubID)).toList();
		if(msg.cc!=null && !msg.cc.isEmpty())
			delete.cc=msg.cc.stream().map(id->new LinkOrObject(users.get(id).activityPubID)).toList();
		delete.activityPubID=new UriBuilder(msg.getActivityPubID()).fragment("delete").build();

		Set<URI> inboxes=users.values().stream().filter(u->u instanceof ForeignUser).map(this::actorInbox).collect(Collectors.toSet());
		submitActivity(delete, self, inboxes);
	}

	public void sendReadMessageActivity(User self, MailMessage msg){
		HashSet<Integer> needUsers=new HashSet<>(msg.to);
		needUsers.add(msg.senderID);
		if(msg.cc!=null)
			needUsers.addAll(msg.cc);
		Map<Integer, User> users=context.getUsersController().getUsers(needUsers);

		Read read=new Read();
		read.actor=new LinkOrObject(self.activityPubID);
		read.object=new LinkOrObject(msg.getActivityPubID());
		HashSet<Integer> to=new HashSet<>(msg.to);
		to.add(msg.senderID);
		read.to=to.stream().filter(id->id!=self.id).map(id->new LinkOrObject(users.get(id).activityPubID)).toList();
		if(msg.cc!=null && !msg.cc.isEmpty())
			read.cc=msg.cc.stream().filter(id->id!=self.id).map(id->new LinkOrObject(users.get(id).activityPubID)).toList();
		read.activityPubID=UriBuilder.local().path("activitypub", "objects", "messages", msg.encodedID).fragment("read"+self.id).build();

		Set<URI> inboxes=users.values().stream().filter(u->u instanceof ForeignUser).map(this::actorInbox).collect(Collectors.toSet());
		submitActivity(read, self, inboxes);
	}

	public void sendUserDeleteSelf(User self) throws SQLException{
		Delete del=new Delete();
		del.activityPubID=new UriBuilder(self.activityPubID).fragment("deleteSelf").build();
		del.actor=new LinkOrObject(self.activityPubID);
		del.object=new LinkOrObject(self.activityPubID);
		submitActivityForFollowers(del, self);
	}

	public synchronized Future<List<Post>> fetchReplyThread(NoteOrQuestion post){
		return fetchingReplyThreads.computeIfAbsent(post.activityPubID, (uri)->executor.submit(new FetchReplyThreadRunnable(this, afterFetchReplyThreadActions, context, fetchingReplyThreads, post)));
	}

	public synchronized Future<List<Post>> fetchReplyThreadAndThen(NoteOrQuestion post, Consumer<List<Post>> action){
		afterFetchReplyThreadActions.computeIfAbsent(post.activityPubID, (uri)->new ArrayList<>()).add(action);
		return fetchReplyThread(post);
	}

	public synchronized Future<Post> fetchAllReplies(Post post){
		return fetchingAllReplies.computeIfAbsent(post.getActivityPubID(), (uri)->executor.submit(new FetchAllRepliesTask(this, context, fetchingAllReplies, post)));
	}

	/**
	 * Fetch and store actor's collections that are related to relationships.
	 * For users, that's friends. For groups, that's members and tentative members.
	 * @param actor the remote actor
	 */
	public synchronized void fetchActorRelationshipCollections(Actor actor){
		LOG.debug("Fetching relationship collections for actor {}", actor.activityPubID);
		actor.ensureRemote();
		if(fetchingRelationshipCollectionsActors.contains(actor.activityPubID)){
			LOG.trace("Another fetch is already in progress for relationship collections of {}", actor.activityPubID);
			return;
		}
		fetchingRelationshipCollectionsActors.add(actor.activityPubID);
		executor.submit(new FetchActorRelationshipCollectionsTask(this, context, fetchingRelationshipCollectionsActors, actor));
	}

	/**
	 * Fetch and store actor's collections that are related to its content.
	 * Currently, that's only the wall with all comments.
	 * @param actor the remote actor
	 */
	public synchronized void fetchActorContentCollections(Actor actor){
		LOG.debug("Fetching content collections for actor {}", actor.activityPubID);
		actor.ensureRemote();
		if(fetchingContentCollectionsActors.contains(actor.activityPubID)){
			LOG.trace("Another fetch is already in progress for content collections of {}", actor.activityPubID);
			return;
		}
		fetchingContentCollectionsActors.add(actor.activityPubID);
		executor.submit(new FetchActorContentCollectionsTask(this, context, fetchingAllReplies, fetchingContentCollectionsActors, actor));
	}

	public synchronized Future<List<Post>> fetchRepostChain(NoteOrQuestion topLevelPost){
		return fetchingRepostChains.computeIfAbsent(topLevelPost.activityPubID, uri->executor.submit(new FetchRepostChainTask(this, context, fetchingRepostChains, topLevelPost)));
	}

	public <T extends Callable<?>> void invokeAll(Collection<T> tasks){
		ArrayList<Future<?>> futures=new ArrayList<>();
		for(Callable<?> task:tasks){
			futures.add(executor.submit(task));
		}
		for(Future<?> future:futures){
			try{
				future.get();
			}catch(Exception x){
				LOG.warn("Task execution failed", x);
			}
		}
	}

	public void submitTask(Runnable task){
		executor.submit(task);
	}

	public <T> Future<T> submitTask(Callable<T> task){
		return executor.submit(task);
	}

	public void scheduleRetry(ActivityDeliveryRetry retry){
		if(retry.needMoreAttempts()){
			retryExecutor.schedule(new RetryActivityRunnable(this, context, retry), retry.getDelayForThisAttempt(), TimeUnit.MILLISECONDS);
		}
	}

	public void submitActivityForFollowers(Activity activity, User actor){
		try{
			submitActivity(activity, actor, UserStorage.getFollowerInboxes(actor.id));
		}catch(SQLException x){
			LOG.error("Error getting follower inboxes for sending {} on behalf of user {}", activity.getType(), actor.id, x);
		}
	}

	public void submitActivityForMembers(Activity activity, Group group){
		try{
			submitActivity(activity, group, GroupStorage.getGroupMemberInboxes(group.id));
		}catch(SQLException x){
			LOG.error("Error getting member inboxes for sending {} on behalf of group {}", activity.getType(), group.id, x);
		}
	}

	public void submitActivity(Activity activity, Actor actor, URI inbox){
		submitTask(new SendOneActivityRunnable(this, context, activity, inbox, actor));
	}

	public void submitActivity(Activity activity, Actor actor, Collection<URI> inboxes){
		for(URI inbox:inboxes){
			submitActivity(activity, actor, inbox);
		}
	}
}
