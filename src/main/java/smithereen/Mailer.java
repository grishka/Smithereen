package smithereen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

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
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.UserBanInfo;
import smithereen.model.UserBanStatus;
import smithereen.templates.Templates;
import smithereen.text.TextProcessor;
import smithereen.util.BackgroundTaskRunner;
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
					"deletionDate", l.formatDate(Objects.requireNonNull(banInfo.bannedAt()).plus(30, ChronoUnit.DAYS), self.prefs.timeZone, false)
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

	private void send(String to, String subject, String plaintext, String templateName, Map<String, Object> templateParams, Locale templateLocale){
		try{
			MimeMessage msg=new MimeMessage(session);
			msg.setFrom(new InternetAddress(Config.mailFrom, Config.getServerDisplayName()));
			msg.setRecipients(Message.RecipientType.TO, to);
			msg.setSubject(subject);
			msg.setSentDate(new Date());

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
