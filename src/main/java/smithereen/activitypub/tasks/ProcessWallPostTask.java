package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.model.Post;
import smithereen.util.NoResultCallable;

public class ProcessWallPostTask extends NoResultCallable{
	protected static final Logger LOG=LoggerFactory.getLogger(ProcessWallPostTask.class);

	protected final ActivityPubWorker apw;
	protected final ApplicationContext context;
	protected NoteOrQuestion post;
	protected final Actor owner;
	private final HashMap<URI, Future<Post>> fetchingAllReplies;

	public ProcessWallPostTask(ActivityPubWorker apw, ApplicationContext context, HashMap<URI, Future<Post>> fetchingAllReplies, NoteOrQuestion post, Actor owner){
		this.post=post;
		this.owner=owner;
		this.apw=apw;
		this.context=context;
		this.fetchingAllReplies=fetchingAllReplies;
	}

	public ProcessWallPostTask(ActivityPubWorker apw, ApplicationContext context, HashMap<URI, Future<Post>> fetchingAllReplies, Actor owner){
		this(apw, context, fetchingAllReplies, null, owner);
	}

	@Override
	protected void compute(){
		try{
			Post nativePost=post.asNativePost(context);
			if(post.inReplyTo==null && post.getQuoteRepostID()!=null){
				List<Post> repostChain=context.getActivityPubWorker().fetchRepostChain(post).get();
				if(!repostChain.isEmpty()){
					nativePost.setRepostedPost(repostChain.getFirst());
				}
			}
			context.getWallController().loadAndPreprocessRemotePostMentions(nativePost, post);
			context.getObjectLinkResolver().storeOrUpdateRemoteObject(nativePost);
			apw.submitTask(new FetchAllWallRepliesTask(apw, context, fetchingAllReplies, nativePost)).get();
		}catch(Exception x){
			LOG.debug("Error processing post {}", post.activityPubID, x);
		}
	}
}
