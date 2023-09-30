package smithereen.routes;

import io.pebbletemplates.pebble.extension.escaper.SafeString;

import java.sql.SQLException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.LruCache;
import smithereen.Mailer;
import smithereen.Utils;
import smithereen.model.Account;
import smithereen.model.EmailCode;
import smithereen.model.SessionInfo;
import smithereen.model.SignupInvitation;
import smithereen.model.User;
import smithereen.model.WebDeltaResponse;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.storage.DatabaseUtils;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.util.FloodControl;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class SessionRoutes{
	private static void setupSessionWithAccount(Request req, Response resp, Account acc) throws SQLException{
		SessionInfo info=new SessionInfo();
		info.account=acc;
		req.session(true).attribute("info", info);
		String psid=SessionStorage.putNewSession(req.session());
		info.csrfToken=Utils.csrfTokenFromSessionID(Base64.getDecoder().decode(psid));
		if(acc.prefs.locale==null){
			Locale requestLocale=req.raw().getLocale();
			if(requestLocale!=null){
				acc.prefs.locale=requestLocale;
				SessionStorage.updatePreferences(acc.id, acc.prefs);
			}
		}
		resp.cookie("/", "psid", psid, 10*365*24*60*60, false);
	}

	public static Object login(Request req, Response resp) throws SQLException{
		if(redirectIfLoggedIn(req, resp))
			return "";

		String to=req.queryParams("to");
		if(to!=null && to.startsWith("/activitypub")){
			req.attribute("templateDir", "popup");
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse("login", req);
		if(req.requestMethod().equalsIgnoreCase("post")){
			Account acc=SessionStorage.getAccountForUsernameAndPassword(req.queryParams("username"), req.queryParams("password"));
			if(acc!=null){
				setupSessionWithAccount(req, resp, acc);
				if(StringUtils.isNotEmpty(to))
					resp.redirect(to);
				else
					resp.redirect("/feed");
				return "";
			}
			model.with("message", Utils.lang(req).get("login_incorrect"));
		}else if(StringUtils.isNotEmpty(req.queryParams("to"))){
			model.with("message", Utils.lang(req).get("login_needed"));
		}
		model.with("additionalParams", "?"+req.queryString()).with("title", lang(req).get("login_title")+" | "+Config.serverDisplayName);
		return model;
	}

	private static boolean redirectIfLoggedIn(Request req, Response resp){
		SessionInfo info=Utils.sessionInfo(req);
		if(info!=null && info.account!=null){
			resp.redirect("/feed");
			return true;
		}
		return false;
	}

	public static Object logout(Request req, Response resp) throws SQLException{
		if(Utils.requireAccount(req, resp) && Utils.verifyCSRF(req, resp)){
			SessionStorage.deleteSession(req.cookie("psid"));
			resp.removeCookie("psid");
			SessionInfo info=req.session().attribute("info");
			info.account=null;
			info.csrfToken=null;
			resp.redirect("/");
			return "";
		}
		return null;
	}

	private static RenderedTemplateResponse regError(Request req, String errKey){
		RenderedTemplateResponse model=new RenderedTemplateResponse("register", req)
				.with("message", Utils.lang(req).get(errKey))
				.with("username", req.queryParams("username"))
				.with("password", req.queryParams("password"))
				.with("password2", req.queryParams("password2"))
				.with("email", req.queryParams("email"))
				.with("first_name", req.queryParams("first_name"))
				.with("last_name", req.queryParams("last_name"))
				.with("invite", req.queryParams("invite"))
				.with("preFilledInvite", req.queryParams("_invite"))
				.with("signupMode", Config.signupMode)
				.with("title", lang(req).get("register"));
		if(Config.signupMode==Config.SignupMode.OPEN && Config.signupFormUseCaptcha){
			model.with("captchaSid", randomAlphanumericString(16));
		}
		return model;
	}

	public static Object register(Request req, Response resp) throws SQLException{
		if(redirectIfLoggedIn(req, resp))
			return "";

		String username=req.queryParams("username");
		if(StringUtils.isEmpty(username) || !Utils.isValidUsername(username))
			return regError(req, "err_reg_invalid_username");
		if(Utils.isReservedUsername(username))
			return regError(req, "err_reg_reserved_username");
		if(UserStorage.getByUsername(username)!=null)
			return regError(req, "err_reg_username_taken");

		final Object[] result={null};
		boolean r=DatabaseUtils.runWithUniqueUsername(username, ()->result[0]=doRegister(req, resp));
		if(!r){
			return regError(req, "err_reg_username_taken");
		}
		return result[0];
	}

	public static Object doRegister(Request req, Response resp) throws SQLException{
		if(redirectIfLoggedIn(req, resp))
			return "";

		String username=req.queryParams("username");
		String password=req.queryParams("password");
		String password2=req.queryParams("password2");
		String email=req.queryParams("email");
		String first=req.queryParams("first_name");
		String last=req.queryParams("last_name");
		String invite=req.queryParams("invite");

		if(StringUtils.isEmpty(password) || password.length()<4)
			return regError(req, "err_password_short");
		if(StringUtils.isEmpty(password2) || !password.equals(password2))
			return regError(req, "err_passwords_dont_match");
		if(StringUtils.isEmpty(email) || !Utils.isValidEmail(email))
			return regError(req, "err_invalid_email");
		if(StringUtils.isEmpty(first) || first.length()<2)
			return regError(req, "err_name_too_short");
		if(SessionStorage.getAccountByEmail(email)!=null)
			return regError(req, "err_reg_email_taken");
		SignupInvitation invitation=null;
		if(StringUtils.isEmpty(invite))
			invite=req.queryParams("_invite");
		if(Config.signupMode!=Config.SignupMode.OPEN || StringUtils.isNotEmpty(invite)){
			if(StringUtils.isEmpty(invite) || !invite.matches("^[A-Fa-f0-9]{32}$"))
				return regError(req, "err_invalid_invitation");
			invitation=context(req).getUsersController().getInvite(invite);
			if(invitation==null)
				return regError(req, "err_invalid_invitation");
		}

		if(Config.signupFormUseCaptcha && Config.signupMode==Config.SignupMode.OPEN){
			try{
				String captcha=requireFormField(req, "captcha", "err_wrong_captcha");
				String sid=requireFormField(req, "captchaSid", "err_wrong_captcha");
				LruCache<String, String> captchas=req.session().attribute("captchas");
				if(captchas==null || !captcha.equals(captchas.remove(sid)))
					throw new UserErrorException("err_wrong_captcha");
			}catch(UserErrorException x){
				return regError(req, x.getMessage());
			}
		}

		User.Gender gender=lang(req).detectGenderForName(first, last, null);

		SessionStorage.SignupResult res;
		if(Config.signupMode==Config.SignupMode.OPEN && invitation==null)
			res=SessionStorage.registerNewAccount(username, password, email, first, last, gender);
		else
			res=SessionStorage.registerNewAccount(username, password, email, first, last, gender, invite);
		if(res==SessionStorage.SignupResult.SUCCESS){
			Account acc=Objects.requireNonNull(SessionStorage.getAccountForUsernameAndPassword(username, password));
			if(Config.signupConfirmEmail && (invitation==null || StringUtils.isEmpty(invitation.email) || !email.equalsIgnoreCase(invitation.email))){
				Account.ActivationInfo info=new Account.ActivationInfo();
				info.emailState=Account.ActivationInfo.EmailConfirmationState.NOT_CONFIRMED;
				info.emailConfirmationKey=Mailer.generateConfirmationKey();
				acc.activationInfo=info;
				SessionStorage.updateActivationInfo(acc.id, info);
				Mailer.getInstance().sendAccountActivation(req, acc);
			}
			setupSessionWithAccount(req, resp, acc);
			resp.redirect("/feed");
		}else if(res==SessionStorage.SignupResult.USERNAME_TAKEN){
			return regError(req, "err_reg_username_taken");
		}else if(res==SessionStorage.SignupResult.INVITE_INVALID){
			return regError(req, "err_invalid_invitation");
		}

		return "";
	}

	public static Object registerForm(Request req, Response resp){
		if(redirectIfLoggedIn(req, resp))
			return "";

		String invite=req.queryParams("invite");
		if(Config.signupMode==Config.SignupMode.CLOSED && StringUtils.isEmpty(invite))
			return wrapError(req, resp, "signups_closed");
		RenderedTemplateResponse model=new RenderedTemplateResponse("register", req);
		model.with("signupMode", Config.signupMode);
		if(Config.signupMode==Config.SignupMode.OPEN && Config.signupFormUseCaptcha){
			model.with("captchaSid", randomAlphanumericString(16));
		}
		if(StringUtils.isNotEmpty(invite)){
			SignupInvitation inv=context(req).getUsersController().getInvite(invite);
			if(inv!=null){
				model.with("preFilledInvite", invite);
				if(StringUtils.isNotEmpty(inv.email)){
					model.with("email", inv.email);
				}
				if(StringUtils.isNotEmpty(inv.firstName)){
					model.with("first_name", inv.firstName);
					if(StringUtils.isNotEmpty(inv.lastName)){
						model.with("last_name", inv.lastName);
					}
				}
			}
		}
		model.with("title", lang(req).get("register"));
		return model;
	}

	public static Object resetPasswordForm(Request req, Response resp){
		if(redirectIfLoggedIn(req, resp))
			return "";

		return new RenderedTemplateResponse("reset_password", req).with("title", lang(req).get("reset_password_title"));
	}

	public static Object resetPassword(Request req, Response resp) throws SQLException{
		SessionInfo sess=sessionInfo(req);
		if(sess!=null && sess.account!=null)
			throw new BadRequestException();
		String username=req.queryParams("username");
		if(StringUtils.isEmpty(username))
			throw new BadRequestException();

		Account account;
		if(username.contains("@"))
			account=SessionStorage.getAccountByEmail(username);
		else
			account=SessionStorage.getAccountByUsername(username);
		if(account==null){
			return new RenderedTemplateResponse("reset_password", req)
					.with("username", username)
					.with("title", lang(req).get("reset_password_title"))
					.with("passwordResetMessage", lang(req).get("password_reset_account_not_found"));
		}

		FloodControl.PASSWORD_RESET.incrementOrThrow(account);

		EmailCode code=new EmailCode();
		code.type=EmailCode.Type.PASSWORD_RESET;
		code.accountID=account.id;
		String c=SessionStorage.storeEmailCode(code);
		Mailer.getInstance().sendPasswordReset(req, account, c);

		return new RenderedTemplateResponse("generic_message", req)
				.with("title", lang(req).get("reset_password_title"))
				.with("message", lang(req).get("password_reset_sent"));
	}

	public static Object actuallyResetPasswordForm(Request req, Response resp) throws SQLException{
		SessionInfo sess=sessionInfo(req);
		if(sess!=null && sess.account!=null)
			throw new BadRequestException();

		String code=req.queryParams("code");
		if(StringUtils.isEmpty(code))
			throw new BadRequestException();
		EmailCode c=SessionStorage.getEmailCode(code);
		if(c==null)
			throw new BadRequestException();
		if(c.isExpired())
			throw new BadRequestException();

		return new RenderedTemplateResponse("reset_password_step2", req)
				.with("code", code)
				.with("title", lang(req).get("reset_password_title"));
	}

	public static Object actuallyResetPassword(Request req, Response resp) throws SQLException{
		SessionInfo sess=sessionInfo(req);
		if(sess!=null && sess.account!=null)
			throw new BadRequestException();

		String code=req.queryParams("code");
		if(StringUtils.isEmpty(code))
			throw new BadRequestException();
		EmailCode c=SessionStorage.getEmailCode(code);
		if(c==null)
			throw new BadRequestException();
		if(c.isExpired())
			throw new BadRequestException();

		String error=null;
		String password=req.queryParams("password");
		String password2=req.queryParams("password2");
		if(StringUtils.isEmpty(password) || password.length()<4)
			error="err_password_short";
		if(StringUtils.isEmpty(password2) || !password.equals(password2))
			error="err_passwords_dont_match";

		if(error!=null){
			return new RenderedTemplateResponse("reset_password_step2", req)
					.with("code", code)
					.with("title", lang(req).get("reset_password_title"))
					.with("passwordResetMessage", lang(req).get(error));
		}

		SessionStorage.updatePassword(c.accountID, password);
		SessionStorage.deleteEmailCode(code);
		setupSessionWithAccount(req, resp, UserStorage.getAccount(c.accountID));
		resp.redirect("/feed");

		return "";
	}

	public static Object resendEmailConfirmation(Request req, Response resp, Account self, ApplicationContext ctx){
		if(self.getUnconfirmedEmail()==null)
			throw new BadRequestException();
		FloodControl.EMAIL_RESEND.incrementOrThrow(self.getUnconfirmedEmail());
		Mailer.getInstance().sendAccountActivation(req, self);
		Lang l=lang(req);
		String msg=l.get("email_confirmation_resent", Map.of("address", escapeHTML(self.getUnconfirmedEmail()))).replace("\n", "<br/>");
		msg=substituteLinks(msg, Map.of("change", Map.of("href", "/account/changeEmailForm", "data-ajax-box", "")));
		if(isAjax(req))
			return new WebDeltaResponse(resp).messageBox(l.get("account_activation"), msg, l.get("close"));
		return new RenderedTemplateResponse("generic_message", req).with("message", new SafeString(msg)).pageTitle(l.get("account_activation"));
	}

	public static Object changeEmailForm(Request req, Response resp, Account self, ApplicationContext ctx){
		if(self.getUnconfirmedEmail()==null)
			throw new BadRequestException();
		RenderedTemplateResponse model=new RenderedTemplateResponse("change_email_form", req);
		model.with("email", self.getUnconfirmedEmail());
		return wrapForm(req, resp, "change_email_form", "/account/changeEmail", lang(req).get("change_email_title"), "save", model);
	}

	public static Object changeEmail(Request req, Response resp, Account self, ApplicationContext ctx){
		if(self.activationInfo==null || self.activationInfo.emailState!=Account.ActivationInfo.EmailConfirmationState.NOT_CONFIRMED)
			throw new BadRequestException();
		String email=req.queryParams("email");
		if(!isValidEmail(email))
			throw new BadRequestException();
		if(email.equalsIgnoreCase(self.email))
			return "";
		try{
			if(SessionStorage.getAccountByEmail(email)!=null){
				if(isAjax(req)){
					return new WebDeltaResponse(resp).show("formMessage_changeEmail").setContent("formMessage_changeEmail", lang(req).get("err_reg_email_taken")).keepBox();
				}
				return "";
			}

			self.activationInfo.emailConfirmationKey=Mailer.generateConfirmationKey();
			SessionStorage.updateActivationInfo(self.id, self.activationInfo);
			SessionStorage.updateEmail(self.id, email);
			self.email=email;

			FloodControl.EMAIL_RESEND.incrementOrThrow(self.getUnconfirmedEmail());
			Mailer.getInstance().sendAccountActivation(req, self);

			if(isAjax(req))
				return new WebDeltaResponse(resp).refresh();
			resp.redirect("/feed");
			return "";
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public static Object activateAccount(Request req, Response resp, Account self, ApplicationContext ctx){
		if(self.activationInfo==null)
			throw new UserErrorException("err_email_already_activated");
		if(!self.activationInfo.emailConfirmationKey.equals(req.queryParams("key")))
			throw new UserErrorException("err_email_link_invalid");
		Account.ActivationInfo.EmailConfirmationState state=self.activationInfo.emailState;
		try{
			if(self.activationInfo.emailState==Account.ActivationInfo.EmailConfirmationState.CHANGE_PENDING){
				Mailer.getInstance().sendEmailChangeDoneToPreviousAddress(req, self);
				SessionStorage.updateEmail(self.id, self.activationInfo.newEmail);
				self.email=self.activationInfo.newEmail;
			}
			SessionStorage.updateActivationInfo(self.id, null);
			self.activationInfo=null;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
		return new RenderedTemplateResponse("email_confirm_success", req).with("activated", state==Account.ActivationInfo.EmailConfirmationState.NOT_CONFIRMED);
	}

	public static Object requestSignupInvite(Request req, Response resp){
		if(Config.signupMode!=Config.SignupMode.MANUAL_APPROVAL){
			throw new UserActionNotAllowedException();
		}
		try{
			String email=requireFormField(req, "email", "err_invalid_email");
			if(!isValidEmail(email))
				throw new UserErrorException("err_invalid_email");
			String firstName=requireFormField(req, "first_name", "err_name_too_short");
			String lastName=req.queryParams("last_name");
			if(StringUtils.isEmpty(lastName))
				lastName=null;
			String reason=requireFormField(req, "reason", "err_request_invite_reason_empty");
			if(Config.signupFormUseCaptcha){
				String captcha=requireFormField(req, "captcha", "err_wrong_captcha");
				String sid=requireFormField(req, "captchaSid", "err_wrong_captcha");
				LruCache<String, String> captchas=req.session().attribute("captchas");
				if(captchas==null || !captcha.equals(captchas.remove(sid)))
					throw new UserErrorException("err_wrong_captcha");
			}
			context(req).getUsersController().requestSignupInvite(req, firstName, lastName, email, reason);
			return new RenderedTemplateResponse("generic_message", req).with("message", lang(req).get("signup_request_submitted"));
		}catch(UserErrorException x){
			RenderedTemplateResponse model=new RenderedTemplateResponse("register_request_invite", req)
					.with("first_name", req.queryParams("first_name"))
					.with("last_name", req.queryParams("last_name"))
					.with("email", req.queryParams("email"))
					.with("reason", req.queryParams("reason"))
					.with("message", lang(req).get(x.getMessage()))
					.pageTitle(lang(req).get("signup_title"));
			if(Config.signupFormUseCaptcha){
				model.with("captchaSid", randomAlphanumericString(16));
			}
			return model;
		}
	}
}
