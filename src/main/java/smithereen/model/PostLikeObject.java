package smithereen.model;

import com.google.gson.JsonParser;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.model.comments.Comment;
import smithereen.storage.DatabaseUtils;
import smithereen.text.TextProcessor;
import spark.utils.StringUtils;

public abstract sealed class PostLikeObject implements OwnedContentObject, AttachmentHostContentObject permits Post, Comment{
	public int authorID;
	// userID or -groupID
	public int ownerID;
	public String text;
	public List<ActivityPubObject> attachments; // TODO move away from AP objects here
	public Instant createdAt;
	public String contentWarning;
	public Instant updatedAt;
	public Set<Integer> mentionedUserIDs=Set.of();
	public int replyCount;
	public FederationState federationState=FederationState.NONE;
	public URI activityPubURL;
	public URI activityPubReplies;
	public boolean deleted;
	protected URI activityPubID;

	protected void fillFromResultSet(ResultSet res) throws SQLException{
		ownerID=res.getInt("owner_user_id");
		if(res.wasNull())
			ownerID=-res.getInt("owner_group_id");
		replyCount=res.getInt("reply_count");

		authorID=res.getInt("author_id");
		if(res.wasNull()){
			deleted=true;
			return;
		}

		text=res.getString("text");

		String att=res.getString("attachments");
		if(att!=null){
			try{
				attachments=ActivityPubObject.parseSingleObjectOrArray(JsonParser.parseString(att), ParserContext.LOCAL);
			}catch(Exception ignore){}
		}

		String id=res.getString("ap_id");
		if(id!=null)
			setActivityPubID(URI.create(id));
		String url=res.getString("ap_url");
		if(url!=null)
			activityPubURL=URI.create(url);
		createdAt=DatabaseUtils.getInstant(res, "created_at");
		contentWarning=res.getString("content_warning");
		updatedAt=DatabaseUtils.getInstant(res, "updated_at");
		mentionedUserIDs=Utils.deserializeIntSet(res.getBytes("mentions"));
		String replies=res.getString("ap_replies");
		if(replies!=null)
			activityPubReplies=URI.create(replies);
		federationState=FederationState.values()[res.getInt("federation_state")];
	}

	public boolean hasContentWarning(){
		return contentWarning!=null;
	}

	public String getContentWarning(){
		return contentWarning;
	}

	public abstract int getReplyLevel();

	public boolean isDeleted(){
		return deleted;
	}

	public String serializeAttachments(){
		if(attachments==null)
			return null;
		return ActivityPubObject.serializeObjectArrayCompact(attachments, new SerializerContext(null, (String)null)).toString();
	}

	public abstract URI getInternalURL();

	public boolean isLocal(){
		return activityPubID==null;
	}

	public String getShortTitle(){
		return getShortTitle(100);
	}

	public String getShortTitle(int maxLen){
		if(StringUtils.isNotEmpty(contentWarning)){
			return contentWarning;
		}
		if(StringUtils.isNotEmpty(text)){
			return TextProcessor.truncateOnWordBoundary(text, maxLen);
		}
		return "";
	}

	@Override
	public int getOwnerID(){
		return ownerID;
	}

	@Override
	public int getAuthorID(){
		return authorID;
	}

	@Override
	public abstract long getObjectID();

	@Override
	public List<ActivityPubObject> getAttachments(){
		return attachments;
	}

	public void setActivityPubID(URI activityPubID){
		this.activityPubID=activityPubID;
	}

	@Override
	public abstract NonCachedRemoteImage.Args getPhotoArgs(int index);

	@Override
	public abstract String getPhotoListID();
}
