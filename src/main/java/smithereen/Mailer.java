package smithereen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.friends.FriendRequest;
import smithereen.model.Group;
import smithereen.model.MailMessage;
import smithereen.model.OwnedContentObject;
import smithereen.model.Post;
import smithereen.model.PostLikeObject;
import smithereen.model.User;
import smithereen.model.UserBanInfo;
import smithereen.model.UserBanStatus;
import smithereen.model.board.BoardTopic;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.notifications.EmailNotificationType;
import smithereen.model.photos.Photo;
import smithereen.templates.Templates;
import smithereen.text.TextProcessor;
import smithereen.util.BackgroundTaskRunner;
import smithereen.util.CryptoUtils;
import smithereen.util.UriBuilder;
import spark.Request;
import spark.utils.StringUtils;

public class Mailer{
	private static Mailer instance;
	private static final Logger LOG=LoggerFactory.getLogger(Mailer.class);

	private Session session;
	private PebbleEngine templateEngine;

	public static Mailer getInstance(){
		if(instance==null){
			instance=new Mailer();
		}
		return instance;
	}

	private Mailer(){
		updateSession();
		templateEngine=Templates.makeEngineInstance("email");
	}

	public void updateSession(){
		Properties props=new Properties();
		props.put("mail.smtp.host", Config.smtpServerAddress);
		props.put("mail.smtp.port", Config.smtpPort+"");
		if(StringUtils.isNotEmpty(Config.smtpUsername))
			props.put("mail.smtp.user", Config.smtpUsername);
		if(Config.smtpUseTLS)
			props.put("mail.smtp.starttls.enable", "true");
		if(StringUtils.isEmpty(Config.smtpPassword)){
			session=Session.getInstance(props);
		}else{
			props.put("mail.smtp.auth", "true");
			session=Session.getInstance(props, new Authenticator(){
				@Override
				protected PasswordAuthentication getPasswordAuthentication(){
					return new PasswordAuthentication(Config.smtpUsername, Config.smtpPassword);
				}
			});
		}
	}

	private static Lang getEmailLang(Request req, Account account){
		if(account!=null && account.prefs!=null && account.prefs.locale!=null){
			return Lang.get(account.prefs.locale);
		}else{
			return Utils.lang(req);
		}
	}

	public void sendPasswordReset(Request req, Account account, String code){
		Lang l=getEmailLang(req, account);
		String link=UriBuilder.local().appendPath("account").appendPath("actuallyResetPassword").queryParam("code", code).build().toString();
		String plaintext=l.get("email_password_reset_header")+"\n\n"+l.get("email_password_reset_plain_before", Map.of("name", account.user.firstName, "serverName", Config.serverDisplayName, "domain", Config.domain))+"\n\n"+link+"\n\n"+l.get("email_password_reset_after");
		send(account.email, l.get("email_password_reset_subject", Map.of("domain", Config.domain)), plaintext, "reset_password", Map.of(
				"domain", Config.domain,
				"serverName", Config.serverDisplayName,
				"name", account.user.firstName,
				"gender", account.user.gender,
				"passwordResetLink", link
		), l.getLocale());
	}

	public void sendTest(Request req, String to, Account self){
		Lang l=getEmailLang(req, self);
		send(to, l.get("email_test_subject"), l.get("email_test"), "test", Map.of("gender", self.user.gender), l.getLocale());
	}

	public void sendAccountActivation(Request req, Account self){
		Account.ActivationInfo info=self.activationInfo;
		if(info==null)
			throw new IllegalArgumentException("This account is already activated");
		if(info.emailState!=Account.ActivationInfo.EmailConfirmationState.NOT_CONFIRMED)
			throw new IllegalArgumentException("Unexpected email state "+info.emailState);
		if(info.emailConfirmationKey==null)
			throw new IllegalArgumentException("No email confirmation key");

		Lang l=getEmailLang(req, self);
		String link=UriBuilder.local().path("account", "activate").queryParam("key", info.emailConfirmationKey).build().toString();
		String plaintext=l.get("email_confirmation_header")+"\n\n"+l.get("email_confirmation_body_plain", Map.of("name", self.user.firstName, "serverName", Config.serverDisplayName))+"\n\n"+link;
		send(self.email, l.get("email_confirmation_subject", Map.of("serverName", Config.serverDisplayName)), plaintext, "activate_account", Map.of(
				"name", self.user.firstName,
				"gender", self.user.gender,
				"serverName", Config.serverDisplayName,
				"domain", Config.domain,
				"confirmationLink", link
		), l.getLocale());
	}

	public void sendEmailChange(Request req, Account self){
		Account.ActivationInfo info=self.activationInfo;
		if(info==null)
			throw new IllegalArgumentException("This account is already activated");
		if(info.emailState!=Account.ActivationInfo.EmailConfirmationState.CHANGE_PENDING)
			throw new IllegalArgumentException("Unexpected email state "+info.emailState);
		if(info.emailConfirmationKey==null)
			throw new IllegalArgumentException("No email confirmation key");

		Lang l=getEmailLang(req, self);
		String link=UriBuilder.local().path("account", "activate").queryParam("key", info.emailConfirmationKey).build().toString();
		String plaintext=l.get("email_change_header")+"\n\n"+l.get("email_change_new_body_plain", Map.of("name", self.user.firstName, "serverName", Config.serverDisplayName, "newAddress", self.getUnconfirmedEmail(), "oldAddress", self.email))+"\n\n"+link;
		send(self.getUnconfirmedEmail(), l.get("email_change_new_subject", Map.of("serverName", Config.serverDisplayName)), plaintext, "update_email_new", Map.of(
				"name", self.user.firstName,
				"gender", self.user.gender,
				"serverName", Config.serverDisplayName,
				"domain", Config.domain,
				"confirmationLink", link,
				"oldAddress", self.email,
				"newAddress", self.getUnconfirmedEmail()
		), l.getLocale());
	}

	public void sendEmailChangeDoneToPreviousAddress(Request req, Account self){
		Account.ActivationInfo info=self.activationInfo;
		if(info==null)
			throw new IllegalArgumentException("This account is already activated");
		if(info.emailState!=Account.ActivationInfo.EmailConfirmationState.CHANGE_PENDING)
			throw new IllegalArgumentException("Unexpected email state "+info.emailState);
		if(info.emailConfirmationKey==null)
			throw new IllegalArgumentException("No email confirmation key");

		Lang l=getEmailLang(req, self);
		String plaintext=l.get("email_change_header")+"\n\n"+TextProcessor.stripHTML(l.get("email_change_old_body", Map.of("name", self.user.firstName, "serverName", Config.serverDisplayName)), true);
		send(self.email, l.get("email_change_old_subject", Map.of("serverName", Config.serverDisplayName)), plaintext, "update_email_old", Map.of(
				"name", self.user.firstName,
				"gender", self.user.gender,
				"address", self.getUnconfirmedEmail(),
				"domain", Config.domain,
				"serverName", Config.serverDisplayName
		), l.getLocale());
	}

	public void sendSignupInvitation(Request req, Account self, String email, String code, String firstName, boolean isRequest){
		Lang l=getEmailLang(req, self);
		String link=UriBuilder.local().path("account", "register").queryParam("invite", code).build().toString();
		String plaintext=l.get(isRequest ? "email_invite_approved_header" : "email_invite_header")+"\n\n";
		plaintext+=TextProcessor.stripHTML(l.get(isRequest ? "email_invite_body_start_approved" : "email_invite_body_start", Map.of(
				"name", firstName,
				"inviterName", self.user.getFullName(),
				"serverName", Config.serverDisplayName
		)), true);
		plaintext+="\n\n"+l.get("email_invite_body_end_plain")+"\n\n"+link;
		send(email, l.get(isRequest ? "email_invite_subject_approved" : "email_invite_subject", Map.of("serverName", Config.serverDisplayName)), plaintext, "signup_invitation", Map.of(
				"name", firstName,
				"inviterName", self.user.getFullName(),
				"inviterLink", UriBuilder.local().path("users", Integer.toString(self.user.id)).build().toString(),
				"serverName", Config.serverDisplayName,
				"domain", Config.domain,
				"inviteLink", link,
				"isRequest", isRequest
		), l.getLocale());
	}

	public void sendAccountBanNotification(Account self, UserBanStatus banStatus, UserBanInfo banInfo){
		Lang l=Lang.get(self.prefs.locale);
		String header=switch(banStatus){
			case FROZEN -> l.get("email_account_frozen_header");
			case SUSPENDED -> l.get("email_account_suspended_header");
			default -> throw new IllegalArgumentException("Unexpected value: "+banStatus);
		};
		String htmlText=switch(banStatus){
			case FROZEN -> l.get("email_account_frozen_body", Map.of(
					"name", self.user.firstName,
					"serverName", Config.serverDisplayName,
					"domain", Config.domain,
					"date", banInfo.expiresAt()==null ? l.get("email_account_frozen_until_first_login") : l.formatDate(banInfo.expiresAt(), self.prefs.timeZone, false)
			));
			case SUSPENDED -> l.get("email_account_suspended_body", Map.of(
					"name", self.user.firstName,
					"serverName", Config.serverDisplayName,
					"domain", Config.domain,
					"deletionDate", l.formatDate(Objects.requireNonNull(banInfo.bannedAt()).plus(UserBanInfo.ACCOUNT_DELETION_DAYS, ChronoUnit.DAYS), self.prefs.timeZone, false)
			));
			default -> throw new IllegalArgumentException("Unexpected value: " + banStatus);
		};
		String subject=switch(banStatus){
			case FROZEN -> l.get("email_account_frozen_subject", Map.of("serverName", Config.serverDisplayName));
			case SUSPENDED -> l.get("email_account_suspended_subject", Map.of("serverName", Config.serverDisplayName));
			default -> throw new IllegalArgumentException("Unexpected value: " + banStatus);
		};
		String plainText=header+"\n\n"+TextProcessor.stripHTML(htmlText, true);
		htmlText="<h1>"+header+"</h1>\n\n"+htmlText;
		if(StringUtils.isNotEmpty(banInfo.message())){
			String messageFromStaff=l.get("message_from_staff", Map.of("message", banInfo.message()));
			htmlText+="<br/><br/>"+messageFromStaff;
			plainText+="\n\n"+TextProcessor.stripHTML(messageFromStaff, true);
		}
		send(self.email, subject, plainText, "generic", Map.of("text", htmlText), self.prefs.locale);
	}

	public void sendActionConfirmationCode(Request req, Account self, String action, String code){
		LOG.trace("Sending code {} for action {}", code, action);
		Lang l=getEmailLang(req, self);
		String plaintext=l.get("email_confirmation_code_header")+"\n\n";
		plaintext+=TextProcessor.stripHTML(l.get("email_confirmation_code", Map.of("name", self.user.firstName, "action", action)), true)+"\n\n"+code+"\n\n"+TextProcessor.stripHTML(l.get("email_confirmation_code_info"), true);
		String subject=l.get("email_confirmation_code_subject", Map.of("serverName", Config.serverDisplayName));
		send(self.email, subject, plaintext, "confirmation_code", Map.of("name", self.user.firstName, "action", action, "code", code), l.getLocale());
	}

	public void sendNotification(Account self, ApplicationContext ctx, EmailNotificationType type, Object object, OwnedContentObject relatedObject, User actor){
		Lang l=Lang.get(self.prefs.locale);
		HashMap<String, Object> params=new HashMap<>();
		params.put("self", self.user);
		params.put("actor", actor);
		params.put("timeZone", self.prefs.timeZone==null ? ZoneId.systemDefault() : self.prefs.timeZone);
		String plaintext=self.user.getFullName()+",\n\n";
		String subject;
		String templateName;

		String unsubscribeKey="a="+self.id+"&t="+type.ordinal()+"&e="+URLEncoder.encode(self.email, StandardCharsets.UTF_8);
		String unsubscribeURL=Config.localURI("/settings/notifications/emailUnsubscribe/"+Base64.getUrlEncoder().withoutPadding().encodeToString(CryptoUtils.aesGcmEncrypt(unsubscribeKey.getBytes(StandardCharsets.UTF_8), Config.emailUnsubscribeKey))).toString();
		params.put("unsubscribeURL", unsubscribeURL);

		switch(type){
			case FRIEND_REQUEST -> {
				FriendRequest req=ctx.getFriendsController().getIncomingFriendRequest(self.user, actor);
				params.put("req", req);
				templateName="friend_request";
				subject=TextProcessor.stripHTML(l.get("notification_content_friend_request", Map.of("name", actor.getFullName(), "gender", actor.gender)), false);
				plaintext+=l.get("email_friend_request_plaintext", Map.of(
						"name", actor.getCompleteName(),
						"gender", actor.gender,
						"url", Config.localURI("/my/incomingFriendRequests").toString()
				));
			}
			case MAIL -> {
				MailMessage msg=(MailMessage)object;
				params.put("msg", msg);
				templateName="mail_message";
				subject=TextProcessor.stripHTML(l.get("notification_content_mail_message", Map.of("name", actor.getFullName(), "gender", actor.gender)), false);
				plaintext+=l.get("email_mail_message_plaintext", Map.of(
						"name", actor.getCompleteName(),
						"gender", actor.gender,
						"url", Config.localURI("/my/mail/messages/"+msg.encodedID).toString()
				));
			}
			case PHOTO_TAG -> {
				Photo photo=(Photo) object;
				params.put("photo", photo);
				templateName="photo_tag";
				subject=TextProcessor.stripHTML(l.get("notification_content_photo_tag", Map.of("name", actor.getFullName(), "gender", actor.gender)), false);
				plaintext+=l.get("email_photo_tag_plaintext", Map.of(
						"name", actor.getCompleteName(),
						"gender", actor.gender,
						"url", Config.localURI("/photos/newTags").toString()
				));
			}
			case WALL_POST -> {
				Post post=(Post) object;
				params.put("parent", post);
				params.put("users", Map.of(actor.id, actor));
				params.put("headerText", l.get("notification_title_wall_post"));
				templateName="wall_post";
				subject=TextProcessor.stripHTML(l.get("notification_content_wall_post", Map.of("name", actor.getFullName(), "gender", actor.gender)), false);
				plaintext+=l.get("email_wall_post_plaintext", Map.of(
						"name", actor.getCompleteName(),
						"gender", actor.gender,
						"url", post.getInternalURL().toString()
				));
			}
			case WALL_COMMENT -> {
				Post comment=(Post) object;
				Post topLevel=(Post) relatedObject;
				params.put("parent", topLevel);
				params.put("comment1", comment);
				params.put("users", ctx.getUsersController().getUsers(Set.of(comment.authorID, topLevel.authorID)));
				params.put("headerText", l.get("notification_title_comment"));
				templateName="wall_post";
				subject=l.get("email_wall_comment_subject", Map.of("name", actor.getFullName(), "gender", actor.gender));
				plaintext+=l.get("email_wall_comment_plaintext", Map.of(
						"name", actor.getCompleteName(),
						"gender", actor.gender,
						"url", comment.getInternalURL().toString()
				));
			}
			case PHOTO_COMMENT -> {
				Comment comment=(Comment) object;
				Photo photo=(Photo) relatedObject;
				params.put("parent", photo);
				params.put("comment1", comment);
				params.put("users", ctx.getUsersController().getUsers(Set.of(comment.authorID, photo.authorID)));
				params.put("headerText", l.get("notification_title_comment"));
				templateName="photo_comment";
				subject=TextProcessor.stripHTML(l.get("notification_content_comment_photo", Map.of("name", actor.getFullName(), "gender", actor.gender)), false);
				plaintext+=l.get("email_photo_comment_plaintext", Map.of(
						"name", actor.getCompleteName(),
						"gender", actor.gender,
						"url", photo.getAbsoluteURL()
				));
			}
			case COMMENT_REPLY -> {
				PostLikeObject plo=(PostLikeObject) object;
				HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
				params.put("headerText", l.get("notification_title_reply"));
				params.put("comment2", object);
				needUsers.add(plo.authorID);
				needUsers.add(relatedObject.getAuthorID());
				templateName=switch(plo){
					case Post post -> {
						Post parent=ctx.getWallController().getPostOrThrow(post.replyKey.getFirst());
						params.put("parent", parent);
						needUsers.add(parent.authorID);
						params.put("comment1", relatedObject);
						yield "wall_post";
					}
					case Comment comment -> {
						params.put("parent", relatedObject);
						needUsers.add(relatedObject.getAuthorID());
						Comment replyTo=ctx.getCommentsController().getCommentIgnoringPrivacy(comment.replyKey.getLast());
						params.put("comment1", replyTo);
						needUsers.add(replyTo.authorID);
						yield switch(comment.parentObjectID.type()){
							case PHOTO -> "photo_comment";
							case BOARD_TOPIC -> {
								BoardTopic topic=(BoardTopic)relatedObject;
								needGroups.add(topic.groupID);
								params.put("useOwnerID", true);
								yield "board_comment";
							}
						};
					}
				};
				subject=l.get("email_comment_reply_subject", Map.of("name", actor.getFullName(), "gender", actor.gender));
				params.put("users", ctx.getUsersController().getUsers(needUsers));
				params.put("groups", ctx.getGroupsController().getGroupsByIdAsMap(needGroups));
				plaintext+=l.get("email_photo_comment_plaintext", Map.of(
						"name", actor.getCompleteName(),
						"gender", actor.gender,
						"url", plo.getInternalURL().toString()
				));
			}
			case MENTION -> {
				PostLikeObject plo=(PostLikeObject) object;
				HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
				params.put("headerText", l.get("notification_title_mention"));
				needUsers.add(plo.authorID);
				boolean isComment=plo instanceof Comment || plo.getReplyLevel()>0;
				subject=l.get(isComment ? "email_mention_comment_subject" : "email_mention_post_subject", Map.of("name", actor.getFullName(), "gender", actor.gender));
				plaintext+=l.get(isComment ? "email_mention_comment_plaintext" : "email_mention_post_plaintext", Map.of(
						"name", actor.getCompleteName(),
						"gender", actor.gender,
						"url", plo.getInternalURL().toString()
				));
				templateName=switch(plo){
					case Post post -> {
						if(post.getReplyLevel()==0){
							params.put("parent", post);
						}else{
							Post parent=ctx.getWallController().getPostOrThrow(post.replyKey.getFirst());
							params.put("parent", parent);
							params.put("comment1", post);
							needUsers.add(parent.authorID);
						}
						yield "wall_post";
					}
					case Comment comment -> {
						CommentableContentObject parent=ctx.getCommentsController().getCommentParentIgnoringPrivacy(comment);
						params.put("parent", parent);
						params.put("comment1", comment);
						needUsers.add(parent.getAuthorID());
						yield switch(comment.parentObjectID.type()){
							case PHOTO -> "photo_comment";
							case BOARD_TOPIC -> {
								BoardTopic topic=(BoardTopic)relatedObject;
								needGroups.add(topic.groupID);
								params.put("useOwnerID", true);
								yield "board_comment";
							}
						};
					}
				};
				params.put("users", ctx.getUsersController().getUsers(needUsers));
				params.put("groups", ctx.getGroupsController().getGroupsByIdAsMap(needGroups));
			}
			case GROUP_INVITE -> {
				Group group=(Group) object;
				templateName="group_invite";
				params.put("group", group);
				subject=l.get(group.isEvent() ? "email_event_invite_subject" : "email_group_invite_subject", Map.of("name", actor.getFullName(), "gender", actor.gender));
				plaintext+=l.get(group.isEvent() ? "email_event_invite_plaintext" : "email_group_invite_plaintext", Map.of(
						"name", actor.getCompleteName(),
						"gender", actor.gender,
						"groupName", group.name,
						"url", Config.localURI(group.isEvent() ? "/my/events/invites" : "/my/groups/invites").toString()
				));
			}
			default -> throw new IllegalStateException(type.toString());
		}

		plaintext+="\n\n______\n"+l.get("email_unsubscribe_footer_plaintext", Map.of("url", unsubscribeURL));

		send(self.email, subject, plaintext, templateName, params, l.getLocale(), unsubscribeURL);
	}

	private void send(String to, String subject, String plaintext, String templateName, Map<String, Object> templateParams, Locale templateLocale){
		send(to, subject, plaintext, templateName, templateParams, templateLocale, null);
	}

	private void send(String to, String subject, String plaintext, String templateName, Map<String, Object> templateParams, Locale templateLocale, String unsubscribeURL){
		try{
			MimeMessage msg=new MimeMessage(session);
			msg.setFrom(new InternetAddress(Config.mailFrom, Config.getServerDisplayName()));
			msg.setRecipients(Message.RecipientType.TO, to);
			msg.setSubject(subject);
			msg.setSentDate(new Date());
			if(unsubscribeURL!=null){
				msg.addHeader("List-Unsubscribe", "<"+unsubscribeURL+">");
				msg.addHeader("List-Unsubscribe-Post", "List-Unsubscribe=One-Click");
			}

			MimeBodyPart plainPart=new MimeBodyPart();
			plainPart.setContent(plaintext, "text/plain; charset=UTF-8");

			StringWriter writer=new StringWriter();
			PebbleTemplate template=templateEngine.getTemplate(templateName);
			HashMap<String, Object> params=new HashMap<>(templateParams);
			params.put("domain", Config.domain);
			params.put("serverName", Config.serverDisplayName);
			template.evaluate(writer, params, templateLocale);
			MimeBodyPart htmlPart=new MimeBodyPart();
			htmlPart.setContent(writer.toString().replaceAll("[\t\n\r]", ""), "text/html; charset=UTF-8");

			MimeMultipart multipart=new MimeMultipart();
			multipart.setSubType("alternative");
			multipart.addBodyPart(plainPart);
			multipart.addBodyPart(htmlPart);

			msg.setContent(multipart);

			BackgroundTaskRunner.getInstance().submit(new SendRunnable(msg));
		}catch(MessagingException|IOException x){
			LOG.error("Exception while creating an email", x);
		}
	}

	public static String generateConfirmationKey(){
		return Utils.randomAlphanumericString(64);
	}

	public static UnsubscribeLinkData decodeUnsubscribeLink(String key, ApplicationContext ctx){
		byte[] data=Base64.getUrlDecoder().decode(key);
		String paramsStr=new String(CryptoUtils.aesGcmDecrypt(data, Config.emailUnsubscribeKey), StandardCharsets.UTF_8);
		Map<String, String> params=UriBuilder.parseQueryString(paramsStr);
		int accountID=Utils.safeParseInt(params.get("a"));
		int typeIndex=Utils.parseIntOrDefault(params.get("t"), -1);
		if(typeIndex<0 || typeIndex>=EmailNotificationType.values().length)
			throw new IllegalArgumentException();
		EmailNotificationType type=EmailNotificationType.values()[typeIndex];
		try{
			Account account=ctx.getUsersController().getAccountOrThrow(accountID);
			if(!Objects.equals(account.email, params.get("e")))
				throw new IllegalArgumentException();
			return new UnsubscribeLinkData(account, type);
		}catch(ObjectNotFoundException x){
			throw new IllegalArgumentException();
		}
	}

	public record UnsubscribeLinkData(Account account, EmailNotificationType type){}

	private static class SendRunnable implements Runnable{

		private final MimeMessage msg;

		public SendRunnable(MimeMessage msg){
			this.msg=msg;
		}

		@Override
		public void run(){
			try{
				LOG.trace("Sending email to {}, subject {}", msg.getAllRecipients(), msg.getSubject());
				Transport.send(msg);
				LOG.trace("Sent email to {}", (Object) msg.getAllRecipients());
			}catch(MessagingException x){
				LOG.error("Exception while sending an email", x);
			}
		}
	}
}
