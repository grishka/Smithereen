package smithereen.activitypub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
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
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.ForeignActor;
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
import smithereen.activitypub.tasks.FetchAllWallRepliesTask;
import smithereen.activitypub.tasks.FetchCommentReplyThreadRunnable;
import smithereen.activitypub.tasks.FetchPhotoAlbumPhotosTask;
import smithereen.activitypub.tasks.FetchWallReplyThreadRunnable;
import smithereen.activitypub.tasks.FetchRepostChainTask;
import smithereen.activitypub.tasks.ForwardOneActivityRunnable;
import smithereen.activitypub.tasks.RetryActivityRunnable;
import smithereen.activitypub.tasks.SendActivitySequenceRunnable;
import smithereen.activitypub.tasks.SendOneActivityRunnable;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.model.ActivityPubRepresentable;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.LikeableContentObject;
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
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentReplyParent;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.model.photos.PhotoTag;
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
	private final HashMap<URI, Future<List<Post>>> fetchingWallReplyThreads=new HashMap<>();
	private final HashMap<URI, List<Consumer<List<Post>>>> afterFetchWallReplyThreadActions=new HashMap<>();
	private final HashMap<URI, Future<Post>> fetchingAllWallReplies=new HashMap<>();
	private final HashMap<URI, Future<List<CommentReplyParent>>> fetchingCommentReplyThreads=new HashMap<>();
	private final HashMap<URI, List<Consumer<List<CommentReplyParent>>>> afterFetchCommentReplyThreadActions=new HashMap<>();
	private final HashMap<URI, Future<CommentReplyParent>> fetchingAllCommentReplies=new HashMap<>();
	private final HashSet<URI> fetchingRelationshipCollectionsActors=new HashSet<>();
	private final HashSet<URI> fetchingContentCollectionsActors=new HashSet<>();
	private final HashMap<URI, Future<List<Post>>> fetchingRepostChains=new HashMap<>();
	private final HashMap<URI, Future<Void>> fetchingPhotoAlbums=new HashMap<>();

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

	public void getInboxesWithPrivacy(Set<URI> inboxes, User owner, PrivacySetting setting) throws SQLException{
		switch(setting.baseRule){
			case EVERYONE -> inboxes.addAll(UserStorage.getFollowerInboxes(owner.id, setting.exceptUsers));
			case FRIENDS, FRIENDS_OF_FRIENDS -> inboxes.addAll(UserStorage.getFriendInboxes(owner.id, setting.exceptUsers));
			case NONE -> context.getUsersController().getUsers(setting.allowUsers).values().stream().map(this::actorInbox).forEach(inboxes::add);
		}
	}

	private long rand(){
		return Math.abs(rand.nextLong());
	}

	public void forwardActivity(String json, Actor signer, Collection<URI> inboxes, String originatingDomain, Server.Feature requiredServerFeature){
		for(URI inbox:inboxes){
			if(inbox.getHost().equalsIgnoreCase(originatingDomain))
				continue;
			executor.submit(new ForwardOneActivityRunnable(context, json, inbox, signer, requiredServerFeature!=null ? EnumSet.of(requiredServerFeature) : EnumSet.noneOf(Server.Feature.class)));
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
			for(User user:context.getUsersController().getUsers(post.mentionedUserIDs).values()){
				if(user instanceof ForeignUser foreignUser){
					URI inbox=actorInbox(foreignUser);
					inboxes.add(inbox);
				}
			}
		}
		return inboxes;
	}

	public Set<URI> getInboxesForComment(Comment comment, CommentableContentObject parent){
		Set<URI> inboxes=new HashSet<>();
		OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(parent);

		try{
			switch(parent){
				case Photo photo -> {
					PhotoAlbum album=context.getPhotosController().getAlbumIgnoringPrivacy(photo.albumID);
					Collection<User> mentionedUsers=context.getUsersController().getUsers(comment.mentionedUserIDs).values();
					if(oaa.owner() instanceof User user){
						getInboxesWithPrivacy(inboxes, user, album.viewPrivacy);
						for(User mentionedUser:mentionedUsers){
							URI inbox=actorInbox(mentionedUser);
							if(inboxes.contains(inbox))
								continue;
							if(context.getPrivacyController().checkUserPrivacy(mentionedUser, user, album.viewPrivacy))
								inboxes.add(inbox);
						}
					}else if(oaa.owner() instanceof Group group){
						inboxes.addAll(GroupStorage.getGroupMemberInboxes(group.id));
						if(group.accessType==Group.AccessType.OPEN){
							mentionedUsers.stream().map(this::actorInbox).forEach(inboxes::add);
						}else{
							for(User mentionedUser:mentionedUsers){
								URI inbox=actorInbox(mentionedUser);
								if(inboxes.contains(inbox))
									continue;
								Group.MembershipState state=context.getGroupsController().getUserMembershipState(group, mentionedUser);
								if(state==Group.MembershipState.MEMBER || state==Group.MembershipState.TENTATIVE_MEMBER)
									inboxes.add(inbox);
							}
						}
					}
				}
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
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

			Create create=new Create().withActorLinkAndObject(author, note);
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

			Update update=new Update().withActorLinkAndObject(author, note);
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

				Add add=new Add().withActorAndObjectLinks(oaa.owner(), note);
				add.activityPubID=UriBuilder.local().path("posts", String.valueOf(post.id), "activityAdd").build();
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
			Delete delete=new Delete().withActorAndObjectLinks(actor, note);

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

		Follow follow=new Follow()
				.withActorAndObjectLinks(self, target)
				.withActorFragmentID("followUser"+target.id+"_"+rand());
		Undo undo=new Undo()
				.withActorLinkAndObject(self, follow)
				.withActorFragmentID("unfollowUser"+target.id+"_"+rand());

		submitActivity(undo, self, target.inbox);
	}

	public void sendRemoveFromFriendsCollectionActivity(User self, User exFriend){
		Remove remove=new Remove()
				.withActorAndObjectLinks(self, exFriend)
				.withActorFragmentID("unfriendUserCollection"+exFriend.id+"_"+rand())
				.withTarget(self.getFriendsURL());
		submitActivityForFollowers(remove, self);
	}

	public void sendAddToFriendsCollectionActivity(User self, User friend){
		Add add=new Add()
				.withActorAndObjectLinks(self, friend)
				.withActorFragmentID("addFriendUserCollection"+friend.id+"_"+rand())
				.withTarget(self.getFriendsURL());
		submitActivityForFollowers(add, self);
	}

	public void sendAddToGroupsCollectionActivity(User self, Group group, boolean tentative){
		Add add=new Add()
				.withActorAndObjectLinks(self, group)
				.withActorFragmentID("addGroupCollection"+group.id+"_"+rand())
				.withTarget(self.getGroupsURL());
		add.tentative=tentative;
		submitActivityForFollowers(add, self);
	}

	public void sendRemoveFromGroupsCollectionActivity(User self, Group group){
		Remove remove=new Remove()
				.withActorAndObjectLinks(self, group)
				.withActorFragmentID("removeGroupCollection"+group.id+"_"+rand())
				.withTarget(self.getGroupsURL());
		submitActivityForFollowers(remove, self);
	}

	public void sendFollowUserActivity(User self, ForeignUser target){
		Follow follow=new Follow()
				.withActorAndObjectLinks(self, target)
				.withActorFragmentID("followUser"+target.id+"_"+rand());
		submitActivity(follow, self, target.inbox);
	}

	public void sendJoinGroupActivity(User self, ForeignGroup target, boolean tentative){
		Join join=new Join(tentative)
				.withActorAndObjectLinks(self, target)
				.withActorFragmentID("joinGroup"+target.id+"_"+rand());
		submitActivity(join, self, target.inbox);
	}

	public void sendLeaveGroupActivity(User self, ForeignGroup target){
		Leave leave=new Leave()
				.withActorAndObjectLinks(self, target)
				.withActorFragmentID("leaveGroup"+target.id+"_"+rand());
		submitActivity(leave, self, target.inbox);
	}

	public void sendFriendRequestActivity(User self, ForeignUser target, String message){
		Follow follow=new Follow()
				.withActorAndObjectLinks(self, target)
				.withActorFragmentID("follow"+target.id+"_"+rand());
		if(target.supportsFriendRequests()){
			Offer offer=new Offer()
					.withActorLinkAndObject(self, new Follow().withActorAndObjectLinks(target, self))
					.withActorFragmentID("friendRequest"+target.id+"_"+rand());
			if(StringUtils.isNotEmpty(message)){
				offer.content=message;
			}
			executor.submit(new SendActivitySequenceRunnable(this, context, List.of(follow, offer), target.inbox, self));
		}else{
			submitActivity(follow, self, target.inbox);
		}
	}

	public void sendAcceptFollowActivity(ForeignUser actor, Actor self, Follow follow){
		self.ensureLocal();
		Accept accept=new Accept()
				.withActorLinkAndObject(self, follow)
				.withActorFragmentID("acceptFollow"+actor.id);
		submitActivity(accept, self, actor.inbox);
	}

	public void sendRejectFriendRequestActivity(User self, ForeignUser target){
		Reject reject=new Reject()
				.withActorLinkAndObject(self, new Offer().withActorLinkAndObject(target, new Follow().withActorAndObjectLinks(self, target)))
				.withActorFragmentID("rejectFriendReq"+target.id);
		submitActivity(reject, self, target.inbox);
	}

	public void sendUpdateUserActivity(User user){
		Update update=new Update()
				.withActorLinkAndObject(user, user)
				.withActorFragmentID("updateProfile"+System.currentTimeMillis());
		update.to=Collections.singletonList(new LinkOrObject(ActivityPub.AS_PUBLIC));
		submitActivityForFollowers(update, user);
	}

	public void sendUpdateGroupActivity(Group group){
		Update update=new Update()
				.withActorLinkAndObject(group, group)
				.withActorFragmentID("updateProfile"+System.currentTimeMillis());
		update.to=Collections.singletonList(new LinkOrObject(ActivityPub.AS_PUBLIC));
		submitActivityForMembers(update, group);
	}

	public void sendLikeActivity(LikeableContentObject object, User user, int likeID) throws SQLException{
		if(!(object instanceof ActivityPubRepresentable apObject))
			throw new IllegalArgumentException();
		Like like=new Like().withActorAndObjectLinks(user, apObject);
		like.activityPubID=Config.localURI("/activitypub/objects/likes/"+likeID);
		switch(object){
			case Post post -> submitActivity(like, user, PostStorage.getInboxesForPostInteractionForwarding(post));
			case Photo photo -> sendActivityForPhoto(user, context.getPhotosController().getAlbumIgnoringPrivacy(photo.albumID), like);
			case Comment comment -> sendActivityForCommentToBeForwarded(comment, user, context.getCommentsController().getCommentParentIgnoringPrivacy(comment), like);
		}
	}

	public void sendUndoLikeActivity(LikeableContentObject object, User user, int likeID) throws SQLException{
		if(!(object instanceof ActivityPubRepresentable apObject))
			throw new IllegalArgumentException();
		Like like=new Like().withActorAndObjectLinks(user, apObject);
		like.activityPubID=Config.localURI("/activitypub/objects/likes/"+likeID);
		Undo undo=new Undo().withActorLinkAndObject(user, like);
		undo.activityPubID=Config.localURI("/activitypub/objects/likes/"+likeID+"/undo");
		ActivityPubCache.putUndoneLike(likeID, undo);
		switch(object){
			case Post post -> submitActivity(undo, user, PostStorage.getInboxesForPostInteractionForwarding(post));
			case Photo photo -> sendActivityForPhoto(user, context.getPhotosController().getAlbumIgnoringPrivacy(photo.albumID), undo);
			case Comment comment -> sendActivityForCommentToBeForwarded(comment, user, context.getCommentsController().getCommentParentIgnoringPrivacy(comment), undo);
		}
	}

	public void sendBlockActivity(Actor self, ForeignUser target){
		Block block=new Block()
				.withActorAndObjectLinks(self, target)
				.withActorFragmentID("blockUser"+target.id+"_"+System.currentTimeMillis());
		submitActivity(block, self, target.inbox);
	}

	public void sendUndoBlockActivity(Actor self, ForeignUser target){
		Block block=new Block()
				.withActorAndObjectLinks(self, target)
				.withActorFragmentID("blockUser"+target.id+"_"+System.currentTimeMillis());
		Undo undo=new Undo()
				.withActorLinkAndObject(self, block)
				.withActorFragmentID("undoBlockUser"+target.id+"_"+System.currentTimeMillis());
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
			Create create=new Create().withActorLinkAndObject(self, vote);
			create.activityPubID=new UriBuilder(self.activityPubID).fragment("pollVotes/"+voteID+"/activity").build();
			create.to=vote.to;
			create.cc=vote.cc;
			create.published=Instant.now();

			submitActivity(create, self, pollOwner.inbox);
		}
	}

	public void sendGroupInvite(int inviteID, User self, Group group, User target){
		if(Config.isLocal(group.activityPubID) && Config.isLocal(target.activityPubID))
			return;

		Invite invite=new Invite().withActorAndObjectLinks(self, group);
		invite.activityPubID=Config.localURI("/activitypub/objects/groupInvites/"+inviteID);
		invite.to=List.of(new LinkOrObject(target.activityPubID));
		invite.cc=List.of(new LinkOrObject(group.activityPubID));

		if(!Objects.equals(group.sharedInbox, target.sharedInbox)){
			if(group instanceof ForeignGroup fg)
				submitActivity(invite, self, fg.inbox);
			if(target instanceof ForeignUser fu)
				submitActivity(invite, self, fu.inbox);
		}else{
			submitActivity(invite, self, actorInbox(target));
		}
	}

	public void sendRejectGroupInvite(User self, ForeignGroup group, int invitationLocalID, User inviter, URI invitationID){
		Invite invite=new Invite()
				.withActorAndObjectLinks(inviter, group);
		invite.activityPubID=invitationID;
		invite.to=List.of(new LinkOrObject(self.activityPubID));
		invite.cc=List.of(new LinkOrObject(group.activityPubID));

		Reject reject=new Reject()
				.withActorLinkAndObject(self, invite)
				.withActorFragmentID("rejectGroupInvite"+invitationLocalID);
		reject.to=List.of(new LinkOrObject(group.activityPubID));

		submitActivity(reject, self, group.inbox);
	}

	public void sendRejectFollowGroup(ForeignUser user, Group group, boolean tentative){
		Reject reject=new Reject()
				.withActorLinkAndObject(group, new Join(tentative).withActorAndObjectLinks(user, group))
				.withActorFragmentID("rejectJoin"+user.id+"_"+rand());
		reject.to=List.of(new LinkOrObject(user.activityPubID));

		submitActivity(reject, group, user.inbox);
	}

	public void sendUndoGroupInvite(ForeignUser user, Group group, int invitationLocalID, User inviter, URI invitationID){
		Invite invite=new Invite()
				.withActorAndObjectLinks(inviter, group);
		invite.activityPubID=invitationID;
		invite.to=List.of(new LinkOrObject(user.activityPubID));
		invite.cc=List.of(new LinkOrObject(group.activityPubID));

		Undo undo=new Undo()
				.withActorLinkAndObject(group, invite)
				.withActorFragmentID("undoGroupInvite"+invitationLocalID);
		undo.to=List.of(new LinkOrObject(user.activityPubID));

		submitActivity(undo, group, user.inbox);
	}

	public void sendAddUserToGroupActivity(User user, Group group, boolean tentative){
		group.ensureLocal();
		Add add=new Add()
				.withActorAndObjectLinks(group, user)
				.withActorFragmentID("addUser"+user.id+"_"+rand());
		ActivityPubCollection target=new ActivityPubCollection(false);
		target.activityPubID=Config.localURI("/groups/"+group.id+"/"+(tentative ? "tentativeMembers" : "members"));
		target.attributedTo=group.activityPubID;
		add.target=new LinkOrObject(target);
		if(group.isEvent())
			add.to=List.of(new LinkOrObject(Config.localURI("/groups/"+group.id+"/members")),
					new LinkOrObject(Config.localURI("/groups/"+group.id+"/tentativeMembers")));
		else
			add.to=List.of(new LinkOrObject(Config.localURI("/groups/"+group.id+"/members")));

		submitActivityForMembers(add, group);
	}

	public void sendRemoveUserFromGroupActivity(User user, Group group, boolean tentative){
		group.ensureLocal();
		Remove remove=new Remove()
				.withActorAndObjectLinks(group, user)
				.withActorFragmentID("removeUser"+user.id+"_"+rand());
		ActivityPubCollection target=new ActivityPubCollection(false);
		target.activityPubID=Config.localURI("/groups/"+group.id+"/"+(tentative ? "tentativeMembers" : "members"));
		target.attributedTo=group.activityPubID;
		remove.target=new LinkOrObject(target);
		if(group.isEvent())
			remove.to=List.of(new LinkOrObject(Config.localURI("/groups/"+group.id+"/members")),
					new LinkOrObject(Config.localURI("/groups/"+group.id+"/tentativeMembers")));
		else
			remove.to=List.of(new LinkOrObject(Config.localURI("/groups/"+group.id+"/members")));

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
		Create create=new Create().withActorLinkAndObject(self, note);
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

		Delete delete=new Delete().withActorAndObjectLinks(self, msg);
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

		Read read=new Read().withActorAndObjectLinks(self, msg);
		HashSet<Integer> to=new HashSet<>(msg.to);
		to.add(msg.senderID);
		read.to=to.stream().filter(id->id!=self.id).map(id->new LinkOrObject(users.get(id).activityPubID)).toList();
		if(msg.cc!=null && !msg.cc.isEmpty())
			read.cc=msg.cc.stream().filter(id->id!=self.id).map(id->new LinkOrObject(users.get(id).activityPubID)).toList();
		read.activityPubID=UriBuilder.local().path("activitypub", "objects", "messages", msg.encodedID).fragment("read"+self.id).build();

		Set<URI> inboxes=users.values().stream().filter(u->u instanceof ForeignUser).map(this::actorInbox).collect(Collectors.toSet());
		submitActivity(read, self, inboxes);
	}

	public void sendUserDeleteSelf(User self){
		Delete del=new Delete()
				.withActorAndObjectLinks(self, self)
				.withActorFragmentID("deleteSelf");
		submitActivityForFollowers(del, self);
	}

	// region Photo albums

	private void sendActivityForPhotoAlbum(Actor actor, PhotoAlbum album, Activity activity){
		if(actor instanceof User user){
			HashSet<URI> inboxes=new HashSet<>();
			try{
				getInboxesWithPrivacy(inboxes, user, album.viewPrivacy);
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
			submitActivity(activity, user, inboxes, Server.Feature.PHOTO_ALBUMS);
		}else if(actor instanceof Group group){
			submitActivityForMembers(activity, group, Server.Feature.PHOTO_ALBUMS);
		}
	}

	public void sendCreatePhotoAlbum(Actor actor, PhotoAlbum album){
		Create create=new Create()
				.withActorLinkAndObject(actor, ActivityPubPhotoAlbum.fromNativeAlbum(album, context));
		create.activityPubID=new UriBuilder(album.getActivityPubID()).fragment("create").build();
		sendActivityForPhotoAlbum(actor, album, create);
	}

	public void sendDeletePhotoAlbum(Actor actor, PhotoAlbum album){
		Delete delete=new Delete()
				.withActorAndObjectLinks(actor, album);
		delete.activityPubID=new UriBuilder(album.getActivityPubID()).fragment("delete").build();
		sendActivityForPhotoAlbum(actor, album, delete);
	}

	public void sendUpdatePhotoAlbum(Actor actor, PhotoAlbum album){
		Update update=new Update()
				.withActorLinkAndObject(actor, ActivityPubPhotoAlbum.fromNativeAlbum(album, context));
		update.activityPubID=new UriBuilder(album.getActivityPubID()).fragment("update"+rand()).build();
		sendActivityForPhotoAlbum(actor, album, update);
	}

	// endregion
	// region Photos

	private void sendActivityForPhoto(Actor actor, PhotoAlbum album, Activity activity){
		if(album.ownerID>0){
			sendActivityForPhotoAlbum(actor, album, activity);
		}else{
			Group owner=context.getGroupsController().getGroupOrThrow(-album.ownerID);
			if(owner instanceof ForeignGroup){
				submitActivity(activity, actor, actorInbox(owner));
			}else{
				try{
					submitActivity(activity, actor, GroupStorage.getGroupMemberInboxes(owner.id), Server.Feature.PHOTO_ALBUMS);
				}catch(SQLException x){
					LOG.error("Error getting group member inboxes for {}", activity.getType(), x);
				}
			}
		}
	}

	public void sendAddPhotoToAlbum(Actor actor, Photo photo, PhotoAlbum album){
		Add add=new Add();
		if(photo.apID!=null)
			add.withActorAndObjectLinks(actor, photo);
		else
			add.withActorLinkAndObject(actor, ActivityPubPhoto.fromNativePhoto(photo, album, context));
		add.activityPubID=new UriBuilder(album.getActivityPubID()).fragment("add"+photo.getIdString()).build();
		ActivityPubPhotoAlbum target=new ActivityPubPhotoAlbum();
		target.activityPubID=album.getActivityPubID();
		target.attributedTo=actor.activityPubID;
		add.target=new LinkOrObject(target);
		sendActivityForPhotoAlbum(actor, album, add);
	}

	public void sendCreateAlbumPhoto(User actor, Photo photo, PhotoAlbum album){
		Create create=new Create()
				.withActorLinkAndObject(actor, ActivityPubPhoto.fromNativePhoto(photo, album, context));
		create.activityPubID=new UriBuilder(photo.getActivityPubID()).appendPath("activityCreate").build();
		ActivityPubPhotoAlbum target=new ActivityPubPhotoAlbum();
		target.activityPubID=album.getActivityPubID();
		target.attributedTo=actor.activityPubID;
		create.target=new LinkOrObject(target);
		URI inbox=actorInbox(context.getWallController().getContentAuthorAndOwner(album).owner());
		submitActivity(create, actor, inbox);
	}

	public void sendUpdateAlbumPhoto(User actor, Photo photo, PhotoAlbum album){
		Update update=new Update()
				.withActorLinkAndObject(actor, ActivityPubPhoto.fromNativePhoto(photo, album, context));
		update.activityPubID=new UriBuilder(photo.getActivityPubID()).fragment("update"+rand()).build();
		Actor owner=context.getWallController().getContentAuthorAndOwner(photo).owner();
		if(owner instanceof ForeignActor){
			// The owner will forward it as needed
			submitActivity(update, actor, actorInbox(owner));
		}else{
			sendActivityForPhoto(actor, album, update);
		}
	}

	public void sendDeleteAlbumPhoto(User actor, Photo photo, PhotoAlbum album){
		Delete delete=new Delete()
				.withActorAndObjectLinks(actor, photo);
		delete.activityPubID=new UriBuilder(photo.getActivityPubID()).fragment("delete").build();
		Actor owner=context.getWallController().getContentAuthorAndOwner(photo).owner();
		if(owner instanceof ForeignActor){
			// The owner will forward it as needed
			submitActivity(delete, actor, actorInbox(owner));
		}else{
			sendActivityForPhoto(actor, album, delete);
		}
	}

	public void sendRemoveAlbumPhoto(Group actor, Photo photo, PhotoAlbum album){
		Remove remove=new Remove()
				.withActorAndObjectLinks(actor, photo)
				.withActorFragmentID("deletePhoto"+photo.getIdString());
		ActivityPubPhotoAlbum target=new ActivityPubPhotoAlbum();
		target.activityPubID=album.getActivityPubID();
		target.attributedTo=actor.activityPubID;
		remove.target=new LinkOrObject(target);
		sendActivityForPhoto(actor, album, remove);
	}

	public void sendApprovePhotoTag(User self, Photo photo, PhotoAlbum album, PhotoTag tag, User placer, Actor photoOwner){
		Add add=new Add();
		if(photo.apID!=null)
			add.withActorAndObjectLinks(self, photo);
		else
			add.withActorLinkAndObject(self, ActivityPubPhoto.fromNativePhoto(photo, album, context));
		add.activityPubID=new UriBuilder(self.activityPubID).fragment("acceptPhotoTag"+tag.id()).build();
		ActivityPubCollection target=new ActivityPubCollection(false);
		target.activityPubID=self.getTaggedPhotosURL();
		target.attributedTo=self.activityPubID;
		add.target=new LinkOrObject(target);
		HashSet<URI> extraInboxes=new HashSet<>();
		if(placer instanceof ForeignUser)
			extraInboxes.add(actorInbox(placer));
		if(photoOwner instanceof ForeignUser || photoOwner instanceof ForeignGroup)
			extraInboxes.add(actorInbox(photoOwner));
		submitActivityForFollowers(add, self, Server.Feature.PHOTO_ALBUMS, extraInboxes);
	}

	public void sendRejectPhotoTag(User self, Photo photo, PhotoAlbum album, PhotoTag tag, User placer, Actor photoOwner){
		Reject reject=new Reject();
		if(photo.apID!=null)
			reject.withActorAndObjectLinks(self, photo);
		else
			reject.withActorLinkAndObject(self, ActivityPubPhoto.fromNativePhoto(photo, album, context));
		reject.activityPubID=new UriBuilder(self.activityPubID).fragment("rejectPhotoTag"+tag.id()).build();
		ActivityPubCollection target=new ActivityPubCollection(false);
		target.activityPubID=self.getTaggedPhotosURL();
		target.attributedTo=self.activityPubID;
		reject.target=new LinkOrObject(target);
		HashSet<URI> inboxes=new HashSet<>();
		if(placer instanceof ForeignUser)
			inboxes.add(actorInbox(placer));
		if(photoOwner instanceof ForeignUser || photoOwner instanceof ForeignGroup)
			inboxes.add(actorInbox(photoOwner));
		submitActivity(reject, self, inboxes);
	}

	public void sendDeletePhotoTag(User self, Photo photo, PhotoAlbum album, PhotoTag tag, User placer, Actor photoOwner){
		Remove remove=new Remove();
		if(photo.apID!=null)
			remove.withActorAndObjectLinks(self, photo);
		else
			remove.withActorLinkAndObject(self, ActivityPubPhoto.fromNativePhoto(photo, album, context));
		remove.activityPubID=new UriBuilder(self.activityPubID).fragment("deletePhotoTag"+tag.id()).build();
		ActivityPubCollection target=new ActivityPubCollection(false);
		target.activityPubID=self.getTaggedPhotosURL();
		target.attributedTo=self.activityPubID;
		remove.target=new LinkOrObject(target);
		HashSet<URI> extraInboxes=new HashSet<>();
		if(placer instanceof ForeignUser)
			extraInboxes.add(actorInbox(placer));
		if(photoOwner instanceof ForeignUser || photoOwner instanceof ForeignGroup)
			extraInboxes.add(actorInbox(photoOwner));
		submitActivityForFollowers(remove, self, Server.Feature.PHOTO_ALBUMS, extraInboxes);
	}

	// endregion
	// region Comments

	private void sendActivityForComment(Comment comment, Actor actor, CommentableContentObject parent, Activity activity){
		Set<URI> inboxes=getInboxesForComment(comment, parent);
		submitActivity(activity, actor, inboxes, comment.parentObjectID.getRqeuiredServerFeature());
	}

	private void sendActivityForCommentToBeForwarded(Comment comment, Actor actor, CommentableContentObject parent, Activity activity){
		if(comment.isLocal()){
			sendActivityForComment(comment, actor, parent, activity);
		}else{
			OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(comment);
			HashSet<URI> inboxes=new HashSet<>();
			if(oaa.owner() instanceof ForeignActor)
				inboxes.add(actorInbox(oaa.owner()));
			if(oaa.author() instanceof ForeignActor)
				inboxes.add(actorInbox(oaa.author()));
			submitActivity(activity, actor, inboxes, comment.parentObjectID.getRqeuiredServerFeature());
		}
	}

	public void sendCreateComment(User actor, Comment comment, CommentableContentObject parent){
		NoteOrQuestion note=NoteOrQuestion.fromNativeComment(comment, context);
		Create create=new Create()
				.withActorLinkAndObject(actor, note);
		create.activityPubID=new UriBuilder(comment.getActivityPubID()).appendPath("activityCreate").build();
		create.target=new LinkOrObject(note.target);
		create.to=note.to;
		create.cc=note.cc;
		submitActivity(create, actor, actorInbox(context.getWallController().getContentAuthorAndOwner(parent).owner()));
	}

	public void sendAddComment(Actor actor, Comment comment, CommentableContentObject parent){
		Add add=new Add();
		if(comment.isLocal())
			add.withActorLinkAndObject(actor, NoteOrQuestion.fromNativeComment(comment, context));
		else
			add.withActorAndObjectLinks(actor, comment);
		add.withActorFragmentID("addComment"+comment.getIDString());
		ActivityPubCollection target=new ActivityPubCollection(false);
		target.activityPubID=parent.getCommentCollectionID(context);
		target.attributedTo=actor.activityPubID;
		add.target=new LinkOrObject(target);
		sendActivityForComment(comment, actor, parent, add);
	}

	public void sendUpdateComment(User actor, Comment comment, CommentableContentObject parent){
		NoteOrQuestion note=NoteOrQuestion.fromNativeComment(comment, context);
		Update update=new Update()
				.withActorLinkAndObject(actor, note);
		update.activityPubID=new UriBuilder(comment.getActivityPubID()).fragment("update"+rand()).build();
		Actor owner=context.getWallController().getContentAuthorAndOwner(parent).owner();
		if(owner instanceof ForeignActor){
			// The owner will forward it as needed
			submitActivity(update, actor, actorInbox(owner));
		}else{
			sendActivityForComment(comment, actor, parent, update);
		}
	}

	public void sendDeleteComment(User actor, Comment comment, CommentableContentObject parent){
		Delete delete=new Delete()
				.withActorAndObjectLinks(actor, comment)
				.withObjectFragmentID("delete");
		Actor owner=context.getWallController().getContentAuthorAndOwner(parent).owner();
		if(owner instanceof ForeignActor){
			// The owner will forward it as needed
			submitActivity(delete, actor, actorInbox(owner));
		}else{
			sendActivityForComment(comment, actor, parent, delete);
		}
	}

	public void sendRemoveComment(Actor actor, Comment comment, CommentableContentObject parent){
		Remove remove=new Remove()
				.withActorAndObjectLinks(actor, comment)
				.withActorFragmentID("deleteComment"+comment.getIDString());
		ActivityPubCollection target=new ActivityPubCollection(false);
		target.activityPubID=parent.getCommentCollectionID(context);
		target.attributedTo=actor.activityPubID;
		remove.target=new LinkOrObject(target);
		sendActivityForComment(comment, actor, parent, remove);
	}

	// endregion

	public synchronized Future<List<Post>> fetchWallReplyThread(NoteOrQuestion post){
		return fetchingWallReplyThreads.computeIfAbsent(post.activityPubID, (uri)->executor.submit(new FetchWallReplyThreadRunnable(this, afterFetchWallReplyThreadActions, context, fetchingWallReplyThreads, post)));
	}

	public synchronized Future<List<Post>> fetchWallReplyThreadAndThen(NoteOrQuestion post, Consumer<List<Post>> action){
		afterFetchWallReplyThreadActions.computeIfAbsent(post.activityPubID, (uri)->new ArrayList<>()).add(action);
		return fetchWallReplyThread(post);
	}

	public synchronized Future<Post> fetchAllReplies(Post post){
		return fetchingAllWallReplies.computeIfAbsent(post.getActivityPubID(), (uri)->executor.submit(new FetchAllWallRepliesTask(this, context, fetchingAllWallReplies, post)));
	}

	public synchronized Future<List<CommentReplyParent>> fetchCommentReplyThread(NoteOrQuestion post){
		return fetchingCommentReplyThreads.computeIfAbsent(post.activityPubID, (uri)->executor.submit(new FetchCommentReplyThreadRunnable(this, afterFetchCommentReplyThreadActions, context, fetchingCommentReplyThreads, post)));
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
		executor.submit(new FetchActorContentCollectionsTask(this, context, fetchingAllWallReplies, fetchingContentCollectionsActors, actor, fetchingPhotoAlbums));
	}

	public synchronized Future<List<Post>> fetchRepostChain(NoteOrQuestion topLevelPost){
		return fetchingRepostChains.computeIfAbsent(topLevelPost.activityPubID, uri->executor.submit(new FetchRepostChainTask(this, context, fetchingRepostChains, topLevelPost)));
	}

	public synchronized Future<Void> fetchPhotoAlbumContents(ActivityPubPhotoAlbum album, PhotoAlbum nativeAlbum){
		return fetchingPhotoAlbums.computeIfAbsent(album.activityPubID, uri->executor.submit(new FetchPhotoAlbumPhotosTask(context, album, nativeAlbum, this, fetchingPhotoAlbums)));
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

	public void submitActivityForFollowers(Activity activity, User actor, Server.Feature requiredFeature, Set<URI> extraInboxes){
		try{
			HashSet<URI> inboxes=new HashSet<>();
			inboxes.addAll(UserStorage.getFollowerInboxes(actor.id));
			inboxes.addAll(extraInboxes);
			submitActivity(activity, actor, inboxes, requiredFeature);
		}catch(SQLException x){
			LOG.error("Error getting follower inboxes for sending {} on behalf of user {}", activity.getType(), actor.id, x);
		}
	}

	public void submitActivityForMembers(Activity activity, Group group, Server.Feature requiredFeature){
		try{
			submitActivity(activity, group, GroupStorage.getGroupMemberInboxes(group.id), requiredFeature);
		}catch(SQLException x){
			LOG.error("Error getting member inboxes for sending {} on behalf of group {}", activity.getType(), group.id, x);
		}
	}

	public void submitActivity(Activity activity, Actor actor, URI inbox){
		if(!Objects.equals(actor.activityPubID, activity.actor.link))
			throw new IllegalArgumentException("Activity "+activity.getType()+" actor ID "+activity.actor.link+" does not match expected "+actor.activityPubID);
		submitTask(new SendOneActivityRunnable(this, context, activity, inbox, actor));
	}

	public void submitActivity(Activity activity, Actor actor, URI inbox, Server.Feature requiredFeature){
		if(!Objects.equals(actor.activityPubID, activity.actor.link))
			throw new IllegalArgumentException("Activity "+activity.getType()+" actor ID "+activity.actor.link+" does not match expected "+actor.activityPubID);
		submitTask(new SendOneActivityRunnable(this, context, activity, inbox, actor).requireFeature(requiredFeature));
	}

	public void submitActivity(Activity activity, Actor actor, Collection<URI> inboxes){
		for(URI inbox:inboxes){
			submitActivity(activity, actor, inbox);
		}
	}

	public void submitActivity(Activity activity, Actor actor, Collection<URI> inboxes, Server.Feature requiredFeature){
		for(URI inbox:inboxes){
			submitActivity(activity, actor, inbox, requiredFeature);
		}
	}
}
