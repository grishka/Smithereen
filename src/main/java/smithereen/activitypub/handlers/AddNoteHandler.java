package smithereen.activitypub.handlers;

import java.sql.SQLException;
import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.activities.Add;
import smithereen.controllers.WallController;
import smithereen.data.Post;
import smithereen.data.notifications.NotificationUtils;
import smithereen.exceptions.BadRequestException;

public class AddNoteHandler extends ActivityTypeHandler<Actor, Add, Post>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Add activity, Post post) throws SQLException{
		if(!Objects.equals(actor.getWallURL(), activity.target.link))
			throw new BadRequestException("Add.target doesn't match actor's wall collection");
		if(!Objects.equals(post.owner.activityPubID, actor.activityPubID))
			throw new BadRequestException("Post's target collection doesn't match actor's wall collection");
		if(post.inReplyTo!=null)
			throw new BadRequestException("Post can't be a reply");

		context.appContext.getWallController().loadAndPreprocessRemotePostMentions(post);
		context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(post);
		NotificationUtils.putNotificationsForPost(post, null);
	}
}
