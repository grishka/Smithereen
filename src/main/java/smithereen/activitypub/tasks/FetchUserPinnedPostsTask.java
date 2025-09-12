package smithereen.activitypub.tasks;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
	private final HashMap<URI, Future<Void>> fetchingUserPinnedPosts;
	private final ActivityPubWorker apw;
	private ArrayList<Post> posts=new ArrayList<>();

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
			List<Integer> existingPostIDs=context.getWallController().getPinnedPosts(user, user).stream().map(p->p.id).toList();
			List<Integer> newPostIDs=posts.stream().map(p->p.id).toList();
			if(existingPostIDs.equals(newPostIDs))
				return;
			context.getWallController().clearPinnedPosts(user);
			for(Post post:posts)
				context.getWallController().pinPost(post, true);
		}finally{
			synchronized(apw){
				fetchingUserPinnedPosts.remove(user.activityPubID);
			}
		}
	}

	@Override
	protected void doOneCollectionPage(ActivityPubCollection page){
		for(LinkOrObject lo:page.items){
			if(lo.object instanceof NoteOrQuestion noq){
				try{
					Post post=noq.asNativePost(context);
					context.getObjectLinkResolver().storeOrUpdateRemoteObject(post, noq);
					posts.addFirst(post); // Posts are returned in reverse order
				}catch(Exception x){
					LOG.debug("Error processing post {}", lo, x);
				}
			}
		}
	}
}
