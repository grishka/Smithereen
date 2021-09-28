package smithereen;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

import smithereen.data.Account;
import smithereen.data.UriBuilder;
import smithereen.lang.Lang;
import smithereen.templates.Templates;
import smithereen.util.BackgroundTaskRunner;
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
			session=Session.getInstance(props, new Authenticator(){
				@Override
				protected PasswordAuthentication getPasswordAuthentication(){
					return new PasswordAuthentication(Config.smtpUsername, Config.smtpPassword);
				}
			});
		}
	}

	public void sendPasswordReset(Request req, Account account, String code){
		Lang l=Utils.lang(req);
		String link=UriBuilder.local().appendPath("account").appendPath("actuallyResetPassword").queryParam("code", code).build().toString();
		String plaintext=l.get("email_password_reset_plain_before", account.user.firstName, Config.domain)+"\n\n"+link+"\n\n"+l.get("email_password_reset_after");
		send(account.email, l.get("email_password_reset_subject", Config.domain), plaintext, "reset_password", Map.of(
				"domain", Config.domain,
				"serverName", Config.serverDisplayName,
				"name", account.user.firstName,
				"gender", account.user.gender,
				"passwordResetLink", link
		), l.getLocale());
	}

	public void sendTest(Request req, String to, Account self){
		Lang l=Utils.lang(req);
		send(to, l.get("email_test_subject"), l.get("email_test"), "test", Map.of("gender", self.user.gender), l.getLocale());
	}

	private void send(String to, String subject, String plaintext, String templateName, Map<String, Object> templateParams, Locale templateLocale){
		try{
			MimeMessage msg=new MimeMessage(session);
			msg.setFrom(new InternetAddress(Config.mailFrom, Config.getServerDisplayName()));
			msg.setRecipients(Message.RecipientType.TO, to);
			msg.setSubject(subject);

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

	private static class SendRunnable implements Runnable{

		private final MimeMessage msg;

		public SendRunnable(MimeMessage msg){
			this.msg=msg;
		}

		@Override
		public void run(){
			try{
				Transport.send(msg);
			}catch(MessagingException x){
				LOG.error("Exception while sending an email", x);
			}
		}
	}
}
