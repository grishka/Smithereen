package smithereen.activitypub.tasks;

import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.Future;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.model.ForeignUser;
import smithereen.model.Post;

public class FetchUserPinnedPostsTask extends ForwardPaginatingCollectionTask{
	private final ForeignUser user;
	private boolean first=true;
	private final HashMap<URI, Future<Void>> fetchingUserPinnedPosts;
	private final ActivityPubWorker apw;

	public FetchUserPinnedPostsTask(ApplicationContext context, ForeignUser user, HashMap<URI, Future<Void>> fetchingUserPinnedPosts, ActivityPubWorker apw){
		super(context, user.getPinnedPostsURL());
		this.user=user;
		this.fetchingUserPinnedPosts=fetchingUserPinnedPosts;
		this.apw=apw;
	}

	@Override
	protected void compute(){
		try{
			super.compute();
		}finally{
			synchronized(apw){
				fetchingUserPinnedPosts.remove(user.activityPubID);
			}
		}
	}

	@Override
	protected void doOneCollectionPage(ActivityPubCollection page){
		if(first){
			context.getWallController().clearPinnedPosts(user);
			first=false;
		}
		for(LinkOrObject lo:page.items){
			if(lo.object instanceof NoteOrQuestion noq){
				try{
					Post post=noq.asNativePost(context);
					context.getObjectLinkResolver().storeOrUpdateRemoteObject(post, noq);
					context.getWallController().pinPost(post, true);
				}catch(Exception x){
					LOG.debug("Error processing post {}", lo, x);
				}
			}
		}
	}
}
