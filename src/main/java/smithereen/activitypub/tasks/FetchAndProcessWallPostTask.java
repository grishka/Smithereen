package smithereen.activitypub.tasks;

import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.Future;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.model.Post;

public class FetchAndProcessWallPostTask extends ProcessWallPostTask{
	private final URI postID;

	public FetchAndProcessWallPostTask(ActivityPubWorker apw, ApplicationContext context, HashMap<URI, Future<Post>> fetchingAllReplies, Actor owner, URI postID){
		super(apw, context, fetchingAllReplies, owner);
		this.postID=postID;
	}

	@Override
	protected void compute(){
		try{
			post=context.getObjectLinkResolver().resolve(postID, NoteOrQuestion.class, true, false, false, owner, false);
			if(post.inReplyTo!=null)
				return;
		}catch(Exception x){
			LOG.debug("Error fetching post {}", postID, x);
		}
		super.compute();
	}
}
