package smithereen.routes;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.LocalImage;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.SizedImage;
import smithereen.model.WebDeltaResponse;
import smithereen.model.apps.ClientApp;
import smithereen.storage.MediaStorageUtils;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class AppsRoutes{
	public static Object createAppForm(Request req, Response resp, Account self, ApplicationContext ctx){
		Lang l=lang(req);
		return new RenderedTemplateResponse("apps_edit", req)
				.addNavBarItem(l.get("settings"), "/settings")
				.addNavBarItem(l.get("menu_apps"), "/settings/apps")
				.addNavBarItem(l.get("settings_apps_create_title"))
				.pageTitle(l.get("settings_apps_create_title"));
	}

	public static Object createApp(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "name");
		String name=req.queryParams("name");
		String description=req.queryParams("description");
		String logoID=req.queryParams("logo");
		long id=ctx.getAppsController().createApp(self.user, name, description, logoID, parseRedirectUris(req));
		req.session().attribute("appsMessage", lang(req).get("apps_app_created"));
		return ajaxAwareRedirect(req, resp, "/apps/"+id+"/edit");
	}

	private static ClientApp getAppAndEnforceEditAccess(Request req, ApplicationContext ctx){
		long id=safeParseLong(req.params(":id"));
		ClientApp app=ctx.getAppsController().getAppByID(id);
		if(app.developerID!=currentUserAccount(req).user.id)
			throw new UserActionNotAllowedException();
		return app;
	}

	public static Object editAppForm(Request req, Response resp, Account self, ApplicationContext ctx){
		ClientApp app=getAppAndEnforceEditAccess(req, ctx);
		Lang l=lang(req);
		return new RenderedTemplateResponse("apps_edit", req)
				.with("app", app)
				.addNavBarItem(l.get("settings"), "/settings")
				.addNavBarItem(l.get("menu_apps"), "/settings/apps")
				.addNavBarItem(app.name, app.getURL())
				.addNavBarItem(l.get("apps_edit_title"))
				.pageTitle(l.get("apps_edit_title"))
				.addMessage(req, "appsMessage", "appInfoMessage");
	}

	public static Object editApp(Request req, Response resp, Account self, ApplicationContext ctx){
		ClientApp app=getAppAndEnforceEditAccess(req, ctx);
		Lang l=lang(req);
		requireQueryParams(req, "name");
		String name=req.queryParams("name");
		String description=req.queryParams("description");
		String logoID=req.queryParams("logo");
		ctx.getAppsController().updateApp(app, name, description, logoID, parseRedirectUris(req));
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.show("formMessage_appInfo")
					.setContent("formMessage_appInfo", l.get("apps_info_updated"));
		}
		resp.redirect(back(req));
		return "";
	}

	private static Set<URI> parseRedirectUris(Request req){
		String redirectUrls=req.queryParams("redirect_urls");
		if(StringUtils.isNotEmpty(redirectUrls)){
			LinkedHashSet<URI> redirectUris=new LinkedHashSet<>();
			for(String urlStr:redirectUrls.split("\n")){
				urlStr=urlStr.trim();
				try{
					URI uri=new URI(urlStr);
					if(StringUtils.isEmpty(uri.getAuthority()) || StringUtils.isEmpty(uri.getScheme()))
						throw new URISyntaxException(urlStr, "Scheme and authority are required");
					redirectUris.add(uri);
				}catch(URISyntaxException x){
					throw new UserErrorException("err_app_redirect_url_invalid", Map.of("url", urlStr));
				}
			}
			return redirectUris;
		}else{
			return Set.of();
		}
	}

	public static Object uploadAppLogo(Request req, Response resp, Account self, ApplicationContext ctx){
		if(!isAjax(req))
			throw new BadRequestException();
		Lang l=lang(req);
		LocalImage img=MediaStorageUtils.saveUploadedImage(req, resp, self, false, "file", 400, 0, vi->{
			if(vi.getWidth()!=vi.getHeight())
				Spark.halt(422, l.get("err_app_logo_not_square"));
			if(vi.getWidth()<200)
				Spark.halt(422, l.get("err_app_logo_too_small", Map.of("size", 200)));
		});
		return new WebDeltaResponse(resp)
				.hide("logoLoader")
				.show("logoUploadText")
				.setInputValue("logoIdField", img.getLocalID())
				.setContent("logoImageW", img.generateAvatarHTML(SizedImage.Type.AVA_SQUARE_MEDIUM, 75, 75, List.of(), List.of()));
	}

	public static Object appPage(Request req, Response resp){
		ApplicationContext ctx=context(req);
		ClientApp app=ctx.getAppsController().getAppByID(safeParseLong(req.params(":id")));
		return ajaxAwareRedirect(req, resp, app.getURL());
	}

	public static Object appPage(Request req, Response resp, long id){
		ApplicationContext ctx=context(req);
		ClientApp app=ctx.getAppsController().getAppByID(id);
		return new RenderedTemplateResponse("app_page", req)
				.with("app", app)
				.pageTitle(app.name);
	}
}
