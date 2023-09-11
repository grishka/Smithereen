package smithereen.data;

import com.google.gson.JsonParser;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.storage.DatabaseUtils;
import smithereen.util.XTEA;
import spark.utils.StringUtils;

public class MailMessage implements AttachmentHostContentObject, ActivityPubRepresentable{
	public long id;
	public int senderID;
	public int ownerID;
	public Set<Integer> to;
	public Set<Integer> cc;
	public String text;
	public String subject;
	public List<ActivityPubObject> attachments;
	public Instant createdAt;
	public Instant updatedAt;
	public Set<Integer> readReceipts;
	public Set<Long> relatedMessageIDs;
	public URI activityPubID;
	public ReplyInfo replyInfo;

	public transient String encodedID;
	public transient URI inReplyTo;

	public static MailMessage fromResultSet(ResultSet res) throws SQLException{
		MailMessage msg=new MailMessage();
		msg.id=XTEA.obfuscateObjectID(res.getLong("id"), ObfuscatedObjectIDType.MAIL_MESSAGE);
		msg.senderID=res.getInt("sender_id");
		msg.ownerID=res.getInt("owner_id");
		msg.to=Utils.deserializeIntSet(res.getBytes("to"));
		msg.cc=Utils.deserializeIntSet(res.getBytes("cc"));
		msg.text=res.getString("text");
		msg.subject=res.getString("subject");

		String att=res.getString("attachments");
		if(att!=null){
			try{
				msg.attachments=ActivityPubObject.parseSingleObjectOrArray(JsonParser.parseString(att), ParserContext.LOCAL);
			}catch(Exception ignore){}
		}

		msg.createdAt=DatabaseUtils.getInstant(res, "created_at");
		msg.updatedAt=DatabaseUtils.getInstant(res, "updated_at");
		msg.readReceipts=Utils.deserializeIntSet(res.getBytes("read_receipts"));
		HashSet<Long> relatedIDs=new HashSet<>();
		Utils.deserializeLongCollection(res.getBytes("related_message_ids"), relatedIDs);
		msg.relatedMessageIDs=relatedIDs.stream().map(id->XTEA.obfuscateObjectID(id, ObfuscatedObjectIDType.MAIL_MESSAGE)).collect(Collectors.toSet());
		String apID=res.getString("ap_id");
		if(apID!=null)
			msg.activityPubID=URI.create(apID);
		String replyInfo=res.getString("reply_info");
		if(StringUtils.isNotEmpty(replyInfo))
			msg.replyInfo=Utils.gson.fromJson(replyInfo, ReplyInfo.class);

		msg.encodedID=Utils.encodeLong(msg.id);
		return msg;
	}

	public String getTextPreview(){
		return Utils.truncateOnWordBoundary(text, 100);
	}

	public boolean isUnread(){
		// Outgoing message is "unread" if no one has seen it
		if(ownerID==senderID)
			return readReceipts.isEmpty();
		// Incoming message is "unread" if its owner hasn't seen it
		return !readReceipts.contains(ownerID);
	}

	@Override
	public List<ActivityPubObject> getAttachments(){
		return attachments;
	}

	@Override
	public NonCachedRemoteImage.Args getPhotoArgs(int index){
		return new NonCachedRemoteImage.MessagePhotoArgs(id, index);
	}

	public int getFirstRecipientID(){
		return to.iterator().next();
	}

	@Override
	public URI getActivityPubID(){
		if(activityPubID!=null)
			return activityPubID;
		return Config.localURI("/activitypub/objects/messages/"+encodedID);
	}

	public int getTotalRecipientCount(){
		int c=to.size();
		if(cc!=null)
			c+=cc.size();
		return c;
	}

	public enum ParentObjectType{
		MESSAGE,
		POST;
	}

	public record ReplyInfo(ParentObjectType type, long id){}
}
