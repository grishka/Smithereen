package smithereen.routes;

import org.jtwig.JtwigModel;

import java.sql.SQLException;

import smithereen.Config;
import smithereen.data.Account;
import smithereen.data.WebDeltaResponseBuilder;
import smithereen.lang.Lang;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class SettingsAdminRoutes{
	public static Object index(Request req, Response resp, Account self){
		JtwigModel model=JtwigModel.newModel();
		Lang l=lang(req);
		model.with("title", l.get("profile_edit_basic")+" | "+l.get("menu_admin"));
		model.with("serverName", Config.getServerDisplayName())
				.with("serverDescription", Config.serverDescription)
				.with("serverAdminEmail", Config.serverAdminEmail);
		String msg=req.session().attribute("admin.serverInfoMessage");
		if(StringUtils.isNotEmpty(msg)){
			req.session().removeAttribute("admin.serverInfoMessage");
			model.with("adminServerInfoMessage", msg);
		}
		return renderTemplate(req, "admin_server_info", model);
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

		if(isAjax(req))
			return new WebDeltaResponseBuilder(resp).show("adminServerInfoMessage").setContent("adminServerInfoMessage", lang(req).get("admin_server_info_updated")).json();
		req.session().attribute("admin.serverInfoMessage", lang(req).get("admin_server_info_updated"));
		resp.redirect("/settings/admin");
		return "";
	}
}
