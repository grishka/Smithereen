package smithereen.model.comments;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Set;

import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.activities.Like;
import smithereen.model.ActivityPubRepresentable;
import smithereen.model.LikeableContentObject;
import smithereen.model.NonCachedRemoteImage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PostLikeObject;
import smithereen.model.notifications.Notification;
import smithereen.model.reports.ReportableContentObject;
import smithereen.model.reports.ReportedComment;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.UriBuilder;
import smithereen.util.XTEA;

public sealed class Comment extends PostLikeObject implements ActivityPubRepresentable, CommentReplyParent, LikeableContentObject, ReportableContentObject permits ReportedComment{
	public long id;
	public CommentParentObjectID parentObjectID;
	public List<Long> replyKey=List.of();

	public static Comment fromResultSet(ResultSet res) throws SQLException{
		Comment c=new Comment();
		c.fillFromResultSet(res);
		return c;
	}

	@Override
	protected void fillFromResultSet(ResultSet res) throws SQLException{
		id=res.getLong("id");
		parentObjectID=new CommentParentObjectID(CommentableObjectType.values()[res.getInt("parent_object_type")], res.getLong("parent_object_id"));
		byte[] rk=res.getBytes("reply_key");
		if(rk!=null){
			replyKey=new ArrayList<>(rk.length/8);
			Utils.deserializeLongCollection(rk, replyKey);
		}

		super.fillFromResultSet(res);
	}

	@Override
	public int getReplyLevel(){
		return replyKey.size();
	}

	@Override
	public URI getInternalURL(){
		return UriBuilder.local().path("comments", getIDString()).build();
	}

	@Override
	public long getObjectID(){
		return id;
	}

	@Override
	public NonCachedRemoteImage.Args getPhotoArgs(int index){
		return null;
	}

	@Override
	public String getPhotoListID(){
		return "comments/"+getIDString();
	}

	public String getReplyParentID(){
		if(replyKey==null || replyKey.isEmpty())
			return null;
		return XTEA.encodeObjectID(replyKey.getLast(), ObfuscatedObjectIDType.COMMENT);
	}

	public String getIDString(){
		return XTEA.encodeObjectID(id, ObfuscatedObjectIDType.COMMENT);
	}

	public List<Long> getReplyKeyForReplies(){
		ArrayList<Long> rk=new ArrayList<>(replyKey);
		rk.add(id);
		return rk;
	}

	public URI getActivityPubID(){
		return activityPubID!=null ? activityPubID : getInternalURL();
	}

	public URI getActivityPubURL(){
		return activityPubURL!=null ? activityPubURL : getInternalURL();
	}

	@Override
	public Like.ObjectType getLikeObjectType(){
		return Like.ObjectType.COMMENT;
	}

	@Override
	public Notification.ObjectType getObjectTypeForLikeNotifications(){
		return Notification.ObjectType.COMMENT;
	}

	@Override
	public JsonObject serializeForReport(int targetID, Set<Long> outFileIDs){
		if(authorID!=targetID && ownerID!=targetID)
			return null;
		JsonObjectBuilder jb=new JsonObjectBuilder()
				.add("type", "comment")
				.add("id", id)
				.add("owner", ownerID)
				.add("author", authorID)
				.add("created_at", createdAt.getEpochSecond())
				.add("text", text)
				.add("parent_type", parentObjectID.type().toString())
				.add("parent_id", parentObjectID.id());
		if(getReplyLevel()>0)
			jb.add("replyKey", Base64.getEncoder().withoutPadding().encodeToString(Utils.serializeLongCollection(replyKey)));
		if(attachments!=null && !attachments.isEmpty())
			jb.add("attachments", ReportableContentObject.serializeMediaAttachments(attachments, outFileIDs));
		if(hasContentWarning())
			jb.add("cw", contentWarning);
		return jb.build();
	}

	@Override
	public void fillFromReport(int reportID, JsonObject jo){
		id=jo.get("id").getAsInt();
		ownerID=jo.get("owner").getAsInt();
		authorID=jo.get("author").getAsInt();
		createdAt=Instant.ofEpochSecond(jo.get("created_at").getAsLong());
		parentObjectID=new CommentParentObjectID(CommentableObjectType.valueOf(jo.get("parent_type").getAsString()), jo.get("parent_id").getAsLong());
		text=jo.get("text").getAsString();
		if(jo.has("replyKey")){
			replyKey=new ArrayList<>();
			Utils.deserializeLongCollection(Base64.getDecoder().decode(jo.get("replyKey").getAsString()), replyKey);
		}
		if(jo.has("attachments")){
			attachments=new ArrayList<>();
			for(JsonElement jatt:jo.getAsJsonArray("attachments")){
				attachments.add(ActivityPubObject.parse(jatt.getAsJsonObject(), ParserContext.LOCAL));
			}
		}
		if(jo.has("cw"))
			contentWarning=jo.get("cw").getAsString();
	}

	public List<String> getReplyKeyAsStrings(){
		return replyKey.stream().map(id->XTEA.encodeObjectID(id, ObfuscatedObjectIDType.COMMENT)).toList();
	}
}
