package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.TopicCreationRequest;
import smithereen.exceptions.BadRequestException;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.board.BoardTopic;

public class TopicCreationRequestHandler extends ActivityTypeHandler<ForeignUser, TopicCreationRequest, NoteOrQuestion>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, TopicCreationRequest activity, NoteOrQuestion object) throws SQLException{
		if(activity.to==null || activity.to.size()!=1)
			throw new BadRequestException("`to` must contain exactly one URI and it must be the group ID in which to create topic");
		URI groupID=activity.to.getFirst().link;
		if(groupID==null)
			throw new BadRequestException("`to` must contain a link");
		Group group=context.appContext.getObjectLinkResolver().resolveLocally(groupID, Group.class);
		if(group instanceof ForeignGroup)
			throw new BadRequestException("Group must be local");
		if(activity.name==null)
			throw new BadRequestException("`name` must be present");
		String title=activity.name.trim();
		if(title.isEmpty())
			throw new BadRequestException("`name` must not be empty");

		BoardTopic topic=context.appContext.getBoardController().createTopicWithExistingFirstComment(actor, group, title, object);

		TopicCreationRequest request=new TopicCreationRequest();
		request.to=activity.to;
		request.activityPubID=activity.activityPubID;
		request.name=activity.name;
		request.actor=activity.actor;
		request.object=new LinkOrObject(object.activityPubID);
		context.appContext.getActivityPubWorker().sendAcceptCreateBoardTopicRequest(group, actor, topic, request);
		context.appContext.getActivityPubWorker().sendCreateBoardTopic(group, topic, context.appContext.getCommentsController().getCommentIgnoringPrivacy(topic.firstCommentID));
	}
}
