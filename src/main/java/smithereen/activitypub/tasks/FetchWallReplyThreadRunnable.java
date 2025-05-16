package smithereen.activitypub.tasks;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.model.Post;

public class FetchWallReplyThreadRunnable implements Callable<List<Post>>{
	private static final Logger LOG=LoggerFactory.getLogger(FetchWallReplyThreadRunnable.class);

	private final LinkedList<NoteOrQuestion> thread=new LinkedList<>();
	private final Set<URI> seenPosts=new HashSet<>();
	private final NoteOrQuestion initialPost;
	private final ActivityPubWorker apw;
	private final HashMap<URI, List<Consumer<List<Post>>>> afterFetchReplyThreadActions;
	private final ApplicationContext context;
	private final HashMap<URI, Future<List<Post>>> fetchingReplyThreads;

	public FetchWallReplyThreadRunnable(ActivityPubWorker apw, HashMap<URI, List<Consumer<List<Post>>>> afterFetchReplyThreadActions, ApplicationContext context, HashMap<URI, Future<List<Post>>> fetchingReplyThreads, NoteOrQuestion post){
		thread.add(post);
		initialPost=post;
		this.apw=apw;
		this.afterFetchReplyThreadActions=afterFetchReplyThreadActions;
		this.context=context;
		this.fetchingReplyThreads=fetchingReplyThreads;
	}

	@Override
	public List<Post> call() throws Exception{
		final ArrayList<Post> realThread=new ArrayList<>();
		try{
			LOG.debug("Started fetching parent thread for post {}", initialPost.activityPubID);
			seenPosts.add(initialPost.activityPubID);
			while(thread.getFirst().inReplyTo!=null){
				NoteOrQuestion post=context.getObjectLinkResolver().resolve(thread.getFirst().inReplyTo, NoteOrQuestion.class, true, false, false, (JsonObject) null, true);
				if(seenPosts.contains(post.activityPubID)){
					LOG.warn("Already seen post {} while fetching parent thread for {}", post.activityPubID, initialPost.activityPubID);
					throw new IllegalStateException("Reply thread contains a loop of links");
				}
				seenPosts.add(post.activityPubID);
				thread.addFirst(post);
			}
			NoteOrQuestion topLevel=thread.getFirst();
			Post parent=null;
			for(NoteOrQuestion noq: thread){
				Post p=noq.asNativePost(context);

				if(p.id!=0){
					realThread.add(p);
					parent=p;
					continue;
				}
				if(noq.inReplyTo==null && noq.getQuoteRepostID()!=null){
					List<Post> repostChain=context.getActivityPubWorker().fetchRepostChain(noq).get();
					if(!repostChain.isEmpty()){
						p.setRepostedPost(repostChain.getFirst());
					}
				}
				context.getWallController().loadAndPreprocessRemotePostMentions(p, noq);
				context.getObjectLinkResolver().storeOrUpdateRemoteObject(p, noq);
				context.getNotificationsController().createNotificationsForObject(p);
				realThread.add(p);
				parent=p;
			}
			LOG.debug("Done fetching parent thread for post {}", topLevel.activityPubID);
			return realThread;
		}finally{
			synchronized(apw){
				fetchingReplyThreads.remove(initialPost.activityPubID);
				List<Consumer<List<Post>>> actions=afterFetchReplyThreadActions.remove(initialPost.activityPubID);
				if(actions!=null){
					for(Consumer<List<Post>> action:actions){
						apw.submitTask(()->action.accept(realThread));
					}
				}
			}
		}
	}
}
