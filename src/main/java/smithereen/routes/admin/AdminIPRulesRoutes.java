package smithereen.routes.admin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.admin.IPBlockRule;
import smithereen.model.admin.IPBlockRuleFull;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.util.InetAddressRange;
import spark.Request;
import spark.Response;

import static smithereen.Utils.*;

public class AdminIPRulesRoutes{
	public static Object ipRules(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_ip_rules", req);
		List<IPBlockRuleFull> rules=ctx.getModerationController().getIPBlockRulesFull();
		model.pageTitle(lang(req).get("admin_ip_rules"))
				.with("rules", rules)
				.with("users", ctx.getUsersController().getUsers(rules.stream().map(IPBlockRuleFull::creatorID).collect(Collectors.toSet())))
				.addMessage(req, "adminIPRulesMessage");
		return model;
	}

	public static Object ipRuleCreateForm(Request req, Response resp, Account self, ApplicationContext ctx){
		return wrapForm(req, resp, "admin_ip_rule_form", "/settings/admin/ipRules/create", lang(req).get("admin_ip_rule_title"), "create", "adminCreateIPRule", List.of(), key->null, null);
	}

	public static Object ipRuleCreate(Request req, Response resp, Account self, ApplicationContext ctx){
		try{
			InetAddressRange address=InetAddressRange.parse(requireFormField(req, "ipAddress", null));
			if(address==null)
				throw new UserErrorException("err_admin_ip_format_invalid");
			IPBlockRule.Action action=requireFormField(req, "ruleAction", null, IPBlockRule.Action.class);
			String note=req.queryParams("note");
			int expiry=Math.min(129600, Math.max(60, safeParseInt(requireFormField(req, "expiry", null))));
			ctx.getModerationController().createIPBlockRule(self.user, address, action, expiry, note);
		}catch(UserErrorException x){
			return wrapForm(req, resp, "admin_email_rule_form", "/settings/admin/ipRules/create", lang(req).get("admin_ip_rule_title"), "create", "adminCreateIPRule",
					List.of("domain", "ruleAction", "note"), req::queryParams, lang(req).get(x.getMessage()));
		}
		req.session().attribute("adminIPRulesMessage", lang(req).get("admin_ip_rule_created"));
		return ajaxAwareRedirect(req, resp, "/settings/admin/ipRules");
	}

	public static Object ipRuleEdit(Request req, Response resp, Account self, ApplicationContext ctx){
		IPBlockRuleFull rule=ctx.getModerationController().getIPBlockRuleFull(safeParseInt(req.params(":id")));
		return wrapForm(req, resp, "admin_ip_rule_form", "/settings/admin/ipRules/"+rule.rule().id()+"/update", lang(req).get("admin_ip_rule_title"), "save", "adminCreateIPRule",
				List.of("ruleAction", "note", "expiry"), key->switch(key){
					case "ruleAction" -> rule.rule().action();
					case "note" -> rule.note();
					case "expiry" -> rule.rule().expiresAt();
					default -> throw new IllegalStateException("Unexpected value: " + key);
				}, null, Map.of("editing", true, "ipAddress", rule.rule().ipRange().toString()));
	}

	public static Object ipRuleUpdate(Request req, Response resp, Account self, ApplicationContext ctx){
		IPBlockRuleFull rule=ctx.getModerationController().getIPBlockRuleFull(safeParseInt(req.params(":id")));
		try{
			IPBlockRule.Action action=requireFormField(req, "ruleAction", null, IPBlockRule.Action.class);
			String note=req.queryParams("note");
			int expiry=Math.min(129600, safeParseInt(requireFormField(req, "expiry", null)));
			if(expiry<60)
				expiry=0;
			ctx.getModerationController().updateIPBlockRule(self.user, rule, action, expiry, note);
		}catch(UserErrorException x){
			return wrapForm(req, resp, "admin_email_rule_form", "/settings/admin/ipRules/"+rule.rule().id()+"/update", lang(req).get("admin_ip_rule_title"), "save", "adminCreateIPRule",
					List.of("ruleAction", "note", "expiry"), key->switch(key){
						case "ruleAction" -> rule.rule().action();
						case "note" -> rule.note();
						case "expiry" -> rule.rule().expiresAt();
						default -> throw new IllegalStateException("Unexpected value: " + key);
					}, null, Map.of("editing", true, "ipAddress", rule.rule().ipRange().toString()));
		}
		return ajaxAwareRedirect(req, resp, "/settings/admin/ipRules");
	}

	public static Object ipRuleConfirmDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		IPBlockRuleFull rule=ctx.getModerationController().getIPBlockRuleFull(safeParseInt(req.params(":id")));
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("delete"), l.get("admin_confirm_delete_rule"), "/settings/admin/ipRules/"+rule.rule().id()+"/delete");
	}

	public static Object ipRuleDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		IPBlockRuleFull rule=ctx.getModerationController().getIPBlockRuleFull(safeParseInt(req.params(":id")));
		ctx.getModerationController().deleteIPBlockRule(self.user, rule);
		return ajaxAwareRedirect(req, resp, "/settings/admin/ipRules");
	}
}
