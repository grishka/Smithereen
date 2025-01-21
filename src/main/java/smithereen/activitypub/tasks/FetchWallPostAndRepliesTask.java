package smithereen.activitypub.tasks;

import java.net.URI;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.Future;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.model.Post;
import smithereen.storage.PostStorage;

public class FetchWallPostAndRepliesTask extends FetchAllWallRepliesTask{
	private final URI postID;
	private final Post parentPost;

	public FetchWallPostAndRepliesTask(ActivityPubWorker apw, ApplicationContext context, HashMap<URI, Future<Post>> fetchingAllReplies, URI postID, Post parentPost, Set<URI> seenPosts){
		super(apw, context, fetchingAllReplies, null, seenPosts);
		this.postID=postID;
		this.parentPost=parentPost;
	}

	@Override
	public Post call() throws Exception{
		LOG.trace("Fetching remote reply from {}", postID);
		NoteOrQuestion noq=context.getObjectLinkResolver().resolve(postID, NoteOrQuestion.class, true, false, false, context.getWallController().getContentAuthorAndOwner(parentPost).owner(), true);
		post=noq.asNativePost(context);
		context.getWallController().loadAndPreprocessRemotePostMentions(post, noq);
		PostStorage.putForeignWallPost(post);
		return super.call();
	}
}
