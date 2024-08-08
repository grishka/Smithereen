package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.Actor;
import smithereen.model.Post;
import smithereen.util.NoResultCallable;

public class FetchActorContentCollectionsTask extends NoResultCallable{
	private static final Logger LOG=LoggerFactory.getLogger(FetchActorContentCollectionsTask.class);

	private final Actor actor;
	private final ActivityPubWorker apw;
	private final ApplicationContext context;
	private final HashMap<URI, Future<Post>> fetchingAllReplies;
	private final HashSet<URI> fetchingContentCollectionsActors;

	public FetchActorContentCollectionsTask(ActivityPubWorker apw, ApplicationContext context, HashMap<URI, Future<Post>> fetchingAllReplies, HashSet<URI> fetchingContentCollectionsActors, Actor actor){
		this.actor=actor;
		this.apw=apw;
		this.context=context;
		this.fetchingAllReplies=fetchingAllReplies;
		this.fetchingContentCollectionsActors=fetchingContentCollectionsActors;
	}

	@Override
	protected void compute(){
		List<Callable<?>> tasks=new ArrayList<>();
		if(actor.hasWall()){
			tasks.add(new FetchActorWallTask(apw, context, fetchingAllReplies, actor));
		}
		try{
			apw.invokeAll(tasks);
		}catch(Throwable x){
			LOG.warn("Error fetching content collections for {}", actor.activityPubID, x);
		}
		synchronized(apw){
			fetchingContentCollectionsActors.remove(actor.activityPubID);
		}
		LOG.debug("Done fetching content collections for {}", actor.activityPubID);
	}
}
