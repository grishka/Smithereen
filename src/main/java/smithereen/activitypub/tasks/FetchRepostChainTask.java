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
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.Post;

public class FetchRepostChainTask implements Callable<List<Post>>{
	private static final Logger LOG=LoggerFactory.getLogger(FetchRepostChainTask.class);

	private final NoteOrQuestion topLevel;
	private final ActivityPubWorker apw;
	private final ApplicationContext context;
	private final HashMap<URI, Future<List<Post>>> fetchingRepostChains;

	public FetchRepostChainTask(ActivityPubWorker apw, ApplicationContext context, HashMap<URI, Future<List<Post>>> fetchingRepostChains, NoteOrQuestion topLevel){
		this.topLevel=topLevel;
		this.apw=apw;
		this.context=context;
		this.fetchingRepostChains=fetchingRepostChains;
	}

	@Override
	public List<Post> call() throws Exception{
		LOG.trace("Fetching repost chain for {}", topLevel.activityPubID);
		try{
			HashSet<URI> seenPostIDs=new HashSet<>();
			ArrayList<Post> repostChain=new ArrayList<>();
			HashMap<URI, NoteOrQuestion> origObjects=new HashMap<>();
			URI nextUri=topLevel.getQuoteRepostID();
			int depth=1;
			while(nextUri!=null && !seenPostIDs.contains(nextUri) && depth<ActivityPubWorker.MAX_REPOST_DEPTH){
				try{
					Post localPost=context.getObjectLinkResolver().resolveLocally(nextUri, Post.class);
					repostChain.add(localPost);
					break;
				}catch(ObjectNotFoundException ignored){
				}
				try{
					seenPostIDs.add(nextUri);
					NoteOrQuestion post=context.getObjectLinkResolver().resolve(nextUri, NoteOrQuestion.class, true, false, false);
					nextUri=post.getQuoteRepostID();
					if(nextUri==null && post.inReplyTo!=null){
						try{
							context.getWallController().getPostOrThrow(post.inReplyTo);
						}catch(ObjectNotFoundException x){
							List<Post> thread=context.getActivityPubWorker().fetchWallReplyThread(post).get();
							if(!thread.isEmpty()){
								context.getActivityPubWorker().fetchAllReplies(thread.getFirst());
							}
						}
					}
					Post nativePost=post.asNativePost(context);
					context.getWallController().loadAndPreprocessRemotePostMentions(nativePost, post);
					repostChain.add(nativePost);
					origObjects.put(post.activityPubID, post);
				}catch(ObjectNotFoundException x){
					LOG.debug("Failed to fetch a complete repost chain for {}, failed at {}, stopping at depth {}", topLevel.activityPubID, nextUri, depth, x);
					break;
				}
				depth++;
			}
			for(int i=repostChain.size()-1;i>=0;i--){
				Post post=repostChain.get(i);
				if(post.id==0)
					context.getObjectLinkResolver().storeOrUpdateRemoteObject(post, origObjects.get(post.getActivityPubID()));
				if(i==0)
					break;
				Post prevPost=repostChain.get(i-1);
				prevPost.setRepostedPost(post);
			}
			return repostChain;
		}finally{
			synchronized(apw){
				fetchingRepostChains.remove(topLevel.activityPubID);
			}
		}
	}
}
