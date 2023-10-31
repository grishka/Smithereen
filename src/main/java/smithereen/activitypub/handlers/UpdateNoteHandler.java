package smithereen.activitypub.handlers;

import java.sql.SQLException;
import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Update;
import smithereen.model.ForeignUser;
import smithereen.model.Post;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.PostStorage;

public class UpdateNoteHandler extends ActivityTypeHandler<ForeignUser, Update, NoteOrQuestion>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Update activity, NoteOrQuestion post) throws SQLException{
		Post existing;
		try{
			existing=context.appContext.getWallController().getPostOrThrow(post.activityPubID);
		}catch(ObjectNotFoundException x){
			LOG.debug("Original post {} for Update{Note} not found, ignoring activity", post.activityPubID);
			return;
		}
		Post updated=post.asNativePost(context.appContext);
		if(updated.authorID!=existing.authorID || actor.id!=existing.authorID)
			throw new IllegalArgumentException("No access to update this post");

		if(post.inReplyTo==null && updated.ownerID!=existing.ownerID){
			throw new IllegalArgumentException("Post owner doesn't match existing");
		}
		if(!Objects.equals(updated.replyKey, existing.replyKey)){
			throw new IllegalArgumentException("inReplyTo doesn't match existing");
		}

		context.appContext.getWallController().loadAndPreprocessRemotePostMentions(updated, post);
		PostStorage.putForeignWallPost(updated);
		if(!updated.isGroupOwner() && updated.ownerID==updated.authorID){
			context.appContext.getNewsfeedController().clearFriendsFeedCache();
		}
	}
}
