package smithereen.activitypub.handlers;

import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubBoardTopic;
import smithereen.activitypub.objects.NoteOrQuestion;
import smithereen.activitypub.objects.activities.Create;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.FederationException;
import smithereen.model.ForeignGroup;
import smithereen.model.board.BoardTopic;

public class GroupCreateBoardTopicHandler extends ActivityTypeHandler<ForeignGroup, Create, ActivityPubBoardTopic>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Create activity, ActivityPubBoardTopic object){
		BoardTopic topic=object.asNativeTopic(context.appContext);
		if(topic.groupID!=actor.id)
			throw new BadRequestException("Group ID does not match the topic group");
		if(topic.id!=0){
			LOG.debug("Topic {} is already stored locally. Ignoring Create{BoardTopic}", object.activityPubID);
			return;
		}

		NoteOrQuestion firstComment=context.appContext.getObjectLinkResolver().resolve(object.firstCommentID, NoteOrQuestion.class, true, false, false);
		if(!Objects.equals(firstComment.attributedTo, object.authorID))
			throw new FederationException("Topic author ID does not match first comment author ID");
		if(!(firstComment.target instanceof ActivityPubBoardTopic target) || !Objects.equals(target.activityPubID, object.activityPubID))
			throw new FederationException("The first comment's `target` is not this topic");
		context.appContext.getBoardController().putForeignTopic(topic, firstComment);
	}
}
