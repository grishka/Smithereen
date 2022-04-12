package smithereen.activitypub.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.controllers.WallController;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.data.notifications.Notification;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;

public class AnnounceNoteHandler extends ActivityTypeHandler<ForeignUser, Announce, Post>{
	private static final Logger LOG=LoggerFactory.getLogger(AnnounceNoteHandler.class);

	private Announce activity;
	private ForeignUser actor;

	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Announce activity, Post post) throws SQLException{
		this.activity=activity;
		this.actor=actor;
		context.appContext.getWallController().loadAndPreprocessRemotePostMentions(post);
		if(post.inReplyTo!=null){
			Post parent=PostStorage.getPostByID(post.inReplyTo);
			if(parent!=null){
				post.setParent(parent);
				context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(post);
				doHandle(post);
			}else{
				context.appContext.getActivityPubWorker().fetchReplyThreadAndThen(post, this::onReplyThreadDone);
			}
		}else{
			context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(post);
			doHandle(post);
		}
	}

	private void onReplyThreadDone(List<Post> thread){
		try{
			doHandle(thread.get(thread.size()-1));
		}catch(SQLException x){
			LOG.warn("Error storing retoot", x);
		}
	}

	private void doHandle(Post post) throws SQLException{
		long time=activity.published==null ? System.currentTimeMillis() : activity.published.toEpochMilli();
		NewsfeedStorage.putEntry(actor.id, post.id, NewsfeedEntry.Type.RETOOT, new Timestamp(time));

		if(!(post.user instanceof ForeignUser)){
			Notification n=new Notification();
			n.type=Notification.Type.RETOOT;
			n.actorID=actor.id;
			n.objectID=post.id;
			n.objectType=Notification.ObjectType.POST;
			NotificationsStorage.putNotification(post.user.id, n);
		}
	}
}
