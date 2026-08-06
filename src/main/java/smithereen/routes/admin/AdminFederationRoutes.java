package smithereen.routes.admin;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.FederationRestriction;
import smithereen.model.Server;
import smithereen.model.StatsPoint;
import smithereen.model.StatsType;
import smithereen.model.User;
import smithereen.model.WebDeltaResponse;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class AdminFederationRoutes{
	public static Object federationServerList(Request req, Response resp, Account self, ApplicationContext ctx){
		Server.Availability availability=switch(req.queryParamOrDefault("availability", "")){
			case "failing" -> Server.Availability.FAILING;
			case "down" -> Server.Availability.DOWN;
			default -> null;
		};
		String q=req.queryParams("q");

		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_server_list", req);
		model.paginate(ctx.getModerationController().getAllServers(offset(req), 100, availability, q));
		model.with("restrictions", (Function<String, FederationRestriction>) s->ctx.getModerationController().getDomainFederationRestriction(s));
		model.pageTitle(lang(req).get("admin_federation"));
		String baseURL=getRequestPathAndQuery(req);
		model.with("urlPath", baseURL)
				.with("availability", availability==null ? null : availability.toString().toLowerCase())
				.with("query", q);
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.setContent("ajaxUpdatable", model.renderBlock("ajaxPartialUpdate"))
					.setAttribute("domainSearch", "data-base-url", baseURL)
					.setURL(baseURL);
		}
		return model;
	}

	public static Object federationServerDetails(Request req, Response resp, Account self, ApplicationContext ctx){
		String domain=req.params(":domain");
		if(StringUtils.isEmpty(domain))
			throw new ObjectNotFoundException();
		Server server=ctx.getModerationController().getServerByDomain(domain);
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_federation_server", req);
		model.with("server", server);
		Map<Integer, User> users;
		FederationRestriction restriction=ctx.getModerationController().getDomainFederationRestriction(domain);
		if(restriction!=null){
			users=ctx.getUsersController().getUsers(Set.of(restriction.moderatorId));
		}else{
			users=Map.of();
		}
		model.with("users", users)
				.with("restriction", restriction);
		Lang l=lang(req);
		model.addNavBarItem(l.get("menu_admin"), "/settings/admin").addNavBarItem(l.get("admin_federation"), "/settings/admin/federation").addNavBarItem(server.host());
		model.pageTitle(server.host()+" | "+l.get("admin_federation"));
		jsLangKey(req, "month_full", "month_short", "month_standalone", "date_format_current_year", "date_format_other_year", "date_format_month_year", "date_format_month_year_short");
		if(!isMobile(req)){
			List<StatsPoint> sentActivities=ctx.getStatsController().getDaily(StatsType.SERVER_ACTIVITIES_SENT, server.id());
			List<StatsPoint> recvdActivities=ctx.getStatsController().getDaily(StatsType.SERVER_ACTIVITIES_RECEIVED, server.id());
			List<StatsPoint> failedActivities=ctx.getStatsController().getDaily(StatsType.SERVER_ACTIVITIES_FAILED_ATTEMPTS, server.id());
			model.with("graphData", makeGraphData(
					List.of(l.get("server_stats_activities_sent"), l.get("server_stats_activities_received"), l.get("server_stats_delivery_errors")),
					List.of(sentActivities, recvdActivities, failedActivities),
					timeZoneForRequest(req)
			).toString());
		}
		return model;
	}

	public static Object federationResetServerAvailability(Request req, Response resp, Account self, ApplicationContext ctx){
		String domain=req.params(":domain");
		if(StringUtils.isEmpty(domain))
			throw new ObjectNotFoundException();
		Server server=ctx.getModerationController().getServerByDomain(domain);
		ctx.getModerationController().resetServerAvailability(server);

		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object federationRules(Request req, Response resp, Account self, ApplicationContext ctx){
		List<FederationRestriction> restrictions=ctx.getModerationController().getAllFederationRestrictions();
		return new RenderedTemplateResponse("admin_federation_rules", req)
				.with("items", restrictions)
				.with("users", ctx.getUsersController().getUsers(restrictions.stream().map(r->r.moderatorId).collect(Collectors.toSet())))
				.pageTitle(lang(req).get("admin_federation_restrictions"));
	}

	public static Object federationRuleCreateForm(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_federation_restriction_form", req)
				.with("type", FederationRestriction.RestrictionType.SUSPENSION)
				.with("domain", req.queryParams("domain"));
		return wrapForm(req, resp, "admin_federation_restriction_form", "/settings/admin/federationRules/create", lang(req).get("federation_restriction_title"),
				"create", model);
	}

	public static Object federationRuleCreate(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "domain", "publicComment", "type");
		String domain=req.queryParams("domain").strip();
		String publicComment=req.queryParams("publicComment").strip();
		String privateComment=req.queryParams("privateComment");
		FederationRestriction.RestrictionType type=enumValue(req.queryParams("type"), FederationRestriction.RestrictionType.class);
		boolean obfuscate="on".equals(req.queryParams("obfuscate"));
		try{
			ctx.getModerationController().createFederationRestriction(self.user, domain, type, publicComment, privateComment, obfuscate);
		}catch(UserErrorException x){
			if(isAjax(req)){
				return new WebDeltaResponse(resp)
						.keepBox()
						.show("formMessage_federationRestriction")
						.setContent("formMessage_federationRestriction", lang(req).get(x.getMessage(), x.langArgs));
			}
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object federationRuleConfirmDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		Lang l=lang(req);
		String domain=req.params(":domain");
		return wrapConfirmation(req, resp, l.get("admin_federation_delete_rule"), l.get("admin_federation_delete_rule_confirm", Map.of("domain", domain)), "/settings/admin/federationRules/"+domain+"/delete");
	}

	public static Object federationRuleDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		String domain=req.params(":domain");
		ctx.getModerationController().deleteFederationRestriction(self.user, domain);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object federationRuleEditForm(Request req, Response resp, Account self, ApplicationContext ctx){
		String domain=req.params(":domain");
		FederationRestriction restriction=ctx.getModerationController().getDomainFederationRestriction(domain);
		if(restriction==null || !restriction.domain.equals(domain))
			throw new ObjectNotFoundException();
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_federation_restriction_form", req)
				.with("type", restriction.type)
				.with("editing", true)
				.with("domain", restriction.domain)
				.with("publicComment", restriction.publicComment)
				.with("privateComment", restriction.privateComment)
				.with("obfuscate", restriction.isDomainObfuscated());
		return wrapForm(req, resp, "admin_federation_restriction_form", "/settings/admin/federationRules/"+restriction.domain+"/edit", lang(req).get("federation_restriction_title"),
				"save", model);
	}

	public static Object federationRuleEdit(Request req, Response resp, Account self, ApplicationContext ctx){
		String domain=req.params(":domain");
		requireQueryParams(req, "publicComment", "type");

		String publicComment=req.queryParams("publicComment").strip();
		String privateComment=req.queryParams("privateComment");
		boolean obfuscate="on".equals(req.queryParams("obfuscate"));
		FederationRestriction.RestrictionType type=enumValue(req.queryParams("type"), FederationRestriction.RestrictionType.class);
		try{
			ctx.getModerationController().updateFederationRestriction(self.user, domain, type, publicComment, privateComment, obfuscate);
		}catch(UserErrorException x){
			if(isAjax(req)){
				return new WebDeltaResponse(resp)
						.keepBox()
						.show("formMessage_federationRestriction")
						.setContent("formMessage_federationRestriction", lang(req).get(x.getMessage(), x.langArgs));
			}
		}

		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(back(req));
		return "";
	}
}
