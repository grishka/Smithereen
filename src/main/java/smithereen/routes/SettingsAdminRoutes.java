package smithereen.routes;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import smithereen.Config;
import smithereen.Mailer;
import smithereen.data.Account;
import smithereen.data.User;
import smithereen.data.WebDeltaResponseBuilder;
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
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_server_info");
		Lang l=lang(req);
		model.with("title", l.get("profile_edit_basic")+" | "+l.get("menu_admin")).with("toolbarTitle", l.get("menu_admin"));
		model.with("serverName", Config.getServerDisplayName())
				.with("serverDescription", Config.serverDescription)
				.with("serverAdminEmail", Config.serverAdminEmail)
				.with("signupMode", Config.signupMode);
		String msg=req.session().attribute("admin.serverInfoMessage");
		if(StringUtils.isNotEmpty(msg)){
			req.session().removeAttribute("admin.serverInfoMessage");
			model.with("adminServerInfoMessage", msg);
		}
		return model.renderToString(req);
	}

	public static Object updateServerInfo(Request req, Response resp, Account self) throws SQLException{
		String name=req.queryParams("server_name");
		String descr=req.queryParams("server_description");
		String email=req.queryParams("server_admin_email");

		Config.serverDisplayName=name;
		Config.serverDescription=descr;
		Config.serverAdminEmail=email;
		Config.updateInDatabase("ServerDisplayName", name);
		Config.updateInDatabase("ServerDescription", descr);
		Config.updateInDatabase("ServerAdminEmail", email);
		try{
			Config.SignupMode signupMode=Config.SignupMode.valueOf(req.queryParams("signup_mode"));
			Config.signupMode=signupMode;
			Config.updateInDatabase("SignupMode", signupMode.toString());
		}catch(IllegalArgumentException ignore){}

		if(isAjax(req))
			return new WebDeltaResponseBuilder(resp).show("formMessage_adminServerInfo").setContent("formMessage_adminServerInfo", lang(req).get("admin_server_info_updated")).json();
		req.session().attribute("admin.serverInfoMessage", lang(req).get("admin_server_info_updated"));
		resp.redirect("/settings/admin");
		return "";
	}

	public static Object users(Request req, Response resp, Account self) throws SQLException{
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_users");
		Lang l=lang(req);
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		List<Account> accounts=UserStorage.getAllAccounts(offset, 100);
		model.with("accounts", accounts);
		model.with("title", l.get("admin_users")+" | "+l.get("menu_admin")).with("toolbarTitle", l.get("menu_admin"));
		model.with("total", UserStorage.getLocalUserCount());
		model.with("pageOffset", offset);
		model.with("wideOnDesktop", true);
		jsLangKey(req, "cancel");
		return model.renderToString(req);
	}

	public static Object accessLevelForm(Request req, Response resp, Account self) throws SQLException{
		Lang l=lang(req);
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null || target.id==self.id)
			throw new ObjectNotFoundException("err_user_not_found");
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_users_access_level");
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

	public static Object otherSettings(Request req, Response resp, Account self) throws SQLException{
		Lang l=lang(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_other_settings");
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
		return model.renderToString(req);
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

			result="settings_saved";
		}

		if(isAjax(req))
			return new WebDeltaResponseBuilder(resp).show("formMessage_adminEmailSettings").setContent("formMessage_adminEmailSettings", lang(req).get(result)).json();
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
			return new WebDeltaResponseBuilder(resp).show("formMessage_adminEmailTest").setContent("formMessage_adminEmailTest", lang(req).get(result)).json();
		req.session().attribute("admin.emailTestMessage", lang(req).get(result));
		resp.redirect("/settings/admin/other");
		return "";
	}
}
