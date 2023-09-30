package smithereen.model;

import com.google.gson.JsonParser;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.storage.DatabaseUtils;
import smithereen.storage.PostStorage;
import spark.utils.StringUtils;

public class Post implements ActivityPubRepresentable, OwnedContentObject, AttachmentHostContentObject{
	public int id;
	public int authorID;
	// userID or -groupID
	public int ownerID;
	public String text;
	public List<ActivityPubObject> attachments; // TODO move away from AP objects here
	public int repostOf;
	public Instant createdAt;
	public String contentWarning;
	public Instant updatedAt;
	public List<Integer> replyKey=List.of();
	public Set<Integer> mentionedUserIDs=Set.of();
	public int replyCount;
	public Poll poll;
	public FederationState federationState=FederationState.NONE;

	private URI activityPubID;
	public URI activityPubURL;
	public URI activityPubReplies;
	public boolean isReplyToUnknownPost;
	public boolean deleted;

	public boolean hasContentWarning(){
		return contentWarning!=null;
	}

	public String getContentWarning(){
		return contentWarning;
	}

	@Override
	public URI getActivityPubID(){
		if(activityPubID!=null)
			return activityPubID;
		return UriBuilder.local().path("posts", String.valueOf(id)).build();
	}

	public static Post fromResultSet(ResultSet res) throws SQLException{
		Post post=new Post();

		post.id=res.getInt("id");
		post.ownerID=res.getInt("owner_user_id");
		if(res.wasNull())
			post.ownerID=-res.getInt("owner_group_id");
		post.replyKey=Utils.deserializeIntList(res.getBytes("reply_key"));

		post.authorID=res.getInt("author_id");
		if(res.wasNull()){
			post.deleted=true;
			return post;
		}

		post.text=res.getString("text");

		String att=res.getString("attachments");
		if(att!=null){
			try{
				post.attachments=ActivityPubObject.parseSingleObjectOrArray(JsonParser.parseString(att), ParserContext.LOCAL);
			}catch(Exception ignore){}
		}

		post.repostOf=res.getInt("repost_of");
		String id=res.getString("ap_id");
		if(id!=null)
			post.setActivityPubID(URI.create(id));
		String url=res.getString("ap_url");
		if(url!=null)
			post.activityPubURL=URI.create(url);
		post.createdAt=DatabaseUtils.getInstant(res, "created_at");
		post.contentWarning=res.getString("content_warning");
		post.updatedAt=DatabaseUtils.getInstant(res, "updated_at");
		post.mentionedUserIDs=Utils.deserializeIntSet(res.getBytes("mentions"));
		post.replyCount=res.getInt("reply_count");
		String replies=res.getString("ap_replies");
		if(replies!=null)
			post.activityPubReplies=URI.create(replies);
		post.federationState=FederationState.values()[res.getInt("federation_state")];

		int pollID=res.getInt("poll_id");
		if(!res.wasNull()){
			post.poll=PostStorage.getPoll(pollID, post.activityPubID);
		}

		return post;
	}

	public int getReplyLevel(){
		return replyKey.size();
	}

	// Reply key for posts that reply to this one.
//	public int[] getReplyKeyForReplies(){
//		int[] r=new int[replyKey.length+1];
//		System.arraycopy(replyKey, 0, r, 0, replyKey.length);
//		r[r.length-1]=id;
//		return r;
//	}

	public boolean isDeleted(){
		return deleted;
	}

	// for use in templates
	public int getReplyChainElement(int level){
		return replyKey.get(level);
	}

	public String serializeAttachments(){
		if(attachments==null)
			return null;
		return ActivityPubObject.serializeObjectArrayCompact(attachments, new SerializerContext(null, (String)null)).toString();
	}

	public boolean canBeManagedBy(User user){
		if(user==null)
			return false;
		return ownerID==user.id || authorID==user.id;
	}

	public URI getInternalURL(){
		return Config.localURI("/posts/"+id);
	}

	public List<Integer> getReplyKeyForReplies(){
		ArrayList<Integer> rk=new ArrayList<>(replyKey);
		rk.add(id);
		return rk;
	}

	public boolean isGroupOwner(){
		return ownerID<0;
	}

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
			return Utils.truncateOnWordBoundary(text, maxLen);
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

	public void setActivityPubID(URI activityPubID){
		this.activityPubID=activityPubID;
	}

	@Override
	public List<ActivityPubObject> getAttachments(){
		return attachments;
	}

	@Override
	public NonCachedRemoteImage.Args getPhotoArgs(int index){
		return new NonCachedRemoteImage.PostPhotoArgs(id, index);
	}
}
