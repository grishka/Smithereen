package smithereen.activitypub.handlers;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

import smithereen.activitypub.ActivityForwardingUtils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.model.ForeignUser;
import smithereen.model.MailMessage;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.Post;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.comments.CommentableObjectType;
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
		}else if(nativeObj instanceof Comment comment){
			handleForComment(comment, actor, context);
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
			}else{
				context.appContext.getNewsfeedController().clearFriendsFeedCache();
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

	private void handleForComment(Comment comment, ForeignUser actor, ActivityHandlerContext context){
		boolean needForward=true;
		if(comment.parentObjectID.type()==CommentableObjectType.BOARD_TOPIC)
			needForward=context.appContext.getBoardController().getTopicIgnoringPrivacy(comment.parentObjectID.id()).firstCommentID!=comment.id;

		context.appContext.getCommentsController().deleteComment(actor, comment);

		if(needForward)
			ActivityForwardingUtils.forwardCommentInteraction(context, comment);
	}
}
