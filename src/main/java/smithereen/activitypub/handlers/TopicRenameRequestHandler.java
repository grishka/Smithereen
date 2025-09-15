package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubBoardTopic;
import smithereen.activitypub.objects.activities.TopicRenameRequest;
import smithereen.exceptions.BadRequestException;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.board.BoardTopic;

public class TopicRenameRequestHandler extends ActivityTypeHandler<ForeignUser, TopicRenameRequest, ActivityPubBoardTopic>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, TopicRenameRequest activity, ActivityPubBoardTopic object){
		if(activity.name==null)
			throw new BadRequestException("`name` is required");
		String title=activity.name.trim();
		if(title.isEmpty())
			throw new BadRequestException("`name` is required");
		BoardTopic topic=object.asNativeTopic(context.appContext);
		Group group=context.appContext.getGroupsController().getLocalGroupOrThrow(topic.groupID);
		context.appContext.getBoardController().renameTopic(actor, topic, title);
	}
}
