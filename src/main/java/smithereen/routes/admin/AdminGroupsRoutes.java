package smithereen.routes.admin;

import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.WebDeltaResponse;
import smithereen.model.admin.ActorStaffNote;
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
				.with("staffNoteCount", ctx.getModerationController().getActorStaffNoteCount(group))
				.headerBack(group);
		return model;
	}

	public static Object groupStaffNotes(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_groups_notes", req);
		PaginatedList<ActorStaffNote> notes=ctx.getModerationController().getActorStaffNotes(group, offset(req), 50);
		model.paginate(notes);
		model.with("users", ctx.getUsersController().getUsers(notes.list.stream().map(ActorStaffNote::authorID).collect(Collectors.toSet())));
		model.with("group", group).with("staffNoteCount", notes.total).headerBack(group);
		return model;
	}

	public static Object groupStaffNoteAdd(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		requireQueryParams(req, "text");
		String text=req.queryParams("text");
		ctx.getModerationController().createActorStaffNote(self.user, group, text);
		if(isAjax(req))
			return new WebDeltaResponse(resp).setContent("commentText", "").refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object groupStaffNoteConfirmDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		int groupID=safeParseInt(req.params(":id"));
		int noteID=safeParseInt(req.params(":noteID"));
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("delete"), l.get("admin_group_staff_note_confirm_delete"), "/groups/"+groupID+"/staffNotes/"+noteID+"/delete");
	}

	public static Object groupStaffNoteDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		int noteID=safeParseInt(req.params(":noteID"));
		ctx.getModerationController().deleteActorStaffNote(group, ctx.getModerationController().getActorStaffNoteOrThrow(group, noteID));
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}
}
