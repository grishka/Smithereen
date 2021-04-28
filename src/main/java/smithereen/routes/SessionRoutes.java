package smithereen.routes;

import java.sql.SQLException;
import java.util.Base64;
import java.util.Locale;

import smithereen.Config;
import smithereen.Mailer;
import smithereen.Utils;
import smithereen.data.Account;
import smithereen.data.EmailCode;
import smithereen.data.SessionInfo;
import smithereen.data.User;
import smithereen.exceptions.BadRequestException;
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
		SessionInfo info=Utils.sessionInfo(req);
		if(info!=null && info.account!=null){
			resp.redirect("/feed");
			return "";
		}
		String to=req.queryParams("to");
		if(to!=null && to.startsWith("/activitypub")){
			req.attribute("templateDir", "popup");
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse("login");
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
		return model.renderToString(req);
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

	private static String regError(Request req, String errKey){
		return new RenderedTemplateResponse("register")
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
				.with("title", lang(req).get("register"))
				.renderToString(req);
	}

	public static Object register(Request req, Response resp) throws SQLException{
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
		if(Config.signupMode!=Config.SignupMode.OPEN){
			if(StringUtils.isEmpty(invite))
				invite=req.queryParams("_invite");
			if(StringUtils.isEmpty(invite) || !invite.matches("[A-Fa-f0-9]{32}"))
				return regError(req, "err_invalid_invitation");
		}

		User.Gender gender=lang(req).detectGenderForName(first, last, null);

		SessionStorage.SignupResult res;
		if(Config.signupMode==Config.SignupMode.OPEN)
			res=SessionStorage.registerNewAccount(username, password, email, first, last, gender);
		else
			res=SessionStorage.registerNewAccount(username, password, email, first, last, gender, invite);
		if(res==SessionStorage.SignupResult.SUCCESS){
			Account acc=SessionStorage.getAccountForUsernameAndPassword(username, password);
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
		if(Config.signupMode==Config.SignupMode.CLOSED && StringUtils.isEmpty(req.queryParams("invite")))
			return wrapError(req, resp, "signups_closed");
		RenderedTemplateResponse model=new RenderedTemplateResponse("register");
		model.with("signupMode", Config.signupMode);
		String invite=req.queryParams("invite");
		if(StringUtils.isNotEmpty(invite))
			model.with("preFilledInvite", invite);
		model.with("title", lang(req).get("register"));
		return model.renderToString(req);
	}

	public static Object resetPasswordForm(Request req, Response resp){
		return new RenderedTemplateResponse("reset_password").with("title", lang(req).get("reset_password_title")).renderToString(req);
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
			return new RenderedTemplateResponse("reset_password")
					.with("username", username)
					.with("title", lang(req).get("reset_password_title"))
					.with("passwordResetMessage", lang(req).get("password_reset_account_not_found"))
					.renderToString(req);
		}

		FloodControl.PASSWORD_RESET.incrementOrThrow(account);

		EmailCode code=new EmailCode();
		code.type=EmailCode.Type.PASSWORD_RESET;
		code.accountID=account.id;
		String c=SessionStorage.storeEmailCode(code);
		Mailer.getInstance().sendPasswordReset(req, account, c);

		return new RenderedTemplateResponse("generic_message")
				.with("title", lang(req).get("reset_password_title"))
				.with("message", lang(req).get("password_reset_sent"))
				.renderToString(req);
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

		return new RenderedTemplateResponse("reset_password_step2")
				.with("code", code)
				.with("title", lang(req).get("reset_password_title"))
				.renderToString(req);
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
			return new RenderedTemplateResponse("reset_password_step2")
					.with("code", code)
					.with("title", lang(req).get("reset_password_title"))
					.with("passwordResetMessage", lang(req).get(error))
					.renderToString(req);
		}

		SessionStorage.updatePassword(c.accountID, password);
		SessionStorage.deleteEmailCode(code);
		setupSessionWithAccount(req, resp, UserStorage.getAccount(c.accountID));
		resp.redirect("/feed");

		return "";
	}
}
