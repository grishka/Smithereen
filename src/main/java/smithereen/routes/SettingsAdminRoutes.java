package smithereen.routes;

import java.net.URLEncoder;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import smithereen.Config;
import smithereen.Mailer;
import smithereen.Utils;
import smithereen.data.Account;
import smithereen.data.PaginatedList;
import smithereen.data.User;
import smithereen.data.WebDeltaResponse;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.lang.Lang;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class SettingsAdminRoutes{
	public static Object index(Request req, Response resp, Account self){
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_server_info", req);
		Lang l=lang(req);
		model.with("title", l.get("profile_edit_basic")+" | "+l.get("menu_admin")).with("toolbarTitle", l.get("menu_admin"));
		model.with("serverName", Config.getServerDisplayName())
				.with("serverDescription", Config.serverDescription)
				.with("serverShortDescription", Config.serverShortDescription)
				.with("serverPolicy", Config.serverPolicy)
				.with("serverAdminEmail", Config.serverAdminEmail)
				.with("signupMode", Config.signupMode);
		String msg=req.session().attribute("admin.serverInfoMessage");
		if(StringUtils.isNotEmpty(msg)){
			req.session().removeAttribute("admin.serverInfoMessage");
			model.with("adminServerInfoMessage", msg);
		}
		return model;
	}

	public static Object updateServerInfo(Request req, Response resp, Account self) throws SQLException{
		String name=req.queryParams("server_name");
		String descr=req.queryParams("server_description");
		String shortDescr=req.queryParams("server_short_description");
		String policy=req.queryParams("server_policy");
		String email=req.queryParams("server_admin_email");

		Config.serverDisplayName=name;
		Config.serverDescription=descr;
		Config.serverShortDescription=shortDescr;
		Config.serverPolicy=policy;
		Config.serverAdminEmail=email;
		Config.updateInDatabase(Map.of(
				"ServerDisplayName", name,
				"ServerDescription", descr,
				"ServerShortDescription", shortDescr,
				"ServerPolicy", policy,
				"ServerAdminEmail", email
		));
		try{
			Config.SignupMode signupMode=Config.SignupMode.valueOf(req.queryParams("signup_mode"));
			Config.signupMode=signupMode;
			Config.updateInDatabase("SignupMode", signupMode.toString());
		}catch(IllegalArgumentException ignore){}

		if(isAjax(req))
			return new WebDeltaResponse(resp).show("formMessage_adminServerInfo").setContent("formMessage_adminServerInfo", lang(req).get("admin_server_info_updated"));
		req.session().attribute("admin.serverInfoMessage", lang(req).get("admin_server_info_updated"));
		resp.redirect("/settings/admin");
		return "";
	}

	public static Object users(Request req, Response resp, Account self) throws SQLException{
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_users", req);
		Lang l=lang(req);
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		List<Account> accounts=UserStorage.getAllAccounts(offset, 100);
		model.paginate(new PaginatedList<>(accounts, UserStorage.getLocalUserCount(), offset, 100));
		model.with("title", l.get("admin_users")+" | "+l.get("menu_admin")).with("toolbarTitle", l.get("menu_admin"));
		model.with("wideOnDesktop", true);
		jsLangKey(req, "cancel", "yes", "no");
		return model;
	}

	public static Object accessLevelForm(Request req, Response resp, Account self) throws SQLException{
		Lang l=lang(req);
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null || target.id==self.id)
			throw new ObjectNotFoundException("err_user_not_found");
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_users_access_level", req);
		model.with("targetAccount", target);
		return wrapForm(req, resp, "admin_users_access_level", "/settings/admin/users/setAccessLevel", l.get("access_level"), "save", model);
	}

	public static Object setUserAccessLevel(Request req, Response resp, Account self) throws SQLException{
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null || target.id==self.id)
			return wrapError(req, resp, "err_user_not_found");
		try{
			UserStorage.setAccountAccessLevel(target.id, Account.AccessLevel.valueOf(req.queryParams("level")));
		}catch(IllegalArgumentException x){}
		if(isAjax(req)){
			resp.type("application/json");
			return "[]";
		}
		return "";
	}

	public static Object banUserForm(Request req, Response resp, Account self) throws SQLException{
		Lang l=lang(req);
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null || target.id==self.id || target.accessLevel.ordinal()>=Account.AccessLevel.MODERATOR.ordinal())
			throw new ObjectNotFoundException("err_user_not_found");
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_ban_user", req);
		model.with("targetAccount", target);
		return wrapForm(req, resp, "admin_ban_user", "/settings/admin/users/ban?accountID="+accountID, l.get("admin_ban"), "admin_ban", model);
	}

	public static Object banUser(Request req, Response resp, Account self) throws SQLException{
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null || target.id==self.id || target.accessLevel.ordinal()>=Account.AccessLevel.MODERATOR.ordinal())
			throw new ObjectNotFoundException("err_user_not_found");
		Account.BanInfo banInfo=new Account.BanInfo();
		banInfo.reason=req.queryParams("message");
		banInfo.adminUserId=self.user.id;
		banInfo.when=Instant.now();
		UserStorage.putAccountBanInfo(accountID, banInfo);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object confirmUnbanUser(Request req, Response resp, Account self) throws SQLException{
		req.attribute("noHistory", true);
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null)
			throw new ObjectNotFoundException("err_user_not_found");
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		User user=target.user;
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("admin_unban_X_confirm", Map.of("name", user.getFirstLastAndGender()))).with("formAction", "/settings/admin/users/unban?accountID="+accountID+"&_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object unbanUser(Request req, Response resp, Account self) throws SQLException{
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null)
			throw new ObjectNotFoundException("err_user_not_found");
		UserStorage.putAccountBanInfo(accountID, null);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object otherSettings(Request req, Response resp, Account self) throws SQLException{
		Lang l=lang(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_other_settings", req);
		model.with("title", l.get("admin_other")+" | "+l.get("menu_admin")).with("toolbarTitle", l.get("menu_admin"));
		model.with("from", Config.mailFrom)
				.with("smtpServer", Config.smtpServerAddress)
				.with("smtpPort", Config.smtpPort)
				.with("smtpUser", Config.smtpUsername)
				.with("smtpPassword", Config.smtpPassword)
				.with("smtpUseTLS", Config.smtpUseTLS);
		String msg=req.session().attribute("admin.emailTestMessage");
		if(StringUtils.isNotEmpty(msg)){
			req.session().removeAttribute("admin.emailTestMessage");
			model.with("adminEmailTestMessage", msg);
		}
		msg=req.session().attribute("admin.emailSettingsMessage");
		if(StringUtils.isNotEmpty(msg)){
			req.session().removeAttribute("admin.emailSettingsMessage");
			model.with("adminEmailSettingsMessage", msg);
		}
		return model;
	}

	public static Object saveEmailSettings(Request req, Response resp, Account self) throws SQLException{
		String from=req.queryParams("from");
		int smtpPort=parseIntOrDefault(req.queryParams("smtp_port"), 25);
		String smtpServer=req.queryParams("smtp_server");
		String smtpUser=req.queryParams("smtp_user");
		String smtpPassword=req.queryParams("smtp_password");
		boolean smtpUseTLS="on".equals(req.queryParams("smtp_use_tls"));

		if(smtpPort<1 || smtpPort>65535)
			smtpPort=25;

		String result;
		if(!isValidEmail(from)){
			result="err_invalid_email";
		}else{
			Config.mailFrom=from;
			Config.smtpPort=smtpPort;
			Config.smtpServerAddress=smtpServer;
			Config.smtpUsername=smtpUser;
			Config.smtpPassword=smtpPassword;
			Config.smtpUseTLS=smtpUseTLS;

			Config.updateInDatabase(Map.of(
					"MailFrom", from,
					"Mail_SMTP_ServerPort", smtpPort+"",
					"Mail_SMTP_ServerAddress", smtpServer,
					"Mail_SMTP_Username", smtpUser,
					"Mail_SMTP_Password", smtpPassword,
					"Mail_SMTP_UseTLS", smtpUseTLS ? "1" : "0"
			));

			Mailer.getInstance().updateSession();

			result="settings_saved";
		}

		if(isAjax(req))
			return new WebDeltaResponse(resp).show("formMessage_adminEmailSettings").setContent("formMessage_adminEmailSettings", lang(req).get(result));
		req.session().attribute("admin.emailSettingsMessage", lang(req).get(result));
		resp.redirect("/settings/admin/other");
		return "";
	}

	public static Object sendTestEmail(Request req, Response resp, Account self) throws SQLException{
		String to=req.queryParams("email");
		String result;
		if(isValidEmail(to)){
			Mailer.getInstance().sendTest(req, to, self);
			result="admin_email_test_sent";
		}else{
			result="err_invalid_email";
		}

		if(isAjax(req))
			return new WebDeltaResponse(resp).show("formMessage_adminEmailTest").setContent("formMessage_adminEmailTest", lang(req).get(result));
		req.session().attribute("admin.emailTestMessage", lang(req).get(result));
		resp.redirect("/settings/admin/other");
		return "";
	}
}
