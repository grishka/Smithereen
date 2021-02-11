package smithereen.activitypub;

import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.Mention;
import smithereen.activitypub.objects.Tombstone;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Create;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Like;
import smithereen.activitypub.objects.activities.Offer;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.activitypub.objects.activities.Update;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.Post;
import smithereen.data.UriBuilder;
import smithereen.data.User;
import smithereen.data.notifications.NotificationUtils;
import smithereen.storage.GroupStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.utils.StringUtils;

public class ActivityPubWorker{
	private static final ActivityPubWorker instance=new ActivityPubWorker();

	private ExecutorService executor;
	private Random rand=new Random();

	public static ActivityPubWorker getInstance(){
		return instance;
	}

	private ActivityPubWorker(){
		executor=Executors.newCachedThreadPool();
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
			}else if(sendToFollowers){
				if(post.getReplyLevel()==0)
					inboxes.addAll(UserStorage.getFollowerInboxes(((User) post.owner).id));
				else
					inboxes.addAll(PostStorage.getInboxesForPostInteractionForwarding(post));
			}
		}else if(post.owner instanceof Group){
			if(post.owner instanceof ForeignGroup){
				inboxes.add(actorInbox((ForeignGroup) post.owner));
			}else{
				if(post.getReplyLevel()==0)
					inboxes.addAll(GroupStorage.getGroupMemberInboxes(((Group) post.owner).id));
				else
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
			System.out.println("Inboxes: "+inboxes);
			for(URI inbox:inboxes){
				executor.submit(new SendOneActivityRunnable(activity, inbox, actor));
			}
		}catch(SQLException x){
			x.printStackTrace();
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

	public void sendDeletePostActivity(final Post post){
		executor.submit(new Runnable(){
			@Override
			public void run(){
				Delete delete=new Delete();
				delete.object=new LinkOrObject(post.activityPubID);
				delete.actor=new LinkOrObject(post.user.activityPubID);
				delete.to=post.to;
				delete.cc=post.cc;
				delete.published=new Date();
				try{
					delete.activityPubID=new URI(post.activityPubID.getScheme(), post.activityPubID.getSchemeSpecificPart(), "delete");
				}catch(URISyntaxException ignore){}
				sendActivityForPost(post, delete, post.user);
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
			x.printStackTrace();
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
			x.printStackTrace();
		}
	}

	public void sendLikeActivity(Post post, User user, int likeID) throws SQLException{
		Like like=new Like();
		like.activityPubID=Config.localURI("/activitypub/objects/likes/"+likeID);
		like.actor=new LinkOrObject(user.activityPubID);
		like.object=new LinkOrObject(post.activityPubID);
		List<URI> inboxes=PostStorage.getInboxesForPostInteractionForwarding(post);
		System.out.println("Inboxes:\n"+inboxes.stream().map(URI::toString).collect(Collectors.joining("\n")));
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

	public void fetchReplyThread(Post post){
		executor.submit(new FetchReplyThreadRunnable(post));
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
				x.printStackTrace();
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
				x.printStackTrace();
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
				x.printStackTrace();
			}
		}
	}

	private static class FetchReplyThreadRunnable implements Runnable{
		private ArrayList<Post> thread=new ArrayList<>();
		private Post initialPost;

		public FetchReplyThreadRunnable(Post post){
			thread.add(post);
			initialPost=post;
		}

		@Override
		public void run(){
			try{
				while(thread.get(0).inReplyTo!=null){
					Post _post=PostStorage.getPostByID(thread.get(0).inReplyTo);
					if(_post==null){
						ActivityPubObject obj=ActivityPub.fetchRemoteObject(thread.get(0).inReplyTo.toString());
						if(obj instanceof Post){
							thread.add(0, (Post) obj);
						}else if(obj!=null){
							throw new IllegalArgumentException("Incorrect parent object type "+obj.getType());
						}else{
							throw new IllegalArgumentException("Failed to fetch "+thread.get(0).inReplyTo);
						}
					}else{
						thread.add(0, _post);
					}
				}
				Post topLevel=thread.get(0);
				if(topLevel.owner==null){
					boolean mentionsLocalUsers=false;
					for(User user:initialPost.mentionedUsers){
						if(!(user instanceof ForeignUser)){
							mentionsLocalUsers=true;
							break;
						}
					}
					if(!mentionsLocalUsers){
						System.out.println("Top-level post from unknown user "+topLevel.attributedTo+" and there are no local user mentions — dropping the thread");
						return;
					}
				}
				for(int i=0;i<thread.size();i++){
					Post p=thread.get(i);
					if(p.id!=0)
						continue;
					if(p.owner==null){
						ActivityPubObject owner=ActivityPub.fetchRemoteObject(p.attributedTo.toString());
						if(owner instanceof ForeignUser){
							UserStorage.putOrUpdateForeignUser((ForeignUser) owner);
							p.owner=p.user=(ForeignUser)owner;
						}else{
							throw new IllegalArgumentException("Failed to get owner for post "+p.activityPubID);
						}
					}
					Post prev=null;
					if(i>0){
						prev=thread.get(i-1);
						p.setParent(prev);
					}
					if(StringUtils.isNotEmpty(p.content))
						p.content=Utils.sanitizeHTML(p.content);
					if(StringUtils.isNotEmpty(p.summary))
						p.summary=Utils.sanitizeHTML(p.summary);
					if(p.tag!=null){
						for(ActivityPubObject tag:p.tag){
							if(tag instanceof Mention){
								URI uri=((Mention) tag).href;
								User mentionedUser=UserStorage.getUserByActivityPubID(uri);
								if(mentionedUser!=null){
									if(p.mentionedUsers.isEmpty())
										p.mentionedUsers=new ArrayList<>();
									if(!p.mentionedUsers.contains(mentionedUser))
										p.mentionedUsers.add(mentionedUser);
								}
							}
						}
					}
					PostStorage.putForeignWallPost(p);
					NotificationUtils.putNotificationsForPost(p, prev);
				}
				System.out.println("Done fetching parent thread for "+topLevel.activityPubID);
			}catch(Exception x){
				x.printStackTrace();
			}
		}
	}
}
