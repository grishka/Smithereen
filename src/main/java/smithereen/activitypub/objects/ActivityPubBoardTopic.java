package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.time.Instant;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.SerializerContext;
import smithereen.exceptions.FederationException;
import smithereen.model.Group;
import smithereen.model.User;
import smithereen.model.board.BoardTopic;
import smithereen.util.UriBuilder;
import spark.utils.StringUtils;

public class ActivityPubBoardTopic extends ActivityPubCollection{
	public Instant pinnedAt;
	public boolean isClosed;
	public URI firstCommentID;
	public URI authorID;

	public ActivityPubBoardTopic(){
		super(false);
	}

	@Override
	public String getType(){
		return "BoardTopic";
	}

	public BoardTopic asNativeTopic(ApplicationContext ctx){
		if(authorID==null)
			throw new FederationException("author is required");
		if(attributedTo==null)
			throw new FederationException("attributedTo is required");
		if(StringUtils.isEmpty(name))
			throw new FederationException("name is required");
		if(published==null)
			throw new FederationException("published is required");
		if(activityPubID==null)
			throw new FederationException("id is required");
		if(updated==null)
			throw new FederationException("updated is required");
		if(firstCommentID==null)
			throw new FederationException("firstComment is required");

		BoardTopic t=new BoardTopic();
		t.id=ctx.getBoardController().getTopicIDByActivityPubID(activityPubID);
		t.apID=activityPubID;
		t.apURL=url==null ? activityPubID : url;
		t.title=name;
		t.createdAt=published;
		t.updatedAt=updated;
		t.pinnedAt=pinnedAt;
		t.authorID=ctx.getObjectLinkResolver().resolve(authorID, User.class, true, true, false).id;
		t.groupID=ctx.getObjectLinkResolver().resolve(attributedTo, Group.class, true, true, false).id;
		// t.firstCommentID not set here because topic object needs to be stored first
		return t;
	}

	public static ActivityPubBoardTopic fromNativeTopic(BoardTopic topic, ApplicationContext ctx){
		LocalActivityPubBoardTopic t=new LocalActivityPubBoardTopic(topic);
		t.activityPubID=topic.getActivityPubID();
		t.url=topic.getActivityPubURL();
		t.name=topic.title;
		t.published=topic.createdAt;
		t.updated=topic.updatedAt;
		t.pinnedAt=topic.pinnedAt;
		t.authorID=ctx.getUsersController().getUserOrThrow(topic.authorID).activityPubID; // TODO handle deleted users?
		t.attributedTo=ctx.getGroupsController().getGroupOrThrow(topic.groupID).activityPubID;
		t.totalItems=topic.numComments;
		t.firstCommentID=ctx.getCommentsController().getCommentIgnoringPrivacy(topic.firstCommentID).getActivityPubID();
		t.first=new LinkOrObject(new UriBuilder(t.activityPubID).queryParam("page", "1").build());
		return t;
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		super.asActivityPubObject(obj, serializerContext);

		serializerContext.addSmAlias("BoardTopic");
		if(firstCommentID!=null){
			serializerContext.addSmAlias("pinnedAt");
			serializerContext.addSmAlias("isLocked");
			serializerContext.addSmIdType("author");
			serializerContext.addSmIdType("firstComment");
			if(pinnedAt!=null)
				obj.addProperty("pinnedAt", Utils.formatDateAsISO(pinnedAt));
			obj.addProperty("isLocked", isClosed);
			obj.addProperty("firstComment", firstCommentID.toString());
			obj.addProperty("author", authorID.toString());
		}

		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);

		pinnedAt=tryParseDate(optString(obj, "pinnedAt"));
		isClosed=optBoolean(obj, "isLocked");
		authorID=tryParseURL(optString(obj, "author"));
		firstCommentID=tryParseURL(optString(obj, "firstComment"));

		return this;
	}

	@Override
	public void validate(@Nullable URI parentID, String propertyName){
		super.validate(parentID, propertyName);
		ensureHostMatchesID(attributedTo, "attributedTo");
	}
}
