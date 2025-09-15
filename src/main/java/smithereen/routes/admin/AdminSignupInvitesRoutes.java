package smithereen.routes.admin;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.PaginatedList;
import smithereen.model.SignupInvitation;
import smithereen.model.User;
import smithereen.model.WebDeltaResponse;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;

import static smithereen.Utils.*;

public class AdminSignupInvitesRoutes{
	public static Object invites(Request req, Response resp, Account self, ApplicationContext ctx){
		PaginatedList<SignupInvitation> invites=ctx.getModerationController().getAllSignupInvites(offset(req), 100);
		Map<Integer, Account> accounts=ctx.getModerationController().getAccounts(invites.list.stream().map(inv->inv.ownerID).collect(Collectors.toSet()));
		Map<Integer, User> users=accounts.values().stream().map(a->a.user).collect(Collectors.toMap(u->u.id, Function.identity()));
		for(SignupInvitation inv:invites.list){
			if(accounts.containsKey(inv.ownerID)){
				inv.ownerID=accounts.get(inv.ownerID).user.id;
			}else{
				inv.ownerID=0;
			}
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_invites", req)
				.paginate(invites)
				.with("users", users)
				.pageTitle(lang(req).get("admin_invites"))
				.addMessage(req, "adminInviteMessage");
		return model;
	}

	public static Object confirmDeleteInvite(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("delete"), l.get("confirm_delete_invite"), "/settings/admin/invites/"+id+"/delete");
	}

	public static Object deleteInvite(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ctx.getModerationController().deleteSignupInvite(self.user, id);
		req.session().attribute("adminInviteMessage", lang(req).get("signup_invite_deleted"));
		return ajaxAwareRedirect(req, resp, "/settings/admin/invites");
	}

	public static Object signupRequests(Request req, Response resp, Account self, ApplicationContext ctx){
		return new RenderedTemplateResponse("admin_signup_requests", req).paginate(ctx.getUsersController().getSignupInviteRequests(offset(req), 50)).pageTitle(lang(req).get("signup_requests_title"));
	}

	public static Object respondToSignupRequest(Request req, Response resp, Account self, ApplicationContext ctx){
		if(req.queryParams("accept")==null && req.queryParams("decline")==null)
			throw new BadRequestException();
		boolean accept=req.queryParams("accept")!=null;
		int id=safeParseInt(req.params(":id"));
		if(id<=0)
			throw new ObjectNotFoundException();

		if(accept){
			ctx.getUsersController().acceptSignupInviteRequest(req, self, id);
		}else{
			ctx.getUsersController().deleteSignupInviteRequest(id);
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp).setContent("signupReqBtns"+id,
					"<div class=\"settingsMessage\">"+lang(req).get(accept ? "email_invite_sent" : "signup_request_deleted")+"</div>");
		}
		resp.redirect(back(req));
		return "";
	}
}
