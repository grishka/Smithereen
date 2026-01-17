package smithereen.model;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.model.reports.ReportableContentObject;
import smithereen.model.reports.ReportableContentObjectID;
import smithereen.model.reports.ReportableContentObjectType;
import smithereen.model.reports.ReportedMailMessage;
import smithereen.storage.DatabaseUtils;
import smithereen.text.TextProcessor;
import smithereen.util.JsonArrayBuilder;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.XTEA;
import spark.utils.StringUtils;

public sealed class MailMessage implements AttachmentHostContentObject, ActivityPubRepresentable, ReportableContentObject permits ReportedMailMessage{
	public long id;
	public int senderID;
	public int ownerID;
	public @NotNull Set<Integer> to=Set.of();
	public @NotNull Set<Integer> cc=Set.of();
	public @NotNull String text="";
	public @NotNull String subject="";
	public @Nullable List<ActivityPubObject> attachments;
	public @NotNull Instant createdAt=Instant.EPOCH;
	public @Nullable Instant updatedAt;
	public @NotNull Set<Integer> readReceipts=Set.of();
	public @NotNull Set<Long> relatedMessageIDs=Set.of();
	public @Nullable URI activityPubID;
	public @Nullable ReplyInfo replyInfo;

	public transient URI inReplyTo;

	public static MailMessage fromResultSet(ResultSet res) throws SQLException{
		MailMessage msg=new MailMessage();
		msg.id=res.getLong("id");
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

		msg.createdAt=Objects.requireNonNull(DatabaseUtils.getInstant(res, "created_at"));
		msg.updatedAt=DatabaseUtils.getInstant(res, "updated_at");
		msg.readReceipts=Utils.deserializeIntSet(res.getBytes("read_receipts"));
		HashSet<Long> relatedIDs=new HashSet<>();
		Utils.deserializeLongCollection(res.getBytes("related_message_ids"), relatedIDs);
		msg.relatedMessageIDs=relatedIDs;
		String apID=res.getString("ap_id");
		if(apID!=null)
			msg.activityPubID=URI.create(apID);
		String replyInfo=res.getString("reply_info");
		if(StringUtils.isNotEmpty(replyInfo))
			msg.replyInfo=Utils.gson.fromJson(replyInfo, ReplyInfo.class);

		return msg;
	}

	public String getTextPreview(){
		return TextProcessor.truncateOnWordBoundary(text, 100);
	}

	public boolean isUnread(){
		// Outgoing message is "unread" if no one has seen it
		if(ownerID==senderID)
			return readReceipts.isEmpty();
		// Incoming message is "unread" if its owner hasn't seen it
		return !readReceipts.contains(ownerID);
	}

	@Override
	@NotNull
	public List<ActivityPubObject> getAttachments(){
		return attachments==null ? List.of() : attachments;
	}

	@Override
	public NonCachedRemoteImage.Args getPhotoArgs(int index){
		return new NonCachedRemoteImage.MessagePhotoArgs(id, index);
	}

	@Override
	public String getPhotoListID(){
		return "messages/"+getIdString();
	}

	public int getFirstRecipientID(){
		return to.iterator().next();
	}

	@Override
	public @NotNull URI getActivityPubID(){
		if(activityPubID!=null)
			return activityPubID;
		return Config.localURI("/activitypub/objects/messages/"+getIdString());
	}

	public int getTotalRecipientCount(){
		return to.size()+cc.size();
	}

	@Override
	public JsonObject serializeForReport(int targetID, Set<Long> outFileIDs){
		if(ownerID!=targetID && senderID!=targetID)
			return null;
		JsonObjectBuilder jb=new JsonObjectBuilder()
				.add("type", "message")
				.add("id", id)
				.add("sender", senderID)
				.add("to", to.stream().collect(JsonArrayBuilder.COLLECTOR))
				.add("created_at", createdAt.getEpochSecond())
				.add("text", text);
		if(replyInfo!=null)
			jb.add("replyInfo", Utils.gson.toJsonTree(replyInfo));
		if(attachments!=null && !attachments.isEmpty())
			jb.add("attachments", ReportableContentObject.serializeMediaAttachments(attachments, outFileIDs));
		return jb.build();
	}

	@Override
	public void fillFromReport(int reportID, JsonObject jo){
		id=jo.get("id").getAsLong();
		senderID=jo.get("sender").getAsInt();
		to=StreamSupport.stream(jo.getAsJsonArray("to").spliterator(), false).map(JsonElement::getAsInt).collect(Collectors.toSet());
		createdAt=Instant.ofEpochSecond(jo.get("created_at").getAsLong());
		text=jo.get("text").getAsString();
		if(jo.has("replyInfo")){
			replyInfo=Utils.gson.fromJson(jo.get("replyInfo"), ReplyInfo.class);
		}
		if(jo.has("attachments")){
			attachments=new ArrayList<>();
			for(JsonElement jatt:jo.getAsJsonArray("attachments")){
				attachments.add(ActivityPubObject.parse(jatt.getAsJsonObject(), ParserContext.LOCAL));
			}
		}
	}

	@Override
	public ReportableContentObjectID getReportableObjectID(){
		return new ReportableContentObjectID(ReportableContentObjectType.MESSAGE, id);
	}

	@Override
	public int getOwnerID(){
		return ownerID;
	}

	@Override
	public int getAuthorID(){
		return senderID;
	}

	@Override
	public long getObjectID(){
		return id;
	}

	public @NotNull String getIdString(){
		return XTEA.encodeObjectID(id, ObfuscatedObjectIDType.MAIL_MESSAGE);
	}

	public enum ParentObjectType{
		MESSAGE,
		POST;
	}

	public record ReplyInfo(ParentObjectType type, long id){}
}
