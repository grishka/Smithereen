package smithereen.activitypub;

import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.Tombstone;
import smithereen.activitypub.objects.activities.Create;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.activitypub.objects.activities.Offer;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;

public class ActivityPubWorker{
	private static final ActivityPubWorker instance=new ActivityPubWorker();

	private ExecutorService executor;

	public static ActivityPubWorker getInstance(){
		return instance;
	}

	private ActivityPubWorker(){
		executor=Executors.newCachedThreadPool();
	}

	private URI actorInbox(ForeignUser actor){
		return actor.sharedInbox!=null ? actor.sharedInbox : actor.inbox;
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
				try{
					create.activityPubID=new URI(post.activityPubID.getScheme(), post.activityPubID.getSchemeSpecificPart()+"/activityCreate", null);
				}catch(URISyntaxException ignore){}
				try{
					boolean sendToFollowers=post.owner.id==post.user.id;
					ArrayList<URI> inboxes=new ArrayList<>();
					if(sendToFollowers){
						inboxes.addAll(UserStorage.getFollowerInboxes(post.owner.id));
					}else if(post.owner instanceof ForeignUser){
						inboxes.add(((ForeignUser)post.owner).inbox);
					}
					for(User user:post.mentionedUsers){
						if(user instanceof ForeignUser){
							URI inbox=actorInbox((ForeignUser) user);
							if(!inboxes.contains(inbox))
								inboxes.add(inbox);
						}
					}
					System.out.println("Inboxes: "+inboxes);
					for(URI inbox:inboxes){
						executor.submit(new SendOneActivityRunnable(create, inbox, post.user));
					}
				}catch(SQLException x){
					x.printStackTrace();
				}
			}
		});
	}

	public void sendDeletePostActivity(final Post post){
		executor.submit(new Runnable(){
			@Override
			public void run(){
				Tombstone ts=new Tombstone();
				ts.activityPubID=post.activityPubID;
				ts.formerType="Note";

				Delete delete=new Delete();
				delete.object=new LinkOrObject(ts);
				delete.actor=new LinkOrObject(post.user.activityPubID);
				delete.to=post.to;
				delete.cc=post.cc;
				delete.published=new Date();
				try{
					delete.activityPubID=new URI(post.activityPubID.getScheme(), post.activityPubID.getSchemeSpecificPart(), "delete");
				}catch(URISyntaxException ignore){}
				try{
					boolean sendToFollowers=post.owner.id==post.user.id;
					List<URI> inboxes;
					if(sendToFollowers){
						inboxes=UserStorage.getFollowerInboxes(post.owner.id);
					}else if(post.owner instanceof ForeignUser){
						inboxes=Collections.singletonList(((ForeignUser) post.owner).inbox);
					}else{
						return;
					}
					System.out.println("Inboxes: "+inboxes);
					for(URI inbox:inboxes){
						executor.submit(new SendOneActivityRunnable(delete, inbox, post.user));
					}
				}catch(SQLException x){
					x.printStackTrace();
				}
			}
		});
	}

	public void sendUnfriendActivity(User self, User target){
		if(!(target instanceof ForeignUser))
			return;
		try{
			Undo undo=new Undo();
			undo.activityPubID=new URI(self.activityPubID.getScheme(), self.activityPubID.getSchemeSpecificPart(), "unfollow"+target.id);
			undo.actor=new LinkOrObject(self.activityPubID);

			Follow follow=new Follow();
			follow.actor=new LinkOrObject(self.activityPubID);
			follow.object=new LinkOrObject(target.activityPubID);
			follow.activityPubID=new URI(self.activityPubID.getScheme(), self.activityPubID.getSchemeSpecificPart(), "follow"+target.id);
			undo.object=new LinkOrObject(follow);

			executor.submit(new SendOneActivityRunnable(undo, ((ForeignUser) target).inbox, self));
		}catch(URISyntaxException ignore){}
	}

	public void sendFollowActivity(User self, ForeignUser target){
		Follow follow=new Follow();
		follow.actor=new LinkOrObject(self.activityPubID);
		follow.object=new LinkOrObject(target.activityPubID);
		follow.activityPubID=URI.create(self.activityPubID+"#follow"+target.id);
		executor.submit(new SendOneActivityRunnable(follow, target.inbox, self));
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
		reject.activityPubID=URI.create(self.activityPubID+"#reject_friend_req"+target.id);
		executor.submit(new SendOneActivityRunnable(reject, target.inbox, self));
	}

	public void fetchReplyThread(Post post){
		executor.submit(new FetchReplyThreadRunnable(post));
	}

	private static class SendOneActivityRunnable implements Runnable{
		private Activity activity;
		private URI destination;
		private User user;

		public SendOneActivityRunnable(Activity activity, URI destination, User user){
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

		public FetchReplyThreadRunnable(Post post){
			thread.add(post);
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
					System.out.println("Post from unknown user "+topLevel.attributedTo);
					return;
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
					if(i>0){
						Post prev=thread.get(i-1);
						p.setParent(prev);
					}
					PostStorage.putForeignWallPost(p);
				}
				System.out.println("Done fetching parent thread for "+topLevel.activityPubID);
			}catch(Exception x){
				x.printStackTrace();
			}
		}
	}
}
