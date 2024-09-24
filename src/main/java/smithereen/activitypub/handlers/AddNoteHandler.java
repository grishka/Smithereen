package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;
import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Add;
import smithereen.model.Post;
import smithereen.exceptions.BadRequestException;

public class AddNoteHandler extends ActivityTypeHandler<Actor, Add, NoteOrQuestion>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Add activity, NoteOrQuestion post) throws SQLException{
		URI targetCollectionID;
		if(activity.target.link!=null)
			targetCollectionID=activity.target.link;
		else if(activity.target.object instanceof ActivityPubCollection collection)
			targetCollectionID=collection.activityPubID;
		else
			throw new BadRequestException("Add.target is required (either wall collection ID or abbreviated collection object)");

		if(!Objects.equals(actor.getWallURL(), targetCollectionID))
			throw new BadRequestException("Add.target doesn't match actor's wall collection");
//		if(!Objects.equals(post.owner.activityPubID, actor.activityPubID))
//			throw new BadRequestException("Post's target collection doesn't match actor's wall collection");
		Post nativePost=post.asNativePost(context.appContext);
		if(post.inReplyTo!=null){
			Post topLevel=context.appContext.getWallController().getPostOrThrow(nativePost.getReplyChainElement(0));
			if(nativePost.ownerID!=topLevel.ownerID)
				throw new BadRequestException("Reply must have target set to top-level post owner's wall");
		}

		context.appContext.getWallController().loadAndPreprocessRemotePostMentions(nativePost, post);
		context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(nativePost);
		context.appContext.getNotificationsController().createNotificationsForObject(nativePost);
	}
}
