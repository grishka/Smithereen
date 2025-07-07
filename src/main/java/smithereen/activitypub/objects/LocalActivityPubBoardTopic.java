package smithereen.activitypub.objects;

import smithereen.model.board.BoardTopic;

public class LocalActivityPubBoardTopic extends ActivityPubBoardTopic{
	public final BoardTopic topic;

	public LocalActivityPubBoardTopic(BoardTopic topic){
		this.topic=topic;
	}

	@Override
	public BoardTopic asNativeTopic(){
		return topic;
	}
}
