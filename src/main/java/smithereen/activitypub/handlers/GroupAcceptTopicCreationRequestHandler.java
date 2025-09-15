package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.Config;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubBoardTopic;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.TopicCreationRequest;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.ForeignGroup;
import smithereen.model.board.BoardTopic;
import smithereen.util.UriBuilder;

public class GroupAcceptTopicCreationRequestHandler extends ActivityTypeHandler<ForeignGroup, Accept, TopicCreationRequest>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Accept activity, TopicCreationRequest object) throws SQLException{
		URI localID=object.activityPubID;
		if(localID==null || !Config.isLocal(localID))
			throw new BadRequestException("TopicCreationRequest `id` is invalid");
		localID=new UriBuilder(localID).fragment(null).build();
		ObjectLinkResolver.ObjectTypeAndID tid=ObjectLinkResolver.getObjectIdFromLocalURL(localID);
		if(tid==null || tid.type()!=ObjectLinkResolver.ObjectType.BOARD_TOPIC)
			throw new BadRequestException("TopicCreationRequest `id` is invalid");

		BoardTopic topic=context.appContext.getBoardController().getTopicIgnoringPrivacy(tid.id());
		if(topic.groupID!=actor.id)
			throw new UserActionNotAllowedException();

		if(activity.result==null || activity.result.isEmpty() || !(activity.result.getFirst().object instanceof ActivityPubBoardTopic result))
			throw new BadRequestException("`result` must be a BoardTopic");

		context.appContext.getBoardController().setTopicActivityPubID(topic, result.activityPubID, result.url==null ? result.activityPubID : result.url);
	}
}
