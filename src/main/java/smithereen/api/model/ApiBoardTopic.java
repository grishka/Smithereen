package smithereen.api.model;

import smithereen.model.board.BoardTopic;

public class ApiBoardTopic{
	public String id;
	public int groupId;
	public String apId;
	public String url;
	public String title;
	public long created;
	public int createdBy;
	public long updated;
	public int updatedBy;
	public boolean isClosed;
	public boolean isPinned;
	public int comments;
	public String commentPreview;

	public transient long rawID;

	public ApiBoardTopic(BoardTopic topic){
		id=topic.getIdString();
		groupId=topic.groupID;
		apId=topic.getActivityPubID().toString();
		url=topic.getActivityPubURL().toString();
		title=topic.title;
		created=topic.createdAt.getEpochSecond();
		createdBy=topic.authorID;
		updated=topic.updatedAt.getEpochSecond();
		updatedBy=topic.lastCommentAuthorID;
		isClosed=topic.isClosed;
		isPinned=topic.isPinned;
		comments=topic.numComments;
		rawID=topic.id;
	}
}
