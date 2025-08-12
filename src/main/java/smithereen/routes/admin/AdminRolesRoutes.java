package smithereen.routes.admin;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.SessionInfo;
import smithereen.model.UserBanInfo;
import smithereen.model.WebDeltaResponse;
import smithereen.model.admin.UserRole;
import smithereen.model.viewmodel.UserRoleViewModel;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class AdminRolesRoutes{
	public static Object editRole(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		UserRole role=Config.userRoles.get(safeParseInt(req.params(":id")));
		if(role==null)
			throw new ObjectNotFoundException();
		UserRole myRole=info.permissions.role;
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_edit_role", req);
		model.pageTitle(lang(req).get("admin_edit_role_title"));
		model.with("role", role);
		if(role.id()==1){
			model.with("permissions", List.of(UserRole.Permission.SUPERUSER));
			model.with("disabledPermissions", EnumSet.of(UserRole.Permission.SUPERUSER));
		}else{
			model.with("permissions", Arrays.stream(UserRole.Permission.values()).filter(p->p!=UserRole.Permission.VISIBLE_IN_STAFF && (myRole.permissions().contains(UserRole.Permission.SUPERUSER) || myRole.permissions().contains(p))).toList());
			if(role.id()==myRole.id())
				model.with("disabledPermissions", EnumSet.allOf(UserRole.Permission.class));
			else
				model.with("disabledPermissions", EnumSet.noneOf(UserRole.Permission.class));
		}
		model.with("numDaysUntilDeletion", UserBanInfo.ACCOUNT_DELETION_DAYS);
		if(info.permissions.hasPermission(UserRole.Permission.VISIBLE_IN_STAFF))
			model.with("settings", List.of(UserRole.Permission.VISIBLE_IN_STAFF));
		return model;
	}

	public static Object saveRole(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		String _id=req.params(":id");
		UserRole role;
		if(StringUtils.isNotEmpty(_id)){
			role=Config.userRoles.get(safeParseInt(_id));
			if(role==null)
				throw new ObjectNotFoundException();
		}else{
			role=null;
		}
		requireQueryParams(req, "name");
		String name=req.queryParams("name");
		EnumSet<UserRole.Permission> permissions=EnumSet.noneOf(UserRole.Permission.class);
		for(UserRole.Permission permission:UserRole.Permission.values()){
			if("on".equals(req.queryParams(permission.toString())))
				permissions.add(permission);
		}
		if(permissions.isEmpty()){
			if(isAjax(req)){
				return new WebDeltaResponse(resp).show("formMessage_editRole").setContent("formMessage_editRole", lang(req).get("admin_no_permissions_selected"));
			}
			resp.redirect(back(req));
			return "";
		}
		if(role!=null){
			ctx.getModerationController().updateRole(info.account.user, info.permissions, role, name, permissions);
			req.session().attribute("adminRolesMessage", lang(req).get("admin_role_X_saved", Map.of("name", name)));
		}else{
			ctx.getModerationController().createRole(info.account.user, info.permissions, name, permissions);
			req.session().attribute("adminRolesMessage", lang(req).get("admin_role_X_created", Map.of("name", name)));
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp).replaceLocation("/settings/admin/roles");
		}
		resp.redirect("/settings/admin/roles");
		return "";
	}

	public static Object createRoleForm(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		UserRole myRole=info.permissions.role;
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_edit_role", req);
		model.pageTitle(lang(req).get("admin_create_role_title"));
		model.with("permissions", Arrays.stream(UserRole.Permission.values()).filter(p->p!=UserRole.Permission.VISIBLE_IN_STAFF && (myRole.permissions().contains(UserRole.Permission.SUPERUSER) || myRole.permissions().contains(p))).toList());
		model.with("disabledPermissions", EnumSet.noneOf(UserRole.Permission.class));
		model.with("numDaysUntilDeletion", UserBanInfo.ACCOUNT_DELETION_DAYS);
		if(info.permissions.hasPermission(UserRole.Permission.VISIBLE_IN_STAFF))
			model.with("settings", List.of(UserRole.Permission.VISIBLE_IN_STAFF));
		return model;
	}

	public static Object deleteRole(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		UserRole role=Config.userRoles.get(safeParseInt(req.params(":id")));
		if(role==null)
			throw new ObjectNotFoundException();
		ctx.getModerationController().deleteRole(info.account.user, info.permissions, role);
		if(isAjax(req)){
			return new WebDeltaResponse(resp).remove("roleRow"+role.id());
		}
		resp.redirect("/settings/admin/roles");
		return "";
	}

	public static Object roles(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		jsLangKey(req, "admin_delete_role", "admin_delete_role_confirm", "yes", "no");
		List<UserRoleViewModel> roles=ctx.getModerationController().getRoles(info.permissions);
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_roles", req)
				.pageTitle(lang(req).get("admin_roles"))
				.with("toolbarTitle", lang(req).get("menu_admin"))
				.with("roles", roles)
				.with("unconfirmedFaspRequests", ctx.getFaspController().getUnconfirmedProviderCount());
		String msg=req.session().attribute("adminRolesMessage");
		if(StringUtils.isNotEmpty(msg)){
			req.session().removeAttribute("adminRolesMessage");
			model.with("message", msg);
		}
		return model;
	}
}
