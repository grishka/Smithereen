package smithereen.routes.admin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.admin.EmailDomainBlockRule;
import smithereen.model.admin.EmailDomainBlockRuleFull;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;

import static smithereen.Utils.*;

public class AdminEmailRulesRoutes{
	public static Object emailDomainRules(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_email_rules", req);
		List<EmailDomainBlockRuleFull> rules=ctx.getModerationController().getEmailDomainBlockRulesFull();
		model.pageTitle(lang(req).get("admin_email_domain_rules"))
				.with("rules", rules)
				.with("users", ctx.getUsersController().getUsers(rules.stream().map(EmailDomainBlockRuleFull::creatorID).collect(Collectors.toSet())))
				.addMessage(req, "adminEmailRulesMessage");
		return model;
	}

	public static Object emailDomainRuleCreateForm(Request req, Response resp, Account self, ApplicationContext ctx){
		return wrapForm(req, resp, "admin_email_rule_form", "/settings/admin/emailRules/create", lang(req).get("admin_email_rule_title"), "create", "adminCreateEmailRule", List.of(), key->null, null);
	}

	public static Object emailDomainRuleCreate(Request req, Response resp, Account self, ApplicationContext ctx){
		try{
			String domain=requireFormField(req, "domain", null);
			EmailDomainBlockRule.Action action=requireFormField(req, "ruleAction", null, EmailDomainBlockRule.Action.class);
			String note=req.queryParams("note");
			ctx.getModerationController().createEmailDomainBlockRule(self.user, domain, action, note);
		}catch(UserErrorException x){
			return wrapForm(req, resp, "admin_email_rule_form", "/settings/admin/emailRules/create", lang(req).get("admin_email_rule_title"), "create", "adminCreateEmailRule",
					List.of("domain", "ruleAction", "note"), req::queryParams, lang(req).get(x.getMessage()));
		}
		req.session().attribute("adminEmailRulesMessage", lang(req).get("admin_email_rule_created"));
		return ajaxAwareRedirect(req, resp, "/settings/admin/emailRules");
	}

	public static Object emailDomainRuleEdit(Request req, Response resp, Account self, ApplicationContext ctx){
		EmailDomainBlockRuleFull rule=ctx.getModerationController().getEmailDomainBlockRuleOrThrow(req.params(":domain"));
		return wrapForm(req, resp, "admin_email_rule_form", "/settings/admin/emailRules/"+rule.rule().domain()+"/update", lang(req).get("admin_email_rule_title"), "save", "adminCreateEmailRule",
				List.of("ruleAction", "note"), key->switch(key){
					case "ruleAction" -> rule.rule().action();
					case "note" -> rule.note();
					default -> throw new IllegalStateException("Unexpected value: " + key);
				}, null, Map.of("editing", true, "domain", rule.rule().domain()));
	}

	public static Object emailDomainRuleUpdate(Request req, Response resp, Account self, ApplicationContext ctx){
		EmailDomainBlockRuleFull rule=ctx.getModerationController().getEmailDomainBlockRuleOrThrow(req.params(":domain"));
		try{
			EmailDomainBlockRule.Action action=requireFormField(req, "ruleAction", null, EmailDomainBlockRule.Action.class);
			String note=req.queryParams("note");
			ctx.getModerationController().updateEmailDomainBlockRule(self.user, rule, action, note);
		}catch(UserErrorException x){
			return wrapForm(req, resp, "admin_email_rule_form", "/settings/admin/emailRules/"+rule.rule().domain()+"/update", lang(req).get("admin_email_rule_title"), "save", "adminCreateEmailRule",
					List.of("ruleAction", "note"), key->switch(key){
						case "ruleAction" -> rule.rule().action();
						case "note" -> rule.note();
						default -> throw new IllegalStateException("Unexpected value: " + key);
					}, lang(req).get(x.getMessage()), Map.of("editing", true, "domain", rule.rule().domain()));
		}
		return ajaxAwareRedirect(req, resp, "/settings/admin/emailRules");
	}

	public static Object emailDomainRuleConfirmDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		EmailDomainBlockRuleFull rule=ctx.getModerationController().getEmailDomainBlockRuleOrThrow(req.params(":domain"));
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("delete"), l.get("admin_confirm_delete_rule"), "/settings/admin/emailRules/"+rule.rule().domain()+"/delete");
	}

	public static Object emailDomainRuleDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		EmailDomainBlockRuleFull rule=ctx.getModerationController().getEmailDomainBlockRuleOrThrow(req.params(":domain"));
		ctx.getModerationController().deleteEmailDomainBlockRule(self.user, rule);
		return ajaxAwareRedirect(req, resp, "/settings/admin/emailRules");
	}
}
