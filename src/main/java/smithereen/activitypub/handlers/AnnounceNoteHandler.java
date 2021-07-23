package smithereen.activitypub.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import smithereen.ObjectLinkResolver;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.Activity;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.BadRequestException;
import smithereen.Utils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Mention;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.data.notifications.Notification;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;
import spark.utils.StringUtils;

public class AnnounceNoteHandler extends ActivityTypeHandler<ForeignUser, Announce, Post>{
	private static final Logger LOG=LoggerFactory.getLogger(AnnounceNoteHandler.class);

	private Announce activity;
	private ForeignUser actor;

	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Announce activity, Post post) throws SQLException{
		this.activity=activity;
		this.actor=actor;
		Utils.loadAndPreprocessRemotePostMentions(post);
		if(post.inReplyTo!=null){
			Post parent=PostStorage.getPostByID(post.inReplyTo);
			if(parent!=null){
				post.setParent(parent);
				ObjectLinkResolver.storeOrUpdateRemoteObject(post);
				doHandle(post);
			}else{
				ActivityPubWorker.getInstance().fetchReplyThreadAndThen(post, this::onReplyThreadDone);
			}
		}else{
			ObjectLinkResolver.storeOrUpdateRemoteObject(post);
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
		long time=activity.published==null ? System.currentTimeMillis() : activity.published.getTime();
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
