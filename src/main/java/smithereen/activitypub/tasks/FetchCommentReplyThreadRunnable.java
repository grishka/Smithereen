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
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.model.Post;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentReplyParent;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.photos.Photo;

public class FetchCommentReplyThreadRunnable implements Callable<List<CommentReplyParent>>{
	private static final Logger LOG=LoggerFactory.getLogger(FetchCommentReplyThreadRunnable.class);

	private final LinkedList<ActivityPubObject> thread=new LinkedList<>();
	private final Set<URI> seenPosts=new HashSet<>();
	private final NoteOrQuestion initialPost;
	private final ActivityPubWorker apw;
	private final HashMap<URI, List<Consumer<List<CommentReplyParent>>>> afterFetchReplyThreadActions;
	private final ApplicationContext context;
	private final HashMap<URI, Future<List<CommentReplyParent>>> fetchingReplyThreads;

	public FetchCommentReplyThreadRunnable(ActivityPubWorker apw, HashMap<URI, List<Consumer<List<CommentReplyParent>>>> afterFetchReplyThreadActions, ApplicationContext context, HashMap<URI, Future<List<CommentReplyParent>>> fetchingReplyThreads, NoteOrQuestion post){
		thread.add(post);
		initialPost=post;
		this.apw=apw;
		this.afterFetchReplyThreadActions=afterFetchReplyThreadActions;
		this.context=context;
		this.fetchingReplyThreads=fetchingReplyThreads;
	}

	@Override
	public List<CommentReplyParent> call() throws Exception{
		try{
			LOG.debug("Started fetching parent thread for comment {}", initialPost.activityPubID);
			seenPosts.add(initialPost.activityPubID);
			while(thread.getFirst().inReplyTo!=null){
				ActivityPubObject post=context.getObjectLinkResolver().resolve(thread.getFirst().inReplyTo, ActivityPubObject.class, true, false, false, (JsonObject) null, true);
				if(seenPosts.contains(post.activityPubID)){
					LOG.warn("Already seen comment {} while fetching parent thread for {}", post.activityPubID, initialPost.activityPubID);
					throw new IllegalStateException("Reply thread contains a loop of links");
				}
				seenPosts.add(post.activityPubID);
				thread.addFirst(post);
			}
			ActivityPubObject topLevel=thread.getFirst();
			final ArrayList<CommentReplyParent> realThread=new ArrayList<>();
			for(ActivityPubObject obj: thread){
				CommentReplyParent p=context.getObjectLinkResolver().convertToNativeObject(obj, CommentReplyParent.class);

				if((p instanceof Comment c && c.id!=0) || (p instanceof CommentableContentObject cco && cco.getObjectID()!=0)){
					realThread.add(p);
					continue;
				}
				if(p instanceof Comment comment)
					context.getWallController().loadAndPreprocessRemotePostMentions(comment, (NoteOrQuestion) obj);
				context.getObjectLinkResolver().storeOrUpdateRemoteObject(p, obj);
				realThread.add(p);
			}
			LOG.debug("Done fetching parent thread for object {}", topLevel.activityPubID);
			List<Consumer<List<CommentReplyParent>>> actions=afterFetchReplyThreadActions.remove(initialPost.activityPubID);
			if(actions!=null){
				for(Consumer<List<CommentReplyParent>> action: actions){
					apw.submitTask(()->action.accept(realThread));
				}
			}
			return realThread;
		}finally{
			synchronized(apw){
				fetchingReplyThreads.remove(initialPost.activityPubID);
			}
		}
	}
}
