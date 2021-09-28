package smithereen.activitypub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.ObjectLinkResolver;
import smithereen.Utils;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Add;
import smithereen.activitypub.objects.activities.Block;
import smithereen.activitypub.objects.activities.Create;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Offer;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.activitypub.objects.activities.Update;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.Poll;
import smithereen.data.PollOption;
import smithereen.data.PollVote;
import smithereen.data.Post;
import smithereen.data.UriBuilder;
import smithereen.data.User;
import smithereen.data.notifications.NotificationUtils;
import smithereen.storage.GroupStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.utils.StringUtils;

public class ActivityPubWorker{
	private static ActivityPubWorker instance;
	private static final Logger LOG=LoggerFactory.getLogger(ActivityPubWorker.class);
	private static final int MAX_COMMENTS=1000;

	private ForkJoinPool executor;
	private Random rand=new Random();
	private HashMap<URI, Future<List<Post>>> fetchingReplyThreads=new HashMap<>();
	private HashMap<URI, List<Consumer<List<Post>>>> afterFetchReplyThreadActions=new HashMap<>();
	private HashMap<URI, Future<Post>> fetchingAllReplies=new HashMap<>();

	public static ActivityPubWorker getInstance(){
		if(instance==null)
			instance=new ActivityPubWorker();
		return instance;
	}

	private ActivityPubWorker(){
		executor=new ForkJoinPool(Runtime.getRuntime().availableProcessors()*2);
	}

	public static void shutDown(){
		if(instance==null)
			return;
		LOG.info("Stopping thread pool");
		Utils.stopExecutorBlocking(instance.executor, LOG);
		LOG.info("Stopped");
	}

	private URI actorInbox(ForeignUser actor){
		return actor.sharedInbox!=null ? actor.sharedInbox : actor.inbox;
	}

	private URI actorInbox(ForeignGroup actor){
		return actor.sharedInbox!=null ? actor.sharedInbox : actor.inbox;
	}

	private long rand(){
		return Math.abs(rand.nextLong());
	}

	public void forwardActivity(String json, User signer, List<URI> inboxes, String originatingDomain){
		for(URI inbox:inboxes){
			if(inbox.getHost().equalsIgnoreCase(originatingDomain))
				continue;
			executor.submit(new ForwardOneActivityRunnable(json, inbox, signer));
		}
	}

	private List<URI> getInboxesForPost(Post post) throws SQLException{
		ArrayList<URI> inboxes=new ArrayList<>();
		if(post.owner instanceof User){
			boolean sendToFollowers=((User) post.owner).id==post.user.id;
			if(post.owner instanceof ForeignUser){
				inboxes.add(actorInbox((ForeignUser) post.owner));
			}else if(sendToFollowers && post.getReplyLevel()==0){
				inboxes.addAll(UserStorage.getFollowerInboxes(((User) post.owner).id));
			}else{
				inboxes.addAll(PostStorage.getInboxesForPostInteractionForwarding(post));
			}
		}else if(post.owner instanceof Group){
			if(post.owner instanceof ForeignGroup){
				inboxes.add(actorInbox((ForeignGroup) post.owner));
			}else if(post.getReplyLevel()==0){
				inboxes.addAll(GroupStorage.getGroupMemberInboxes(((Group) post.owner).id));
			}else{
				inboxes.addAll(PostStorage.getInboxesForPostInteractionForwarding(post));
			}
		}
		for(User user:post.mentionedUsers){
			if(user instanceof ForeignUser){
				URI inbox=actorInbox((ForeignUser) user);
				if(!inboxes.contains(inbox))
					inboxes.add(inbox);
			}
		}
		return inboxes;
	}

	private void sendActivityForPost(Post post, Activity activity, Actor actor){
		try{
			List<URI> inboxes=getInboxesForPost(post);
			LOG.info("Inboxes: {}", inboxes);
			for(URI inbox:inboxes){
				executor.submit(new SendOneActivityRunnable(activity, inbox, actor));
			}
		}catch(SQLException x){
			LOG.error("Exception while sending activity for post {}", post.activityPubID, x);
		}
	}

	public void sendCreatePostActivity(final Post post){
		executor.submit(new Runnable(){
			@Override
			public void run(){
				Create create=new Create();
				create.object=new LinkOrObject(post);
				create.actor=new LinkOrObject(post.user.activityPubID);
				create.to=post.to;
				create.cc=post.cc;
				create.published=post.published;
				create.activityPubID=Config.localURI(post.activityPubID.getPath()+"/activityCreate");
				sendActivityForPost(post, create, post.user);
			}
		});
	}

	public void sendAddPostToWallActivity(final Post post){
		executor.submit(new Runnable(){
			@Override
			public void run(){
				try{
					Add add=new Add();
					add.activityPubID=UriBuilder.local().path("posts", post.id+"", "activityAdd").build();
					add.object=new LinkOrObject(post.activityPubID);
					add.actor=new LinkOrObject(post.owner.activityPubID);
					add.to=List.of(new LinkOrObject(ActivityPub.AS_PUBLIC), new LinkOrObject(post.owner.getFollowersURL()), new LinkOrObject(post.user.activityPubID));
					if(!post.mentionedUsers.isEmpty()){
						ArrayList<LinkOrObject> cc=new ArrayList<>();
						for(User user : post.mentionedUsers){
							cc.add(new LinkOrObject(user.activityPubID));
						}
						add.cc=cc;
					}
					add.target=new LinkOrObject(post.owner.getWallURL());

					ArrayList<URI> inboxes=new ArrayList<>();
					if(post.owner instanceof User)
						inboxes.addAll(UserStorage.getFollowerInboxes(((User) post.owner).id));
					else
						inboxes.addAll(GroupStorage.getGroupMemberInboxes(((Group) post.owner).id));

					for(User user:post.mentionedUsers){
						if(user instanceof ForeignUser){
							URI inbox=actorInbox((ForeignUser) user);
							if(!inboxes.contains(inbox))
								inboxes.add(inbox);
						}
					}
					if(post.user instanceof ForeignUser){
						URI inbox=actorInbox((ForeignUser) post.user);
						if(!inboxes.contains(inbox))
							inboxes.add(inbox);
					}

					for(URI inbox:inboxes){
						executor.submit(new SendOneActivityRunnable(add, inbox, post.owner));
					}
				}catch(SQLException x){
					LOG.error("Exception while sending wall post {}", post.activityPubID, x);
				}
			}
		});
	}

	public void sendDeletePostActivity(final Post post, final User actualActor){
		executor.submit(new Runnable(){
			@Override
			public void run(){
				Actor actor;
				Delete delete=new Delete();
				delete.object=new LinkOrObject(post.activityPubID);
				if(post.user.id==actualActor.id)
					actor=actualActor;
				else if(!post.isGroupOwner() && ((User)post.owner).id==actualActor.id)
					actor=actualActor;
				else if(post.isGroupOwner())
					actor=post.owner;
				else{
					LOG.error("Shouldn't happen: post {} actor for delete can't be chosen", post.id);
					return;
				}
				if(actor instanceof ForeignGroup || actor instanceof ForeignUser){
					LOG.error("Shouldn't happen: {} actor for delete is a foreign actor", post.id);
					return;
				}
				delete.actor=new LinkOrObject(actor.activityPubID);
				delete.to=post.to;
				delete.cc=post.cc;
				delete.published=new Date();
				delete.activityPubID=new UriBuilder(post.activityPubID).appendPath("delete").build();
				sendActivityForPost(post, delete, actor);
			}
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

	public void sendAddToGroupsCollectionActivity(User self, Group group){
		Add add=new Add();
		add.activityPubID=new UriBuilder(self.activityPubID).fragment("addGroupCollection"+group.id+"_"+rand()).build();
		add.actor=new LinkOrObject(self.activityPubID);
		add.object=new LinkOrObject(group.activityPubID);
		add.target=new LinkOrObject(self.getGroupsURL());

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

	public void sendFollowActivity(User self, ForeignUser target){
		Follow follow=new Follow();
		follow.actor=new LinkOrObject(self.activityPubID);
		follow.object=new LinkOrObject(target.activityPubID);
		follow.activityPubID=new UriBuilder(self.activityPubID).fragment("followUser"+target.id+"_"+rand()).build();
		executor.submit(new SendOneActivityRunnable(follow, target.inbox, self));
	}

	public void sendFollowActivity(User self, ForeignGroup target){
		Follow follow=new Follow();
		follow.actor=new LinkOrObject(self.activityPubID);
		follow.object=new LinkOrObject(target.activityPubID);
		follow.activityPubID=new UriBuilder(self.activityPubID).fragment("joinGroup"+target.id+"_"+rand()).build();
		executor.submit(new SendOneActivityRunnable(follow, target.inbox, self));
	}

	public void sendUnfollowActivity(User self, ForeignGroup target){
		Undo undo=new Undo();
		undo.activityPubID=new UriBuilder(self.activityPubID).fragment("leaveGroup"+target.id+"_"+rand()).build();
		undo.actor=new LinkOrObject(self.activityPubID);

		Follow follow=new Follow();
		follow.actor=new LinkOrObject(self.activityPubID);
		follow.object=new LinkOrObject(target.activityPubID);
		follow.activityPubID=new UriBuilder(self.activityPubID).fragment("joinGroup"+target.id+"_"+rand()).build();
		undo.object=new LinkOrObject(follow);

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
		like.object=new LinkOrObject(post.activityPubID);
		List<URI> inboxes=PostStorage.getInboxesForPostInteractionForwarding(post);
		LOG.info("Inboxes: {}", inboxes);
		for(URI inbox:inboxes){
			executor.submit(new SendOneActivityRunnable(like, inbox, user));
		}
	}

	public void sendUndoLikeActivity(Post post, User user, int likeID) throws SQLException{
		Like like=new Like();
		like.activityPubID=Config.localURI("/activitypub/objects/likes/"+likeID);
		like.actor=new LinkOrObject(user.activityPubID);
		like.object=new LinkOrObject(post.activityPubID);
		Undo undo=new Undo();
		undo.activityPubID=Config.localURI("/activitypub/objects/likes/"+likeID+"/undo");
		undo.object=new LinkOrObject(like);
		undo.actor=new LinkOrObject(user.activityPubID);
		ActivityPubCache.putUndoneLike(likeID, undo);
		List<URI> inboxes=PostStorage.getInboxesForPostInteractionForwarding(post);
		for(URI inbox:inboxes){
			executor.submit(new SendOneActivityRunnable(undo, inbox, user));
		}
	}

	public void sendBlockActivity(Actor self, ForeignUser target) throws SQLException{
		Block block=new Block();
		block.activityPubID=new UriBuilder(self.activityPubID).fragment("blockUser"+target.id+"_"+System.currentTimeMillis()).build();
		block.actor=new LinkOrObject(self.activityPubID);
		block.object=new LinkOrObject(target.activityPubID);
		executor.submit(new SendOneActivityRunnable(block, target.inbox, self));
	}

	public void sendUndoBlockActivity(Actor self, ForeignUser target) throws SQLException{
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
			vote.name=opt.name;
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
			create.published=new Date();

			executor.submit(new SendOneActivityRunnable(create, pollOwner.inbox, self));
		}
	}

	public synchronized Future<List<Post>> fetchReplyThread(Post post){
		return fetchingReplyThreads.computeIfAbsent(post.activityPubID, (uri)->executor.submit(new FetchReplyThreadRunnable(post)));
	}

	public synchronized Future<List<Post>> fetchReplyThreadAndThen(Post post, Consumer<List<Post>> action){
		afterFetchReplyThreadActions.computeIfAbsent(post.activityPubID, (uri)->new ArrayList<>()).add(action);
		return fetchReplyThread(post);
	}

	public synchronized Future<Post> fetchAllReplies(Post post){
		return fetchingAllReplies.computeIfAbsent(post.activityPubID, (uri)->executor.submit(new FetchAllRepliesTask(post)));
	}

	private static class SendOneActivityRunnable implements Runnable{
		private Activity activity;
		private URI destination;
		private Actor actor;

		public SendOneActivityRunnable(Activity activity, URI destination, Actor actor){
			this.activity=activity;
			this.destination=destination;
			this.actor=actor;
		}

		@Override
		public void run(){
			try{
				ActivityPub.postActivity(destination, activity, actor);
			}catch(Exception x){
				LOG.error("Exception while sending activity", x);
			}
		}
	}

	private static class SendActivitySequenceRunnable implements Runnable{
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
			try{
				for(Activity activity:activities)
					ActivityPub.postActivity(destination, activity, user);
			}catch(Exception x){
				LOG.error("Exception while sending activity", x);
			}
		}
	}

	private static class ForwardOneActivityRunnable implements Runnable{
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
				ActivityPub.postActivity(destination, activity, user);
			}catch(Exception x){
				LOG.error("Exception while forwarding activity", x);
			}
		}
	}

	private static class FetchReplyThreadRunnable implements Callable<List<Post>>{
		private ArrayList<Post> thread=new ArrayList<>();
		private Set<URI> seenPosts=new HashSet<>();
		private Post initialPost;

		public FetchReplyThreadRunnable(Post post){
			thread.add(post);
			initialPost=post;
		}

		@Override
		public List<Post> call() throws Exception{
			LOG.debug("Started fetching parent thread for post {}", initialPost.activityPubID);
			seenPosts.add(initialPost.activityPubID);
			while(thread.get(0).inReplyTo!=null){
				Post post=ObjectLinkResolver.resolve(thread.get(0).inReplyTo, Post.class, true, false, false);
				if(seenPosts.contains(post.activityPubID)){
					LOG.warn("Already seen post {} while fetching parent thread for {}", post.activityPubID, initialPost.activityPubID);
					throw new IllegalStateException("Reply thread contains a loop of links");
				}
				seenPosts.add(post.activityPubID);
				thread.add(0, post);
			}
			Post topLevel=thread.get(0);
			for(int i=0;i<thread.size();i++){
				Post p=thread.get(i);
				if(p.id!=0)
					continue;
				p.storeDependencies();
				Post prev=null;
				if(i>0){
					prev=thread.get(i-1);
					p.setParent(prev);
				}
				if(StringUtils.isNotEmpty(p.content))
					p.content=Utils.sanitizeHTML(p.content);
				if(StringUtils.isNotEmpty(p.summary))
					p.summary=Utils.sanitizeHTML(p.summary);
				Utils.loadAndPreprocessRemotePostMentions(p);
				PostStorage.putForeignWallPost(p);
				NotificationUtils.putNotificationsForPost(p, prev);
			}
			LOG.info("Done fetching parent thread for post {}", topLevel.activityPubID);
			synchronized(instance){
				instance.fetchingReplyThreads.remove(initialPost.activityPubID);
				List<Consumer<List<Post>>> actions=instance.afterFetchReplyThreadActions.remove(initialPost.activityPubID);
				if(actions!=null){
					for(Consumer<List<Post>> action:actions){
						instance.executor.submit(()->action.accept(thread));
					}
				}
				return thread;
			}
		}
	}

	private static class FetchAllRepliesTask extends RecursiveTask<Post>{
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
			LOG.debug("Started fetching full reply tree for post {}", post.activityPubID);
			try{
				if(post.replies==null){
					if(post.local){
						post.repliesObjects=PostStorage.getRepliesExact(post.getReplyKeyForReplies(), Integer.MAX_VALUE, 1000, null);
					}else{
						return post;
					}
				}else{
					ActivityPubCollection collection;
					if(post.replies.link!=null){
						collection=ObjectLinkResolver.resolve(post.replies.link, ActivityPubCollection.class, true, false, false);
						collection.validate(post.activityPubID, "replies");
					}else if(post.replies.object instanceof ActivityPubCollection){
						collection=(ActivityPubCollection) post.replies.object;
					}else{
						LOG.warn("Post {} doesn't have a replies collection", post.activityPubID);
						return post;
					}
					LOG.trace("collection: {}", collection);
					if(collection.first==null){
						LOG.warn("Post {} doesn't have replies.first", post.activityPubID);
						return post;
					}
					CollectionPage page;
					if(collection.first.link!=null){
						page=ObjectLinkResolver.resolve(collection.first.link, CollectionPage.class, true, false, false);
						page.validate(post.activityPubID, "replies.first");
					}else if(collection.first.object instanceof CollectionPage){
						page=(CollectionPage) collection.first.object;
					}else{
						LOG.warn("Post {} doesn't have a correct CollectionPage in replies.first", post.activityPubID);
						return post;
					}
					LOG.trace("first page: {}", page);
					if(page.items!=null && !page.items.isEmpty()){
						doOneCollectionPage(page.items);
					}
					while(page.next!=null){
						LOG.trace("getting next page: {}", page.next);
						page=ObjectLinkResolver.resolve(page.next, CollectionPage.class, true, false, false);
						if(page.items==null){ // you're supposed to not return the "next" field when there are no more pages, but mastodon still does...
							LOG.debug("done fetching replies because page.items is empty");
							break;
						}
						doOneCollectionPage(page.items);
					}
				}
			}catch(Exception x){
				completeExceptionally(x);
			}
			if(post.getReplyLevel()==0){
				synchronized(instance){
					instance.fetchingAllReplies.remove(post.activityPubID);
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
				}else if(item.object instanceof Post){
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
					post=(Post) item.object;
					post.setParent(this.post);
					post.resolveDependencies(true, true);
					PostStorage.putForeignWallPost(post);
					LOG.trace("got post: {}", post);
					FetchAllRepliesTask subtask=new FetchAllRepliesTask(post, seenPosts);
					subtasks.add(subtask);
					subtask.fork();
				}else{
					LOG.warn("reply object isn't a post: {}", item.object);
					continue;
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

	private static class FetchPostAndRepliesTask extends FetchAllRepliesTask{
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
				post=ObjectLinkResolver.resolve(postID, Post.class, true, false, false);
				post.setParent(parentPost);
				post.storeDependencies();
				PostStorage.putForeignWallPost(post);
			}catch(Exception x){
				completeExceptionally(x);
			}
			return super.compute();
		}
	}
}
