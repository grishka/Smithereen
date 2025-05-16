package smithereen.routes;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.WebDeltaResponse;
import smithereen.model.fasp.FASPCapability;
import smithereen.model.fasp.FASPProvider;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;

import static smithereen.Utils.*;

public class SettingsAdminFaspRoutes{
	public static Object activeFasps(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_fasp_active", req)
				.pageTitle(lang(req).get("admin_fasps"));
		model.with("unconfirmedFaspRequests", ctx.getFaspController().getUnconfirmedProviderCount())
				.with("fasps", ctx.getFaspController().getProviders(true));
		String msg=req.session().attribute("adminFaspMessage");
		if(msg!=null){
			req.session().removeAttribute("adminFaspMessage");
			model.with("resultMessage", msg);
		}
		return model;
	}

	public static Object faspRequests(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_fasp_requests", req)
				.pageTitle(lang(req).get("admin_fasps"));
		model.with("unconfirmedFaspRequests", ctx.getFaspController().getUnconfirmedProviderCount())
				.with("fasps", ctx.getFaspController().getProviders(false));
		return model;
	}

	public static Object confirmFaspRegistration(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		FASPProvider provider=ctx.getFaspController().getProvider(id);
		if(provider.confirmed)
			throw new ObjectNotFoundException();
		if(provider.capabilities.isEmpty()){
			ctx.getFaspController().reloadProviderInfo(provider);
			provider=ctx.getFaspController().getProvider(id);
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_fasp_confirm_registration", req)
				.with("provider", provider);
		return wrapForm(req, resp, "admin_fasp_confirm_registration", "/settings/admin/fasp/"+id+"/confirm", lang(req).get("admin_fasp_registration_title"), "admin_fasp_confirm_connection", model);
	}

	public static Object doConfirmFaspRegistration(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		FASPProvider provider=ctx.getFaspController().getProvider(id);
		if(provider.confirmed)
			throw new ObjectNotFoundException();
		EnumSet<FASPCapability> caps=EnumSet.noneOf(FASPCapability.class);
		for(FASPCapability cap:FASPCapability.values()){
			if("on".equals(req.queryParams("cap_"+cap)))
				caps.add(cap);
		}
		Map<FASPCapability, Throwable> errors=ctx.getFaspController().confirmFaspRegistrationRequest(provider, caps);
		Lang l=lang(req);
		String msg=l.get("admin_fasp_registration_success", Map.of("host", provider.baseUrl.getHost()));
		for(Map.Entry<FASPCapability, Throwable> e:errors.entrySet()){
			msg+="<br/>"+l.get("admin_fasp_capability_activation_error", Map.of("capability", l.get(e.getKey().getLangKey())))+" "+e.getValue().getMessage();
		}
		req.session().attribute("adminFaspMessage", msg);
		return ajaxAwareRedirect(req, resp, "/settings/admin/fasp");
	}

	public static Object rejectFaspRegistration(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		FASPProvider provider=ctx.getFaspController().getProvider(id);
		if(provider.confirmed)
			throw new ObjectNotFoundException();
		ctx.getFaspController().deleteFaspRegistrationRequest(provider);
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.setContent("faspRequestButton"+provider.id, "<div class=\"settingsMessage\">"+lang(req).get("admin_fasp_registration_rejected")+"</div>");
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object faspCapabilities(Request req, Response resp, Account self, ApplicationContext ctx){
		FASPProvider provider=getProvider(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_fasp_capabilities", req)
				.with("provider", provider);
		return wrapForm(req, resp, "admin_fasp_capabilities", "/settings/admin/fasp/"+provider.id+"/setCapabilities", lang(req).get("admin_fasp_capabilities_title"), "save", model);
	}

	public static Object setFaspCapabilities(Request req, Response resp, Account self, ApplicationContext ctx){
		FASPProvider provider=getProvider(req);
		EnumSet<FASPCapability> origCaps=provider.enabledCapabilities.isEmpty() ? EnumSet.noneOf(FASPCapability.class) : EnumSet.copyOf(provider.enabledCapabilities.keySet());
		EnumSet<FASPCapability> caps=EnumSet.noneOf(FASPCapability.class);
		for(FASPCapability cap:FASPCapability.values()){
			if("on".equals(req.queryParams("cap_"+cap)))
				caps.add(cap);
		}
		Map<FASPCapability, Throwable> errors=ctx.getFaspController().setProviderCapabilities(provider, caps);
		if(!errors.isEmpty()){
			Lang l=lang(req);
			ArrayList<String> lines=new ArrayList<>();
			for(Map.Entry<FASPCapability, Throwable> e:errors.entrySet()){
				lines.add(l.get(origCaps.contains(e.getKey()) ? "admin_fasp_capability_deactivation_error" : "admin_fasp_capability_activation_error", Map.of("capability", l.get(e.getKey().getLangKey())))+" "+e.getValue().getMessage());
			}
			req.session().attribute("adminFaspMessage", String.join("<br/>", lines));
		}
		return ajaxAwareRedirect(req, resp, "/settings/admin/fasp");
	}

	public static Object faspDebugCallbackLog(Request req, Response resp, Account self, ApplicationContext ctx){
		FASPProvider provider=getProvider(req);
		Lang l=lang(req);
		return new RenderedTemplateResponse("admin_fasp_debug", req)
				.pageTitle(l.get("admin_fasp_capability_debug"))
				.paginate(ctx.getFaspController().getProviderDebugCallbacks(provider, offset(req), 50))
				.with("provider", provider)
				.addNavBarItem(l.get("menu_admin"), "/settings/admin")
				.addNavBarItem(l.get("admin_fasps"), "/settings/admin/fasp")
				.addNavBarItem(provider.name+" — "+l.get("admin_fasp_capability_debug"));
	}

	public static Object faspDebugCallback(Request req, Response resp, Account self, ApplicationContext ctx){
		FASPProvider provider=getProvider(req);
		ctx.getFaspController().makeDebugCallbackRequest(provider);
		return ajaxAwareRedirect(req, resp, "/settings/admin/fasp/"+provider.id+"/capabilities/callback");
	}

	public static Object confirmDeleteFasp(Request req, Response resp, Account self, ApplicationContext ctx){
		FASPProvider provider=getProvider(req);
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("admin_fasp_delete_title"), l.get("admin_fasp_confirm_delete", Map.of("host", provider.baseUrl.getHost())), "/settings/admin/fasp/"+provider.id+"/delete");
	}

	public static Object deleteFasp(Request req, Response resp, Account self, ApplicationContext ctx){
		FASPProvider provider=getProvider(req);
		ctx.getFaspController().deleteProvider(provider);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	private static FASPProvider getProvider(Request req){
		FASPProvider provider=context(req).getFaspController().getProvider(safeParseInt(req.params(":id")));
		if(!provider.confirmed)
			throw new ObjectNotFoundException();
		return provider;
	}
}
