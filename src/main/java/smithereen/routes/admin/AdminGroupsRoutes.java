package smithereen.routes.admin;

import smithereen.ApplicationContext;
import smithereen.model.Account;
import smithereen.model.Group;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;

import static smithereen.Utils.*;

public class AdminGroupsRoutes{
	private static Group getGroup(Request req){
		return context(req).getGroupsController().getGroupOrThrow(safeParseInt(req.params(":id")));
	}

	public static Object groupInfo(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_groups_info", req)
				.with("group", group)
				.headerBack(group);
		return model;
	}
}
