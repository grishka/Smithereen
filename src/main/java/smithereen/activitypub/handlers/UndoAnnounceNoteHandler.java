package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Announce;
import smithereen.activitypub.objects.activities.Undo;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.ForeignUser;
import smithereen.model.Post;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.notifications.Notification;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;

public class UndoAnnounceNoteHandler extends NestedActivityTypeHandler<ForeignUser, Undo, Announce, NoteOrQuestion>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Undo activity, Announce nested, NoteOrQuestion _post) throws SQLException{
		Post post;
		Post repost;
		try{
			repost=context.appContext.getWallController().getPostOrThrow(nested.activityPubID);
		}catch(ObjectNotFoundException x){
			LOG.debug("Repost {} for Undo{Announce{Note}} not found", nested.activityPubID, x);
			return;
		}
		if(repost.ownerID!=actor.id)
			throw new UserActionNotAllowedException("Post "+repost.getActivityPubID()+" is not owned by actor "+actor.activityPubID);
		try{
			post=context.appContext.getWallController().getPostOrThrow(_post.activityPubID);
		}catch(ObjectNotFoundException x){
			LOG.debug("Reposted post {} for Undo{Announce{Note}} not found", _post.activityPubID, x);
			return;
		}
		if(repost.repostOf!=post.id || !repost.flags.contains(Post.Flag.MASTODON_STYLE_REPOST))
			throw new BadRequestException("Post "+repost.getActivityPubID()+" is not a repost of "+post.getActivityPubID());

		PostStorage.deletePost(repost.id);
		context.appContext.getNewsfeedController().clearFriendsFeedCache();
		NotificationsStorage.deleteNotification(Notification.ObjectType.POST, post.id, Notification.Type.RETOOT, actor.id);
	}
}
