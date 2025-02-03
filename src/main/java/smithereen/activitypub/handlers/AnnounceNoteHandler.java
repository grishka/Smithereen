package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.ForeignUser;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.notifications.Notification;
import smithereen.storage.PostStorage;

public class AnnounceNoteHandler extends ActivityTypeHandler<ForeignUser, Announce, NoteOrQuestion>{

	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Announce activity, NoteOrQuestion post) throws SQLException{
		if(post.inReplyTo!=null){
			Post parent=PostStorage.getPostByID(post.inReplyTo);
			if(parent!=null){
				Post nativePost=post.asNativePost(context.appContext);
				context.appContext.getWallController().loadAndPreprocessRemotePostMentions(nativePost, post);
				context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(nativePost, post);
				doHandle(nativePost, actor, activity, context);
			}else{
				context.appContext.getActivityPubWorker().fetchWallReplyThreadAndThen(post, thread->onReplyThreadDone(thread, actor, activity, context));
			}
		}else{
			Post nativePost=post.asNativePost(context.appContext);
			if(post.getQuoteRepostID()!=null){
				try{
					List<Post> repostChain=context.appContext.getActivityPubWorker().fetchRepostChain(post).get();
					if(!repostChain.isEmpty()){
						nativePost.setRepostedPost(repostChain.getFirst());
					}
				}catch(InterruptedException x){
					throw new RuntimeException(x);
				}catch(ExecutionException x){
					LOG.debug("Failed to fetch repost chain for {}", post.activityPubID, x);
				}
			}
			context.appContext.getWallController().loadAndPreprocessRemotePostMentions(nativePost, post);
			context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(nativePost, post);
			doHandle(nativePost, actor, activity, context);
			context.appContext.getActivityPubWorker().fetchAllReplies(nativePost);
		}
	}

	private void onReplyThreadDone(List<Post> thread, ForeignUser actor, Announce activity, ActivityHandlerContext context){
		try{
			doHandle(thread.get(thread.size()-1), actor, activity, context);
		}catch(SQLException x){
			LOG.warn("Error storing retoot", x);
		}
	}

	private void doHandle(Post post, ForeignUser actor, Announce activity, ActivityHandlerContext context) throws SQLException{
		Post repost=new Post();
		repost.authorID=repost.ownerID=actor.id;
		repost.repostOf=post.id;
		repost.flags.add(Post.Flag.MASTODON_STYLE_REPOST);
		repost.createdAt=activity.published==null ? Instant.now() : activity.published;
		repost.setActivityPubID(activity.activityPubID);
		repost.privacy=post.privacy;
		if(activity.url!=null){
			repost.activityPubURL=activity.url;
		}else{
			// Remove "/activity" off the end of the activity ID to get the ID for the "pseudo-post". Mastodon and Misskey don't expose IDs of these pseudo-posts themselves.
			// I'm very sorry I have to do this.
			// Mastodon example: https://raspberry.grishka.me/users/grishka/statuses/112293152682340239/activity
			// Misskey example: https://misskey.io/notes/9s92azzu76fm045q/activity
			if(activity.activityPubID.getRawPath().endsWith("/activity")){
				String idStr=activity.activityPubID.toString();
				repost.activityPubURL=URI.create(idStr.substring(0, idStr.lastIndexOf('/')));
			}else{
				// Oh no! Anyway...
				repost.activityPubURL=activity.activityPubID;
			}
		}
		PostStorage.putForeignWallPost(repost);
		context.appContext.getNewsfeedController().clearFriendsFeedCache();

		User author=context.appContext.getUsersController().getUserOrThrow(post.authorID);
		Post topLevel=null;
		if(post.getReplyLevel()>0){
			try{
				topLevel=context.appContext.getWallController().getPostOrThrow(post.replyKey.getFirst());
			}catch(ObjectNotFoundException ignore){}
		}
		context.appContext.getNotificationsController().createNotification(author, Notification.Type.RETOOT, post, topLevel, actor);
	}
}
