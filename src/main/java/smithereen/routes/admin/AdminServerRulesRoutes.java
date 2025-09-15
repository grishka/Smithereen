package smithereen.routes.admin;

import java.util.HashMap;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.exceptions.BadRequestException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.ServerRule;
import smithereen.model.WebDeltaResponse;
import smithereen.templates.RenderedTemplateResponse;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class AdminServerRulesRoutes{
	public static Object rules(Request req, Response resp, Account self, ApplicationContext ctx){
		Lang l=lang(req);
		return new RenderedTemplateResponse("admin_server_rules", req)
				.with("rules", ctx.getModerationController().getServerRules())
				.addMessage(req, "settingsAdminRulesMessage", "message")
				.pageTitle(l.get("admin_server_rules")+" | "+l.get("menu_admin"));
	}

	public static Object createRuleForm(Request req, Response resp, Account self, ApplicationContext ctx){
		Lang l=lang(req);
		return new RenderedTemplateResponse("admin_server_rule_form", req)
				.addNavBarItem(l.get("menu_admin"), "/settings/admin")
				.addNavBarItem(l.get("admin_server_rules"), "/settings/admin/rules")
				.addNavBarItem(l.get("admin_server_rules_create"))
				.with("languages", Lang.list)
				.pageTitle(l.get("admin_server_rules_create")+" | "+l.get("menu_admin"));
	}

	private static Map<String, ServerRule.Translation> parseServerRuleTranslations(Request req){
		HashMap<String, ServerRule.Translation> parsedTranslations=new HashMap<>();
		QueryParamsMap translations=req.queryMap("translations");
		if(translations!=null){
			for(int i=0;translations.hasKey(i+"");i++){
				String tLang=translations.value(i+"", "lang");
				String tTitle=translations.value(i+"", "title");
				String tDescription=translations.value(i+"", "description");
				if(StringUtils.isEmpty(tLang) || StringUtils.isEmpty(tTitle))
					throw new BadRequestException();
				if(tDescription==null)
					tDescription="";
				if(tTitle.length()>300)
					tTitle=tTitle.substring(0, 300);
				parsedTranslations.put(tLang, new ServerRule.Translation(tTitle, tDescription));
			}
		}
		return parsedTranslations;
	}

	public static Object createRule(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "title", "priority");
		String title=req.queryParams("title");
		String description=req.queryParamOrDefault("description", "");
		int priority=safeParseInt(req.queryParams("priority"));
		ctx.getModerationController().createServerRule(self.user, title, description, priority, parseServerRuleTranslations(req));
		req.session().attribute("settingsAdminRulesMessage", lang(req).get("admin_server_rule_added"));
		return ajaxAwareRedirect(req, resp, "/settings/admin/rules");
	}

	public static Object editRuleForm(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ServerRule rule=ctx.getModerationController().getServerRuleByID(id);
		Lang l=lang(req);
		return new RenderedTemplateResponse("admin_server_rule_form", req)
				.addNavBarItem(l.get("menu_admin"), "/settings/admin")
				.addNavBarItem(l.get("admin_server_rules"), "/settings/admin/rules")
				.addNavBarItem(l.get("admin_server_rules_edit"))
				.with("rule", rule)
				.with("languages", Lang.list)
				.pageTitle(l.get("admin_server_rules_edit")+" | "+l.get("menu_admin"));
	}

	public static Object updateRule(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ServerRule rule=ctx.getModerationController().getServerRuleByID(id);
		requireQueryParams(req, "title", "priority");
		String title=req.queryParams("title");
		String description=req.queryParamOrDefault("description", "");
		int priority=safeParseInt(req.queryParams("priority"));
		ctx.getModerationController().updateServerRule(self.user, rule, title, description, priority, parseServerRuleTranslations(req));
		req.session().attribute("settingsAdminRulesMessage", lang(req).get("admin_server_rule_updated"));
		return ajaxAwareRedirect(req, resp, "/settings/admin/rules");
	}

	public static Object confirmDeleteRule(Request req, Response resp, Account self, ApplicationContext ctx){
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("admin_server_rule_deletion"), l.get("admin_server_rule_delete_confirm"), "/settings/admin/rules/"+req.params(":id")+"/delete");
	}

	public static Object deleteRule(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ServerRule rule=ctx.getModerationController().getServerRuleByID(id);
		ctx.getModerationController().deleteServerRule(self.user, rule);
		if(isAjax(req))
			return new WebDeltaResponse(resp).remove("rule"+id);
		resp.redirect(back(req));
		return "";
	}
}
