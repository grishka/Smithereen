package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.net.URI;
import java.time.Instant;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.model.board.BoardTopic;

public class ActivityPubBoardTopic extends ActivityPubCollection{
	public Instant pinnedAt;
	public boolean isClosed;
	public URI groupID;

	public ActivityPubBoardTopic(){
		super(false);
	}

	@Override
	public String getType(){
		return "BoardTopic";
	}

	public BoardTopic asNativeTopic(){
		return null;
	}

	public static ActivityPubBoardTopic fromNativeTopic(BoardTopic topic, ApplicationContext ctx){
		LocalActivityPubBoardTopic t=new LocalActivityPubBoardTopic(topic);
		t.activityPubID=topic.getActivityPubID();
		t.url=topic.getActivityPubURL();
		t.name=topic.title;
		t.published=topic.createdAt;
		t.updated=topic.updatedAt;
		t.pinnedAt=topic.pinnedAt;
		t.attributedTo=ctx.getUsersController().getUserOrThrow(topic.authorID).activityPubID; // TODO handle deleted users?
		t.groupID=ctx.getGroupsController().getGroupOrThrow(topic.groupID).activityPubID;
		t.totalItems=topic.numComments;
		return t;
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		super.asActivityPubObject(obj, serializerContext);

		serializerContext.addSmAlias("pinnedAt");
		serializerContext.addSmAlias("isLocked");
		serializerContext.addSmIdType("group");

		if(pinnedAt!=null)
			obj.addProperty("pinnedAt", Utils.formatDateAsISO(pinnedAt));
		obj.addProperty("isLocked", isClosed);
		obj.addProperty("group", groupID.toString());

		return obj;
	}
}
