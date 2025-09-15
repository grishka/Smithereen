package smithereen.activitypub.tasks;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Future;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.model.Post;

public class FetchActorWallTask extends ForwardPaginatingCollectionTask{
	private final Actor actor;
	private final ActivityPubWorker apw;
	private final HashMap<URI, Future<Post>> fetchingAllReplies;

	public FetchActorWallTask(ActivityPubWorker apw, ApplicationContext context, HashMap<URI, Future<Post>> fetchingAllReplies, Actor actor){
		super(context, actor.getWallURL());
		this.actor=actor;
		maxItems=ActivityPubWorker.MAX_COMMENTS;
		this.apw=apw;
		this.fetchingAllReplies=fetchingAllReplies;
	}

	@Override
	protected void doOneCollectionPage(ActivityPubCollection page){
		ArrayList<ProcessWallPostTask> tasks=new ArrayList<>();
		for(LinkOrObject lo: page.items){
			try{
				if(lo.object instanceof NoteOrQuestion post){
					if(post.inReplyTo==null)
						tasks.add(new ProcessWallPostTask(apw, context, fetchingAllReplies, post, actor));
				}else if(lo.link!=null){
					tasks.add(new FetchAndProcessWallPostTask(apw, context, fetchingAllReplies, actor, lo.link));
				}
			}catch(Exception x){
				LOG.debug("Error processing post {}", lo, x);
			}
		}
		apw.invokeAll(tasks);
	}
}
