package smithereen.routes.admin;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import smithereen.ApplicationContext;
import smithereen.exceptions.ObjectNotFoundException;
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
		boolean onlyRestricted=req.queryParams("restricted")!=null;
		String q=req.queryParams("q");

		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_server_list", req);
		model.paginate(ctx.getModerationController().getAllServers(offset(req), 100, availability, onlyRestricted, q));
		model.pageTitle(lang(req).get("admin_federation"));
		String baseURL=getRequestPathAndQuery(req);
		model.with("urlPath", baseURL)
				.with("availability", availability==null ? null : availability.toString().toLowerCase())
				.with("onlyRestricted", onlyRestricted)
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
		if(server.restriction()!=null){
			users=ctx.getUsersController().getUsers(Set.of(server.restriction().moderatorId));
		}else{
			users=Map.of();
		}
		model.with("users", users);
		Lang l=lang(req);
		model.addNavBarItem(l.get("menu_admin"), "/settings/admin").addNavBarItem(l.get("admin_federation"), "/settings/admin/federation").addNavBarItem(server.host());
		model.pageTitle(server.host()+" | "+l.get("admin_federation"));
		jsLangKey(req, "month_full", "month_short", "month_standalone", "date_format_current_year", "date_format_other_year", "date_format_month_year", "date_format_month_year_short");
		if(!isMobile(req)){
			List<StatsPoint> sentActivities=ctx.getStatsController().getDaily(StatsType.SERVER_ACTIVITIES_SENT, server.id());
			List<StatsPoint> recvdActivities=ctx.getStatsController().getDaily(StatsType.SERVER_ACTIVITIES_RECEIVED, server.id());
			List<StatsPoint> failedActivities=ctx.getStatsController().getDaily(StatsType.SERVER_ACTIVITIES_FAILED_ATTEMPTS, server.id());
			String gd;
			model.with("graphData", gd=makeGraphData(
					List.of(l.get("server_stats_activities_sent"), l.get("server_stats_activities_received"), l.get("server_stats_delivery_errors")),
					List.of(sentActivities, recvdActivities, failedActivities),
					timeZoneForRequest(req)
			).toString());
		}
		return model;
	}

	public static Object federationServerRestrictionForm(Request req, Response resp, Account self, ApplicationContext ctx){
		String domain=req.params(":domain");
		if(StringUtils.isEmpty(domain))
			throw new ObjectNotFoundException();
		Server server=ctx.getModerationController().getServerByDomain(domain);

		Object form=wrapForm(req, resp, "admin_federation_restriction_form", "/settings/admin/federation/"+domain+"/restrict",
				lang(req).get("federation_restriction_title"), "save", "federationRestriction", List.of("type", "privateComment", "publicComment"), k->switch(k){
					case "type" -> (server.restriction()!=null ? server.restriction().type : FederationRestriction.RestrictionType.NONE).toString();
					case "privateComment" -> server.restriction()!=null ? server.restriction().privateComment : null;
					case "publicComment" -> server.restriction()!=null ? server.restriction().publicComment : null;
					default -> throw new IllegalArgumentException();
				}, null);
		if(form instanceof WebDeltaResponse wdr){
			wdr.runScript("""
					function federationServerRestrictionForm_updateFieldVisibility(){
						var publicComment=ge("formRow_publicComment");
						var privateComment=ge("formRow_privateComment");
						var publicCommentField=ge("publicComment");
						if(ge("type0").checked){
							publicComment.hide();
							privateComment.hide();
							publicCommentField.required=false;
						}else{
							publicComment.show();
							privateComment.show();
							publicCommentField.required=true;
						}
					}
					ge("type0").addEventListener("change", function(){federationServerRestrictionForm_updateFieldVisibility();}, false);
					ge("type1").addEventListener("change", function(){federationServerRestrictionForm_updateFieldVisibility();}, false);
					federationServerRestrictionForm_updateFieldVisibility();""");
		}
		return form;
	}

	public static Object federationRestrictServer(Request req, Response resp, Account self, ApplicationContext ctx){
		String domain=req.params(":domain");
		if(StringUtils.isEmpty(domain))
			throw new ObjectNotFoundException();
		Server server=ctx.getModerationController().getServerByDomain(domain);
		requireQueryParams(req, "type");

		FederationRestriction.RestrictionType type=enumValue(req.queryParams("type"), FederationRestriction.RestrictionType.class);
		if(type!=FederationRestriction.RestrictionType.NONE){
			requireQueryParams(req, "publicComment");
			FederationRestriction r=new FederationRestriction();
			r.type=type;
			r.createdAt=Instant.now();
			r.moderatorId=self.user.id;
			r.publicComment=req.queryParams("publicComment");
			r.privateComment=req.queryParamOrDefault("privateComment", "");
			ctx.getModerationController().setServerRestriction(server, r);
		}else{
			ctx.getModerationController().setServerRestriction(server, null);
		}

		if(isAjax(req)){
			return new WebDeltaResponse(resp).refresh();
		}
		resp.redirect(back(req));
		return "";
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
}
