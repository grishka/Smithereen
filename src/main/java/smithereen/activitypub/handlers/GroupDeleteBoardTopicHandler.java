package smithereen.activitypub.handlers;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubBoardTopic;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.ForeignGroup;
import smithereen.model.board.BoardTopic;

public class GroupDeleteBoardTopicHandler extends ActivityTypeHandler<ForeignGroup, Delete, ActivityPubBoardTopic>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Delete activity, ActivityPubBoardTopic object){
		BoardTopic topic=object.asNativeTopic(context.appContext);
		if(topic.groupID!=actor.id)
			throw new UserActionNotAllowedException("This topic does not belong to this group");
		context.appContext.getBoardController().deleteTopic(topic);
	}
}
