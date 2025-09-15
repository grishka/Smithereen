package smithereen.activitypub.objects;

import smithereen.ApplicationContext;
import smithereen.model.board.BoardTopic;

public class LocalActivityPubBoardTopic extends ActivityPubBoardTopic{
	public final BoardTopic topic;

	public LocalActivityPubBoardTopic(BoardTopic topic){
		this.topic=topic;
	}

	@Override
	public BoardTopic asNativeTopic(ApplicationContext ctx){
		return topic;
	}
}
