package smithereen.activitypub.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.data.notifications.Notification;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;

public class DeleteNoteHandler extends ActivityTypeHandler<ForeignUser, Delete, NoteOrQuestion>{
	private static final Logger LOG=LoggerFactory.getLogger(DeleteNoteHandler.class);

	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Delete activity, NoteOrQuestion post) throws SQLException{
		Post nativePost=PostStorage.getPostByID(post.activityPubID);
		if(nativePost==null){
			LOG.debug("Deleted post {} does not exist anyway", post.activityPubID);
			return;
		}
		if(nativePost.canBeManagedBy(actor)){
			PostStorage.deletePost(nativePost.id);
			NotificationsStorage.deleteNotificationsForObject(Notification.ObjectType.POST, nativePost.id);
			if(nativePost.getReplyLevel()>0){
				Post topLevel=PostStorage.getPostByID(nativePost.replyKey.get(0), false);
				if(topLevel!=null && topLevel.isLocal()){
					if(context.ldSignatureOwner!=null)
						context.forwardActivity(PostStorage.getInboxesForPostInteractionForwarding(topLevel), context.appContext.getUsersController().getUserOrThrow(topLevel.authorID));
				}
			}
		}else{
			throw new IllegalArgumentException("No access to delete this post");
		}
	}
}
