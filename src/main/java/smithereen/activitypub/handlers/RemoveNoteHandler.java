package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;
import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.ForeignUser;
import smithereen.model.Post;
import smithereen.model.comments.Comment;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.notifications.Notification;
import smithereen.storage.MediaStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;

public class RemoveNoteHandler extends ActivityTypeHandler<Actor, Remove, NoteOrQuestion>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Remove activity, NoteOrQuestion object) throws SQLException{
		Object existingObject=context.appContext.getObjectLinkResolver().resolveLocally(object.activityPubID, Object.class);
		if(existingObject instanceof Comment comment){
			context.appContext.getCommentsController().deleteComment(actor, comment);
		}else if(existingObject instanceof Post post){
			URI target=activity.target.link;
			if(target==null)
				throw new BadRequestException("Remove.target is required");
			if(Objects.equals(actor.getWallCommentsURL(), target) || Objects.equals(actor.getWallURL(), target)){
				if(post.canBeManagedBy(actor)){
					PostStorage.deletePost(post.id);
					NotificationsStorage.deleteNotificationsForObject(Notification.ObjectType.POST, post.id);
					if(post.isLocal() && post.attachments!=null){
						MediaStorage.deleteMediaFileReferences(post.id, MediaFileReferenceType.WALL_ATTACHMENT);
					}
				}else{
					throw new UserActionNotAllowedException("Can't delete this post");
				}
			}else if(actor instanceof ForeignUser user && Objects.equals(user.getPinnedPostsURL(), target)){
				context.appContext.getWallController().unpinPost(post);
			}
		}
	}
}
