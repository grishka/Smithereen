package smithereen.activitypub.handlers;

import java.sql.SQLException;
import java.util.Objects;

import smithereen.activitypub.ActivityForwardingUtils;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Update;
import smithereen.model.ForeignUser;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.Post;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableContentObject;
import smithereen.storage.PostStorage;

public class UpdateNoteHandler extends ActivityTypeHandler<ForeignUser, Update, NoteOrQuestion>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Update activity, NoteOrQuestion post) throws SQLException{
		Object existing;
		try{
			existing=context.appContext.getObjectLinkResolver().resolveLocally(post.activityPubID, Object.class);
		}catch(ObjectNotFoundException x){
			LOG.debug("Original object {} for Update{Note} not found, ignoring activity", post.activityPubID);
			return;
		}
		if(existing instanceof Post existingPost){
			Post updated=post.asNativePost(context.appContext);
			if(updated.authorID!=existingPost.authorID || actor.id!=existingPost.authorID)
				throw new IllegalArgumentException("No access to update this post");

			if(post.inReplyTo==null && updated.ownerID!=existingPost.ownerID){
				throw new IllegalArgumentException("Post owner doesn't match existing");
			}
			if(!Objects.equals(updated.replyKey, existingPost.replyKey)){
				throw new IllegalArgumentException("inReplyTo doesn't match existing");
			}

			context.appContext.getWallController().loadAndPreprocessRemotePostMentions(updated, post);
			PostStorage.putForeignWallPost(updated);
			if(!updated.isGroupOwner() && updated.ownerID==updated.authorID){
				context.appContext.getNewsfeedController().clearFriendsFeedCache();
			}
		}else if(existing instanceof Comment existingComment){
			Comment updated=post.asNativeComment(context.appContext);
			if(updated.authorID!=existingComment.authorID || actor.id!=existingComment.authorID)
				throw new IllegalArgumentException("No access to update this post");
			if(!Objects.equals(updated.replyKey, existingComment.replyKey)){
				throw new IllegalArgumentException("inReplyTo doesn't match existing");
			}
			context.appContext.getWallController().loadAndPreprocessRemotePostMentions(updated, post);
			context.appContext.getCommentsController().putOrUpdateForeignComment(updated);
			ActivityForwardingUtils.forwardCommentInteraction(context, updated);
		}
	}
}
