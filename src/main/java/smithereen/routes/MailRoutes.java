package smithereen.routes;

import com.google.gson.reflect.TypeToken;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.controllers.FriendsController;
import smithereen.model.Account;
import smithereen.model.MailMessage;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.UserPrivacySettingKey;
import smithereen.model.WebDeltaResponse;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.lang.Lang;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
import smithereen.text.TextProcessor;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class MailRoutes{
	public static Object inbox(Request req, Response resp, Account self, ApplicationContext ctx){
		PaginatedList<MailMessage> list=ctx.getMailController().getInbox(self.user, offset(req), 20);
		RenderedTemplateResponse model=new RenderedTemplateResponse("mail_message_list", req);
		Set<Integer> needUsers=list.list.stream().map(m->m.senderID).collect(Collectors.toSet());
		if(req.queryParams("fromCompose")!=null){
			MailMessage msg=req.session().attribute("recentMessage");
			if(msg!=null){
				req.session().removeAttribute("recentMessage");
				model.with("recentlySentMessage", msg);
				needUsers=new HashSet<>(needUsers);
				needUsers.addAll(msg.to);
			}
		}
		model.with("tab", "inbox").pageTitle(lang(req).get("mail_inbox_title")).paginate(list).with("users", ctx.getUsersController().getUsers(needUsers));
		model.with("toolbarTitle", lang(req).get("messages_title"));
		return model;
	}

	public static Object outbox(Request req, Response resp, Account self, ApplicationContext ctx){
		PaginatedList<MailMessage> list=ctx.getMailController().getOutbox(self.user, offset(req), 20);
		Set<Integer> needUsers=list.list.stream().flatMap(m->m.to.stream()).collect(Collectors.toSet());
		RenderedTemplateResponse model=new RenderedTemplateResponse("mail_message_list", req);
		model.with("tab", "outbox").pageTitle(lang(req).get("mail_outbox_title")).paginate(list).with("users", ctx.getUsersController().getUsers(needUsers));
		model.with("toolbarTitle", lang(req).get("messages_title"));
		return model;
	}

	public static Object viewMessage(Request req, Response resp, Account self, ApplicationContext ctx){
		long id=req.attribute("id");
		MailMessage msg=ctx.getMailController().getMessage(self.user, id, false);
		if(msg.isUnread() && msg.senderID!=self.user.id){
			ctx.getMailController().markMessageRead(self.user, msg);
		}
		HashSet<Integer> needUsers=new HashSet<>();
		needUsers.add(msg.senderID);
		needUsers.addAll(msg.to);
		needUsers.addAll(msg.cc);
		Map<Integer, User> users=ctx.getUsersController().getUsers(needUsers);
		RenderedTemplateResponse model=new RenderedTemplateResponse("mail_message", req);
		model.with("tab", "view").with("message", msg).with("users", users);
		boolean isOutgoing=msg.senderID==self.user.id;
		User peer=users.get(isOutgoing ? msg.to.iterator().next() : msg.senderID);
		model.with("peer", peer);
		model.pageTitle(lang(req).get(isOutgoing ? "mail_message_title_outgoing" : "mail_message_title_incoming", Map.of("name", peer==null ? "DELETED" : peer.getFirstLastAndGender())));
		if(StringUtils.isNotEmpty(msg.subject)){
			String subject=msg.subject;
			if(!subject.toLowerCase().startsWith("re:"))
				subject="Re: "+subject;
			model.with("replySubject", subject);
		}
		model.with("toolbarTitle", lang(req).get("messages_title"));
		Templates.addJsLangForNewPostForm(req);
		if(msg.replyInfo!=null && msg.replyInfo.type()!=MailMessage.ParentObjectType.MESSAGE){
			switch(msg.replyInfo.type()){
				case POST -> {
					try{
						Post post=ctx.getWallController().getPostOrThrow((int)msg.replyInfo.id());
						String langKey;
						if(post.authorID==self.user.id){
							langKey=post.getReplyLevel()>0 ? "mail_in_reply_to_own_comment" : "mail_in_reply_to_own_post";
						}else{
							langKey=post.getReplyLevel()>0 ? "mail_in_reply_to_comment" : "mail_in_reply_to_post";
						}
						User author=null;
						try{
							author=ctx.getUsersController().getUserOrThrow(post.authorID);
						}catch(ObjectNotFoundException ignore){}
						model.with("inReplyToLink", lang(req).get(langKey, Map.of("name", author==null ? "DELETED" : author.getFirstLastAndGender()))).with("inReplyToURL", post.getInternalURL());
					}catch(ObjectNotFoundException ignore){}
				}
			}
		}

		Set<Integer> toUserIDs=new HashSet<>();
		if(msg.senderID==self.user.id){
			toUserIDs.addAll(msg.to);
		}else{
			toUserIDs.add(msg.senderID);
			toUserIDs.addAll(msg.to);
			toUserIDs.addAll(msg.cc);
			toUserIDs.remove(self.user.id);
		}
		boolean canReply=!toUserIDs.stream()
				.map(users::get)
				.filter(u->u!=null && ctx.getPrivacyController().checkUserPrivacy(self.user, u, UserPrivacySettingKey.PRIVATE_MESSAGES))
				.collect(Collectors.toSet())
				.isEmpty();
		model.with("canReply", canReply);
		return model;
	}

	public static Object sendMessage(Request req, Response resp, Account self, ApplicationContext ctx){
		String _inReplyTo=req.queryParams("inReplyTo");
		MailMessage inReplyTo=null;
		Set<Integer> toUserIDs;
		String subject=req.queryParams("subject");
		if(StringUtils.isNotEmpty(_inReplyTo)){
			long replyToID=decodeLong(_inReplyTo);
			inReplyTo=ctx.getMailController().getMessage(self.user, replyToID, false);
			toUserIDs=new HashSet<>();
			if(inReplyTo.senderID==self.user.id){
				toUserIDs.addAll(inReplyTo.to);
			}else{
				toUserIDs.add(inReplyTo.senderID);
				toUserIDs.addAll(inReplyTo.to);
				toUserIDs.addAll(inReplyTo.cc);
				toUserIDs.remove(self.user.id);
			}
		}else{
			requireQueryParams(req, "to");
			toUserIDs=Arrays.stream(req.queryParams("to").split(","))
					.map(Utils::safeParseInt)
					.filter(i->i>0 && i!=self.user.id)
					.collect(Collectors.toSet());
		}
		Set<User> toUsers=ctx.getUsersController().getUsers(toUserIDs).values().stream().filter(Objects::nonNull).collect(Collectors.toSet());
		if(toUsers.isEmpty())
			throw new BadRequestException();
		List<String> attachments;
		Map<String, String> attachmentAltTexts;
		if(StringUtils.isNotEmpty(req.queryParams("attachments"))){
			attachments=Arrays.stream(req.queryParams("attachments").split(",")).collect(Collectors.toList());
			String altTextsJson=req.queryParams("attachAltTexts");
			if(StringUtils.isNotEmpty(altTextsJson)){
				try{
					attachmentAltTexts=gson.fromJson(altTextsJson, new TypeToken<>(){});
				}catch(Exception x){
					attachmentAltTexts=Map.of();
				}
			}else{
				attachmentAltTexts=Map.of();
			}
		}else{
			attachments=Collections.emptyList();
			attachmentAltTexts=Map.of();
		}
		long id=ctx.getMailController().sendMessage(self.user, self.id, toUsers, req.queryParamOrDefault("text", ""), subject, attachments, attachmentAltTexts, inReplyTo);
		MailMessage msg=ctx.getMailController().getMessage(self.user, id, false);
		req.session().attribute("recentMessage", msg);
		if(isAjax(req)){
			String from=req.queryParams("from");
			User toUser=toUsers.iterator().next();
			if("ajaxBox".equals(from)){
				return new WebDeltaResponse(resp).showSnackbar(lang(req).get("mail_message_sent", Map.of("name", toUser.getFirstLastAndGender())));
			}
			return new WebDeltaResponse(resp).replaceLocation("/my/mail?fromCompose");
		}
		resp.redirect("/my/mail?fromCompose");
		return "";
	}

	public static Object compose(Request req, Response resp, Account self, ApplicationContext ctx){
		boolean ajax=isAjax(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse(ajax ? "mail_compose_ajax_box" : "mail_compose", req);
		model.pageTitle(lang(req).get("mail_tab_compose"));
		model.with("toolbarTitle", lang(req).get("messages_title"));
		int to=safeParseInt(req.queryParams("to"));
		if(to==0){
			model.with("friendList", ctx.getFriendsController()
					.getFriends(self.user, 0, 1000, FriendsController.SortOrder.ID_ASCENDING)
					.list.stream()
					.map(u->Map.of("id", String.valueOf(u.id), "title", u.getFullName()))
					.toList()
			).pageTitle(lang(req).get("mail_tab_compose"));
			String replyToID=req.queryParams("replyTo");
			if(StringUtils.isNotEmpty(replyToID)){
				try{
					MailMessage replyTo=ctx.getMailController().getMessage(self.user, decodeLong(replyToID), false);
					model.with("replyTo", replyTo);
					HashSet<Integer> needUsers=new HashSet<>(replyTo.to);
					needUsers.addAll(replyTo.cc);
					needUsers.add(replyTo.senderID);
					if(StringUtils.isNotEmpty(replyTo.subject)){
						String subject=replyTo.subject;
						if(!subject.toLowerCase().startsWith("re:"))
							subject="Re: "+subject;
						model.with("subject", subject);
					}
					Map<Integer, User> users=ctx.getUsersController().getUsers(needUsers);
					HashSet<Integer> to_=new HashSet<>(needUsers);
					to_.remove(self.user.id);
					model.with("to", to_).with("users", users);
				}catch(ObjectNotFoundException ignore){
				}
			}
		}else{
			model.with("to", Set.of(to));
			model.with("users", ctx.getUsersController().getUsers(Set.of(to)));
		}
		Templates.addJsLangForNewPostForm(req);
		return model;
	}

	public static Object delete(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isAjax(req))
			return "";
		long id=req.attribute("id");
		MailMessage msg=ctx.getMailController().getMessage(self.user, id, true);
		ctx.getMailController().deleteMessage(self.user, msg);
		String csrf=sessionInfo(req).csrfToken;
		Lang l=lang(req);
		boolean fromView=req.queryParams("fromView")!=null;
		String rowClasses=fromView ? "" : "mailMessageRow deleted";
		String restoreHtml="<div class=\""+rowClasses+"\" id=\"msgDeletedRow"+msg.encodedID+"\"><div class=\"restore\">";
		restoreHtml+="<div>"+l.get("mail_message_deleted")+"</div><div id=\"msgRestore"+msg.encodedID+"\">";
		String restoreHref="/my/mail/messages/"+msg.encodedID+"/restore?csrf="+csrf;
		String deleteForEveryoneHref="/my/mail/messages/"+msg.encodedID+"/deleteForEveryone?csrf="+csrf;
		String origElementID;
		if(fromView){
			restoreHref+="&fromView";
			origElementID="messageViewInner";
		}else{
			origElementID="msgRow"+msg.encodedID;
		}
		if(msg.isUnread()){
			restoreHtml+=TextProcessor.substituteLinks(l.get(msg.to.size()>1 ? "restore_or_delete_for_peer_multi" : "restore_or_delete_for_peer",
					Map.of("name", ctx.getUsersController().getUserOrThrow(msg.getFirstRecipientID()).getFirstAndGender())),
					Map.of(
							"restore", Map.of("href", restoreHref, "data-ajax", "", "data-ajax-hide", "msgRestore"+msg.encodedID, "data-ajax-show", "msgRestoreLoader"+msg.encodedID),
							"deleteForPeer", Map.of("href", deleteForEveryoneHref, "data-ajax", "", "data-ajax-hide", "msgRestore"+msg.encodedID, "data-ajax-show", "msgRestoreLoader"+msg.encodedID)
					)
			);
		}else{
			restoreHtml+="<a href=\""+restoreHref+"\" data-ajax data-ajax-hide=\"msgRestore"+msg.encodedID+"\" data-ajax-show=\"msgRestoreLoader"+msg.encodedID+"\">"+l.get("restore_message")+"</a>";
		}
		restoreHtml+="</div><div class=\"inlineLoader\" style=\"display: none\" id=\"msgRestoreLoader"+msg.encodedID+"\"></div></div></div>";
		return new WebDeltaResponse(resp)
				.hide(origElementID)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.AFTER_END, origElementID, restoreHtml);
	}

	public static Object deleteForEveryone(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isAjax(req))
			return "";
		long id=req.attribute("id");
		MailMessage msg=ctx.getMailController().getMessage(self.user, id, true);
		ctx.getMailController().actuallyDeleteMessage(self.user, msg, true);
		return new WebDeltaResponse(resp).remove("msgRestore"+msg.encodedID);
	}

	public static Object restore(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isAjax(req))
			return "";
		long id=req.attribute("id");
		MailMessage msg=ctx.getMailController().getMessage(self.user, id, true);
		ctx.getMailController().restoreMessage(self.user, msg);
		boolean fromView=req.queryParams("fromView")!=null;
		String origElementID;
		if(fromView){
			origElementID="messageViewInner";
		}else{
			origElementID="msgRow"+msg.encodedID;
		}
		return new WebDeltaResponse(resp)
				.remove("msgDeletedRow"+msg.encodedID)
				.show(origElementID);
	}

	public static Object history(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isAjax(req)){
			resp.redirect("/my/mail");
			return "";
		}
		requireQueryParams(req, "peer");
		int peerID=safeParseInt(req.queryParams("peer"));
		User peer=ctx.getUsersController().getUserOrThrow(peerID);
		RenderedTemplateResponse model=new RenderedTemplateResponse("mail_history", req);
		model.paginate(ctx.getMailController().getHistory(self.user, peer, offset(req), 50));
		model.with("users", Map.of(self.user.id, self.user, peerID, peer));
		return new WebDeltaResponse(resp).setContent("mailHistoryWrap", model.renderToString());
	}
}
