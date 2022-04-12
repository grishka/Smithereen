package smithereen.activitypub.handlers;

import java.sql.SQLException;
import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Update;
import smithereen.controllers.WallController;
import smithereen.data.ForeignUser;
import smithereen.data.Post;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.PostStorage;

public class UpdateNoteHandler extends ActivityTypeHandler<ForeignUser, Update, Post>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Update activity, Post post) throws SQLException{
		if(post.user.id==actor.id){
			Post existing=PostStorage.getPostByID(post.activityPubID);
			if(existing==null){
				throw new ObjectNotFoundException("Existing post not found");
			}
			if(post.user.id!=existing.user.id){
				throw new IllegalArgumentException("Post author doesn't match existing");
			}
			if(post.inReplyTo==null && !post.owner.activityPubID.equals(existing.owner.activityPubID)){
				throw new IllegalArgumentException("Post owner doesn't match existing");
			}
			if(!Objects.equals(post.inReplyTo, existing.inReplyTo)){
				throw new IllegalArgumentException("inReplyTo doesn't match existing");
			}
			context.appContext.getWallController().loadAndPreprocessRemotePostMentions(post);
			PostStorage.putForeignWallPost(post);
		}else{
			throw new IllegalArgumentException("No access to update this post");
		}
	}
}
