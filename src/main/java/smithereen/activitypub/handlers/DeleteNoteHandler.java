package smithereen.activitypub.handlers;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.model.ForeignUser;
import smithereen.model.MailMessage;
import smithereen.model.Post;
import smithereen.model.notifications.Notification;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.MailStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;

public class DeleteNoteHandler extends ActivityTypeHandler<ForeignUser, Delete, NoteOrQuestion>{
	private static final Logger LOG=LoggerFactory.getLogger(DeleteNoteHandler.class);

	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Delete activity, NoteOrQuestion post) throws SQLException{
		Object nativeObj;
		try{
			nativeObj=context.appContext.getObjectLinkResolver().resolveNative(post.activityPubID, Object.class, false, false, false, (JsonObject) null, false);
		}catch(ObjectNotFoundException x){
			LOG.debug("Deleted object {} does not exist anyway", post.activityPubID);
			return;
		}
		if(nativeObj instanceof Post nativePost){
			handleForPost(nativePost, actor, context);
		}else if(nativeObj instanceof MailMessage msg){
			handleForMessage(msg, actor, context);
		}
	}

	private void handleForPost(Post nativePost, ForeignUser actor, ActivityHandlerContext context) throws SQLException{
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
			throw new BadRequestException("No access to delete this post");
		}
	}

	private void handleForMessage(MailMessage msg, ForeignUser actor, ActivityHandlerContext context) throws SQLException{
		if(msg.senderID!=actor.id){
			LOG.debug("Actor {} does not have access to delete message {}", actor.activityPubID, msg.getActivityPubID());
			return;
		}
		List<MailMessage> allMessages;
		if(msg.getTotalRecipientCount()==1){
			allMessages=List.of(msg);
		}else if(msg.activityPubID!=null){
			allMessages=MailStorage.getMessages(msg.getActivityPubID());
		}else{
			LOG.debug("Remote actor {} tried to delete a local message {}", actor.activityPubID, msg.getActivityPubID());
			return;
		}
		allMessages.stream().filter(MailMessage::isUnread).forEach(m->{
			context.appContext.getMailController().actuallyDeleteMessage(actor, msg, false);
		});
	}
}
