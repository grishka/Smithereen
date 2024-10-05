package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.model.comments.Comment;

public class RemoveNoteHandler extends ActivityTypeHandler<Actor, Remove, NoteOrQuestion>{
	@Override
	public void handle(ActivityHandlerContext context, Actor actor, Remove activity, NoteOrQuestion object) throws SQLException{
		Object existingObject=context.appContext.getObjectLinkResolver().resolveLocally(object.activityPubID, Object.class);
		if(existingObject instanceof Comment comment){
			context.appContext.getCommentsController().deleteComment(actor, comment);
		}
	}
}
