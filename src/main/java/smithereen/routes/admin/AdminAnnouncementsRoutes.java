package smithereen.routes.admin;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.ServerAnnouncement;
import smithereen.model.ServerRule;
import smithereen.model.WebDeltaResponse;
import smithereen.templates.RenderedTemplateResponse;
import spark.QueryParamsMap;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class AdminAnnouncementsRoutes{
	public static Object announcements(Request req, Response resp, Account self, ApplicationContext ctx){
		Lang l=lang(req);
		return new RenderedTemplateResponse("admin_announcements", req)
				.with("now", Instant.now())
				.paginate(ctx.getModerationController().getAllAnnouncements(offset(req), 50))
				.addMessage(req, "settingsAdminAnnouncementsMessage", "message")
				.pageTitle(l.get("admin_announcements")+" | "+l.get("menu_admin"));
	}

	public static Object createAnnouncementForm(Request req, Response resp, Account self, ApplicationContext ctx){
		Lang l=lang(req);
		return new RenderedTemplateResponse("admin_announcement_form", req)
				.with("now", Instant.now())
				.with("languages", Lang.list)
				.addNavBarItem(l.get("menu_admin"), "/settings/admin")
				.addNavBarItem(l.get("admin_announcements"), "/settings/admin/announcements")
				.addNavBarItem(l.get("admin_create_announcement"))
				.pageTitle(l.get("admin_create_announcement")+" | "+l.get("menu_admin"));
	}

	private static Map<String, ServerAnnouncement.Translation> parseAnnouncementTranslations(Request req){
		HashMap<String, ServerAnnouncement.Translation> parsedTranslations=new HashMap<>();
		QueryParamsMap translations=req.queryMap("translations");
		if(translations!=null){
			for(int i=0;translations.hasKey(i+"");i++){
				String tLang=translations.value(i+"", "lang");
				String tTitle=translations.value(i+"", "title");
				String tDescription=translations.value(i+"", "description");
				String tLinkText=translations.value(i+"", "linkText");
				String tLinkURL=translations.value(i+"", "linkURL");
				if(tTitle!=null && tTitle.length()>300)
					tTitle=tTitle.substring(0, 300);
				if(tLinkText!=null && tLinkText.length()>300)
					tLinkText=tLinkText.substring(0, 300);
				if(tLinkURL!=null && tLinkURL.length()>300)
					tLinkURL=tLinkURL.substring(0, 300);
				parsedTranslations.put(tLang, new ServerAnnouncement.Translation(tTitle, tDescription, tLinkText, tLinkURL));
			}
		}
		return parsedTranslations;
	}

	public static Object createAnnouncement(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "description", "showFrom_date", "showFrom_time", "showTo_date", "showFrom_time");

		Instant showFrom=instantFromDateAndTime(req, req.queryParams("showFrom_date"), req.queryParams("showFrom_time"));
		Instant showTo=instantFromDateAndTime(req, req.queryParams("showTo_date"), req.queryParams("showTo_time"));
		if(showFrom.isBefore(Instant.now()))
			showFrom=Instant.now();
		if(showTo.isBefore(showFrom))
			throw new UserErrorException("End time must be before start time");

		if(!isWithinDatabaseLimits(showTo) || !isWithinDatabaseLimits(showFrom))
			throw new BadRequestException();

		ctx.getModerationController().createAnnouncement(req.queryParams("title"), req.queryParams("description"), req.queryParams("linkTitle"), req.queryParams("linkURL"), showFrom, showTo, parseAnnouncementTranslations(req));

		req.session().attribute("settingsAdminAnnouncementsMessage", lang(req).get("admin_announcement_added"));
		return ajaxAwareRedirect(req, resp, "/settings/admin/announcements");
	}

	public static Object confirmDeleteAnnouncement(Request req, Response resp, Account self, ApplicationContext ctx){
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("admin_announcement_deletion"), l.get("admin_announcement_delete_confirm"), "/settings/admin/announcements/"+req.params(":id")+"/delete");
	}

	public static Object deleteAnnouncement(Request req, Response resp, Account self, ApplicationContext ctx){
		ServerAnnouncement announcement=ctx.getModerationController().getAnnouncement(safeParseInt(req.params(":id")));
		ctx.getModerationController().deleteAnnouncement(announcement);
		if(isAjax(req))
			return new WebDeltaResponse(resp).remove("announcement"+announcement.id());
		resp.redirect(back(req));
		return "";
	}

	public static Object editAnnouncement(Request req, Response resp, Account self, ApplicationContext ctx){
		ServerAnnouncement announcement=ctx.getModerationController().getAnnouncement(safeParseInt(req.params(":id")));
		Lang l=lang(req);
		return new RenderedTemplateResponse("admin_announcement_form", req)
				.with("now", Instant.now())
				.with("languages", Lang.list)
				.with("announcement", announcement)
				.addNavBarItem(l.get("menu_admin"), "/settings/admin")
				.addNavBarItem(l.get("admin_announcements"), "/settings/admin/announcements")
				.addNavBarItem(l.get("admin_announcement_edit"))
				.pageTitle(l.get("admin_create_announcement")+" | "+l.get("menu_admin"));
	}

	public static Object updateAnnouncement(Request req, Response resp, Account self, ApplicationContext ctx){
		ServerAnnouncement announcement=ctx.getModerationController().getAnnouncement(safeParseInt(req.params(":id")));
		requireQueryParams(req, "description", "showFrom_date", "showFrom_time", "showTo_date", "showFrom_time");

		Instant showFrom=instantFromDateAndTime(req, req.queryParams("showFrom_date"), req.queryParams("showFrom_time"));
		Instant showTo=instantFromDateAndTime(req, req.queryParams("showTo_date"), req.queryParams("showTo_time"));
		if(showFrom.isBefore(Instant.now()))
			showFrom=Instant.now();
		if(showTo.isBefore(showFrom))
			throw new UserErrorException("End time must be before start time");

		if(!isWithinDatabaseLimits(showTo) || !isWithinDatabaseLimits(showFrom))
			throw new BadRequestException();

		ctx.getModerationController().updateAnnouncement(announcement, req.queryParams("title"), req.queryParams("description"), req.queryParams("linkTitle"), req.queryParams("linkURL"), showFrom, showTo, parseAnnouncementTranslations(req));

		req.session().attribute("settingsAdminAnnouncementsMessage", lang(req).get("admin_announcement_updated"));
		return ajaxAwareRedirect(req, resp, "/settings/admin/announcements");
	}
}
