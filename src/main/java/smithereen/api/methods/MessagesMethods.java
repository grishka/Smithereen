package smithereen.api.methods;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.api.ApiCallContext;
import smithereen.api.model.ApiErrorType;
import smithereen.api.model.ApiMessage;
import smithereen.api.model.ApiPaginatedList;
import smithereen.api.model.ApiPaginatedListWithActors;
import smithereen.api.model.ApiUser;
import smithereen.model.MailMessage;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.attachments.Attachment;
import smithereen.model.attachments.PhotoAttachment;
import smithereen.model.photos.Photo;
import smithereen.util.XTEA;
import spark.utils.StringUtils;

public class MessagesMethods{
	public static Object get(ApplicationContext ctx, ApiCallContext actx){
		PaginatedList<MailMessage> messages;
		int offset=actx.getOffset();
		int count=actx.getCount(20, 200);
		if(actx.booleanParam("out"))
			messages=ctx.getMailController().getOutbox(actx.self.user, offset, count);
		else
			messages=ctx.getMailController().getInbox(actx.self.user, offset, count);

		ApiPaginatedListWithActors<ApiMessage> resp=new ApiPaginatedListWithActors<>(messages.total, getMessages(messages.list, ctx, actx));
		if(actx.booleanParam("extended")){
			HashSet<Integer> needUsers=new HashSet<>();
			for(MailMessage msg:messages.list){
				needUsers.add(msg.senderID);
				needUsers.addAll(msg.to);
				if(msg.cc!=null)
					needUsers.addAll(msg.cc);
			}
			needUsers.remove(actx.self.user.id);
			resp.profiles=ApiUtils.getUsers(needUsers, ctx, actx);
		}
		return resp;
	}

	public static Object getById(ApplicationContext ctx, ApiCallContext actx){
		List<Long> msgIDs=actx.requireCommaSeparatedStringList("message_ids")
				.stream()
				.limit(200)
				.map(id->XTEA.decodeObjectID(id, ObfuscatedObjectIDType.MAIL_MESSAGE))
				.distinct()
				.toList();
		Map<Long, MailMessage> rawMessagesMap=ctx.getMailController().getMessagesByIDs(actx.self.user, msgIDs, false);
		List<ApiMessage> messages=getMessages(msgIDs.stream().map(rawMessagesMap::get).filter(Objects::nonNull).toList(), ctx, actx);

		if(actx.booleanParam("extended")){
			HashSet<Integer> needUsers=new HashSet<>();
			for(MailMessage msg:rawMessagesMap.values()){
				needUsers.add(msg.senderID);
				needUsers.addAll(msg.to);
				if(msg.cc!=null)
					needUsers.addAll(msg.cc);
			}
			needUsers.remove(actx.self.user.id);

			record ExtendedResponse(List<ApiMessage> items, List<ApiUser> profiles){}
			return new ExtendedResponse(messages, ApiUtils.getUsers(needUsers, ctx, actx));
		}
		return messages;
	}

	public static Object getHistory(ApplicationContext ctx, ApiCallContext actx){
		User peer=ApiUtils.getUser(ctx, actx, "user_id");
		if(peer.id==actx.self.user.id)
			throw actx.paramError("user_id must not be the current user");
		PaginatedList<MailMessage> messages=ctx.getMailController().getHistory(actx.self.user, peer, actx.getOffset(), actx.getCount(20, 200));
		return new ApiPaginatedList<>(messages.total, getMessages(messages.list, ctx, actx));
	}

	private static List<ApiMessage> getMessages(List<MailMessage> messages, ApplicationContext ctx, ApiCallContext actx){
		Set<Long> needPhotos=new HashSet<>();
		Set<Integer> needPosts=new HashSet<>();
		for(MailMessage msg:messages){
			List<Attachment> attachments=msg.getProcessedAttachments();
			for(Attachment att:attachments){
				if(att instanceof PhotoAttachment pa && pa.photoID!=0){
					needPhotos.add(pa.photoID);
				}
			}
			if(msg.replyInfo instanceof MailMessage.ReplyInfo(MailMessage.ParentObjectType type, long id) && type==MailMessage.ParentObjectType.POST){
				needPosts.add((int)id);
			}
		}
		Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
		Map<Integer, Post> posts=ctx.getWallController().getPosts(needPosts);
		return messages.stream().map(m->new ApiMessage(m, actx, photos, posts)).toList();
	}

	public static Object send(ApplicationContext ctx, ApiCallContext actx){
		Set<Integer> toIDs;
		MailMessage inReplyTo=null;
		if(actx.hasParam("reply_to")){
			long replyToID=XTEA.decodeObjectID(actx.requireParamString("reply_to"), ObfuscatedObjectIDType.MAIL_MESSAGE);
			inReplyTo=ctx.getMailController().getMessage(actx.self.user, replyToID, false);
			toIDs=new HashSet<>();
			if(inReplyTo.senderID==actx.self.user.id){
				toIDs.addAll(inReplyTo.to);
			}else{
				toIDs.add(inReplyTo.senderID);
				toIDs.addAll(inReplyTo.to);
				toIDs.addAll(inReplyTo.cc);
			}
		}else{
			toIDs=actx.requireCommaSeparatedStringList("to")
					.stream()
					.map(Utils::safeParseInt)
					.filter(id->id>0)
					.collect(Collectors.toSet());
		}
		toIDs.remove(actx.self.user.id);
		Set<User> to=new HashSet<>(ctx.getUsersController().getUsers(toIDs).values());
		if(to.isEmpty())
			throw actx.paramError("to does not contain any valid user IDs");
		String subject=actx.optParamString("subject");
		String body=actx.optParamString("body", "").strip();
		ApiUtils.InputAttachments attachments=ApiUtils.parseAttachments(ctx, actx, false, false);
		if(StringUtils.isEmpty(body) && attachments.ids().isEmpty())
			throw actx.paramError("both body and attachments are empty");
		String guid=actx.optParamString("guid");
		if(StringUtils.isNotEmpty(guid)){
			guid=actx.token.getEncodedID()+"|"+guid;
		}

		long id=ctx.getMailController().sendMessage(actx.self.user, to, body, ApiUtils.getTextFormat(actx), subject, attachments.ids(), attachments.altTexts(), inReplyTo, guid);
		return XTEA.encodeObjectID(id, ObfuscatedObjectIDType.MAIL_MESSAGE);
	}

	public static Object delete(ApplicationContext ctx, ApiCallContext actx){
		long id=XTEA.decodeObjectID(actx.requireParamString("message_id"), ObfuscatedObjectIDType.MAIL_MESSAGE);
		boolean revoke=actx.booleanParam("revoke");
		MailMessage msg=ctx.getMailController().getMessage(actx.self.user, id, revoke);
		if(revoke)
			ctx.getMailController().actuallyDeleteMessage(actx.self.user, msg, true);
		else
			ctx.getMailController().deleteMessage(actx.self.user, msg);
		return true;
	}

	public static Object restore(ApplicationContext ctx, ApiCallContext actx){
		long id=XTEA.decodeObjectID(actx.requireParamString("message_id"), ObfuscatedObjectIDType.MAIL_MESSAGE);
		MailMessage msg=ctx.getMailController().getMessage(actx.self.user, id, true);
		ctx.getMailController().restoreMessage(actx.self.user, msg);
		return true;
	}

	public static Object markAsRead(ApplicationContext ctx, ApiCallContext actx){
		long id=XTEA.decodeObjectID(actx.requireParamString("message_id"), ObfuscatedObjectIDType.MAIL_MESSAGE);
		MailMessage msg=ctx.getMailController().getMessage(actx.self.user, id, true);
		if(msg.senderID==actx.self.user.id)
			throw actx.error(ApiErrorType.ACTION_NOT_APPLICABLE, "only incoming messages can be marked as read");
		ctx.getMailController().markMessageRead(actx.self.user, msg);
		return true;
	}
}
