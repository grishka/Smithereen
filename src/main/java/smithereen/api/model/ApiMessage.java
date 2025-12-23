package smithereen.api.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import smithereen.api.ApiCallContext;
import smithereen.model.MailMessage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.Post;
import smithereen.model.attachments.Attachment;
import smithereen.model.photos.Photo;
import smithereen.util.XTEA;

public class ApiMessage{
	public @NotNull String id;
	public @NotNull String apId;
	public int from;
	public @NotNull List<Integer> to, cc;
	public long date;
	public @NotNull List<Integer> readBy;
	public @NotNull String subject;
	public @NotNull String body;
	public @Nullable List<ApiAttachment> attachments;
	public @Nullable ReplyInfo replyTo;

	public transient long rawID;

	public record ReplyInfo(String type, Integer wallPostId, Integer wallCommentId, String messageId){}

	public ApiMessage(MailMessage msg, ApiCallContext actx, Map<Long, Photo> photos, Map<Integer, Post> posts){
		id=msg.getIdString();
		rawID=msg.id;
		apId=msg.getActivityPubID().toString();
		from=msg.senderID;
		to=new ArrayList<>(msg.to);
		cc=new ArrayList<>(msg.cc);
		date=msg.createdAt.getEpochSecond();
		readBy=new ArrayList<>(msg.readReceipts);
		subject=msg.subject;
		body=msg.text;

		if(msg.attachments!=null && !msg.attachments.isEmpty()){
			List<Attachment> atts=msg.getProcessedAttachments();
			attachments=new ArrayList<>();
			for(Attachment att:atts){
				attachments.add(new ApiAttachment(att, actx, msg, photos));
			}
		}

		if(msg.replyInfo!=null){
			replyTo=switch(msg.replyInfo.type()){
				case MESSAGE -> new ReplyInfo("message", null, null, XTEA.encodeObjectID(msg.replyInfo.id(), ObfuscatedObjectIDType.MAIL_MESSAGE));
				case POST -> {
					int postID=(int) msg.replyInfo.id();
					Post p=posts.get(postID);
					if(p!=null && p.getReplyLevel()>0)
						yield new ReplyInfo("wall_comment", null, postID, null);
					yield new ReplyInfo("wall_post", postID, null, null);
				}
			};
		}
	}
}
