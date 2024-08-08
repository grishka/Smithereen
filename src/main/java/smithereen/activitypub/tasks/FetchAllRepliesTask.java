package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.Post;
import smithereen.storage.PostStorage;

public class FetchAllRepliesTask implements Callable<Post>{
	protected static final Logger LOG=LoggerFactory.getLogger(FetchAllRepliesTask.class);

	private final ActivityPubWorker apw;
	protected final ApplicationContext context;
	private final HashMap<URI, Future<Post>> fetchingAllReplies;
	protected Post post;
	/**
	 * This keeps track of all the posts we've seen in this comment thread, to prevent a DoS via infinite recursion.
	 * NB: used from multiple threads simultaneously
	 */
	protected final Set<URI> seenPosts;

	public FetchAllRepliesTask(ActivityPubWorker apw, ApplicationContext context, HashMap<URI, Future<Post>> fetchingAllReplies, Post post, Set<URI> seenPosts){
		this.post=post;
		this.seenPosts=seenPosts;
		this.apw=apw;
		this.context=context;
		this.fetchingAllReplies=fetchingAllReplies;
	}

	public FetchAllRepliesTask(ActivityPubWorker apw, ApplicationContext context, HashMap<URI, Future<Post>> fetchingAllReplies, Post post){
		this(apw, context, fetchingAllReplies, post, new HashSet<>());
		if(post.getReplyLevel()>0)
			throw new IllegalArgumentException("This constructor is only for top-level posts");
	}

	@Override
	public Post call() throws Exception{
		LOG.debug("Started fetching full reply tree for post {}", post.getActivityPubID());
		if(post.activityPubReplies==null){
			if(!post.isLocal()){
				return post;
			}
		}else{
			Actor owner=context.getWallController().getContentAuthorAndOwner(post).owner();

			ActivityPubCollection collection;
			collection=context.getObjectLinkResolver().resolve(post.activityPubReplies, ActivityPubCollection.class, true, false, false, owner, true);
			collection.validate(post.getActivityPubID(), "replies");
			LOG.trace("collection: {}", collection);
			if(collection.first==null){
				LOG.warn("Post {} doesn't have replies.first", post.getActivityPubID());
				return post;
			}
			CollectionPage page;
			if(collection.first.link!=null){
				page=context.getObjectLinkResolver().resolve(collection.first.link, CollectionPage.class, true, false, false, owner, false);
				page.validate(post.getActivityPubID(), "replies.first");
			}else if(collection.first.object instanceof CollectionPage){
				page=(CollectionPage) collection.first.object;
			}else{
				LOG.warn("Post {} doesn't have a correct CollectionPage in replies.first", post.getActivityPubID());
				return post;
			}
			LOG.trace("first page: {}", page);
			if(page.items!=null && !page.items.isEmpty()){
				doOneCollectionPage(page.items);
			}
			while(page.next!=null){
				LOG.trace("getting next page: {}", page.next);
				try{
					page=context.getObjectLinkResolver().resolve(page.next, CollectionPage.class, true, false, false, owner, false);
					if(page.items==null){ // you're supposed to not return the "next" field when there are no more pages, but mastodon still does...
						LOG.debug("done fetching replies because page.items is empty");
						break;
					}
					doOneCollectionPage(page.items);
				}catch(ObjectNotFoundException x){
					LOG.warn("Failed to get replies collection page for post {}", post.getActivityPubID());
					return post;
				}
			}
		}
		if(post.getReplyLevel()==0){
			synchronized(apw){
				fetchingAllReplies.remove(post.getActivityPubID());
				return post;
			}
		}
		return post;
	}

	private void doOneCollectionPage(List<LinkOrObject> page) throws Exception{
		ArrayList<Future<Post>> subtasks=new ArrayList<>();
		for(LinkOrObject item: page){
			Post post;
			if(item.link!=null){
				synchronized(seenPosts){
					if(seenPosts.contains(item.link)){
						LOG.warn("Already seen post {}", item.link);
						continue;
					}
					if(seenPosts.size()>=ActivityPubWorker.MAX_COMMENTS){
						LOG.warn("Reached limit of {} on comment thread length. Stopping.", ActivityPubWorker.MAX_COMMENTS);
						return;
					}
					seenPosts.add(item.link);
				}
				FetchPostAndRepliesTask subtask=new FetchPostAndRepliesTask(apw, context, fetchingAllReplies, item.link, this.post, seenPosts);
				subtasks.add(apw.submitTask(subtask));
			}else if(item.object instanceof NoteOrQuestion noq){
				synchronized(seenPosts){
					if(seenPosts.contains(item.object.activityPubID)){
						LOG.warn("Already seen post {}", item.object.activityPubID);
						continue;
					}
					if(seenPosts.size()>=ActivityPubWorker.MAX_COMMENTS){
						LOG.warn("Reached limit of {} on comment thread length. Stopping.", ActivityPubWorker.MAX_COMMENTS);
						return;
					}
					seenPosts.add(item.object.activityPubID);
				}
				post=noq.asNativePost(context);
				context.getWallController().loadAndPreprocessRemotePostMentions(post, noq);
				PostStorage.putForeignWallPost(post);
				LOG.trace("got post: {}", post);
				FetchAllRepliesTask subtask=new FetchAllRepliesTask(apw, context, fetchingAllReplies, post, seenPosts);
				subtasks.add(apw.submitTask(subtask));
			}else{
				LOG.warn("reply object isn't a post: {}", item.object);
			}
		}
		for(Future<Post> task: subtasks){
			try{
				task.get();
			}catch(Exception x){
				LOG.warn("error fetching reply", x);
			}
		}
	}
}
