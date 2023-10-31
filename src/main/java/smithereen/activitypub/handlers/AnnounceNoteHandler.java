package smithereen.activitypub.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.model.ForeignUser;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.notifications.Notification;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;

public class AnnounceNoteHandler extends ActivityTypeHandler<ForeignUser, Announce, NoteOrQuestion>{
	private static final Logger LOG=LoggerFactory.getLogger(AnnounceNoteHandler.class);

	private Announce activity;
	private ForeignUser actor;

	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Announce activity, NoteOrQuestion post) throws SQLException{
		this.activity=activity;
		this.actor=actor;
		if(post.inReplyTo!=null){
			Post parent=PostStorage.getPostByID(post.inReplyTo);
			if(parent!=null){
				Post nativePost=post.asNativePost(context.appContext);
				context.appContext.getWallController().loadAndPreprocessRemotePostMentions(nativePost, post);
				context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(nativePost);
				doHandle(nativePost, context);
			}else{
				context.appContext.getActivityPubWorker().fetchReplyThreadAndThen(post, thread->onReplyThreadDone(thread, context));
			}
		}else{
			Post nativePost=post.asNativePost(context.appContext);
			context.appContext.getWallController().loadAndPreprocessRemotePostMentions(nativePost, post);
			context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(nativePost);
			doHandle(nativePost, context);
			context.appContext.getActivityPubWorker().fetchAllReplies(nativePost);
		}
	}

	private void onReplyThreadDone(List<Post> thread, ActivityHandlerContext context){
		try{
			doHandle(thread.get(thread.size()-1), context);
		}catch(SQLException x){
			LOG.warn("Error storing retoot", x);
		}
	}

	private void doHandle(Post post, ActivityHandlerContext context) throws SQLException{
		long time=activity.published==null ? System.currentTimeMillis() : activity.published.toEpochMilli();
		context.appContext.getNewsfeedController().putFriendsFeedEntry(actor, post.id, NewsfeedEntry.Type.RETOOT, Instant.ofEpochMilli(time));
		User author=context.appContext.getUsersController().getUserOrThrow(post.authorID);

		if(!(author instanceof ForeignUser)){
			Notification n=new Notification();
			n.type=Notification.Type.RETOOT;
			n.actorID=actor.id;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			NotificationsStorage.putNotification(post.authorID, n);
		}
	}
}
