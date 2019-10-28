package smithereen.activitypub;

import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.activities.Create;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.User;
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
						executor.submit(new SendOneActivityRunnable(create, inbox, post.user));
					}
				}catch(SQLException x){
					x.printStackTrace();
				}
			}
		});
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
}
