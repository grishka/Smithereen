package smithereen.routes;

import java.sql.SQLException;
import java.util.List;

import smithereen.Config;
import smithereen.data.Account;
import smithereen.data.User;
import smithereen.data.WebDeltaResponseBuilder;
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
		model.with("title", l.get("profile_edit_basic")+" | "+l.get("menu_admin"));
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
		model.with("title", l.get("admin_users")+" | "+l.get("menu_admin"));
		model.with("total", UserStorage.getLocalUserCount());
		model.with("pageOffset", offset);
		jsLangKey(req, "cancel");
		return model.renderToString(req);
	}

	public static Object accessLevelForm(Request req, Response resp, Account self) throws SQLException{
		Lang l=lang(req);
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null || target.id==self.id)
			return wrapError(req, resp, "err_user_not_found");
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
}
