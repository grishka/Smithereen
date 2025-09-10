package smithereen.routes.admin;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.UserBanInfo;
import smithereen.model.UserBanStatus;
import smithereen.model.WebDeltaResponse;
import smithereen.model.admin.ActorStaffNote;
import smithereen.model.admin.GroupActionLogAction;
import smithereen.model.admin.GroupActionLogEntry;
import smithereen.model.admin.ViolationReport;
import smithereen.model.groups.GroupBanInfo;
import smithereen.model.groups.GroupBanStatus;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class AdminGroupsRoutes{
	private static Group getGroup(Request req){
		return context(req).getGroupsController().getGroupOrThrow(safeParseInt(req.params(":id")));
	}

	public static Object groupInfo(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		PaginatedList<GroupActionLogEntry> log=ctx.getModerationController().getGroupActionLog(group, 0, 5);
		HashSet<Integer> needUsers=new HashSet<>();
		for(GroupActionLogEntry e:log.list){
			needUsers.add(e.adminID());
			if(e.action()==GroupActionLogAction.CHANGE_MEMBER_ADMIN_LEVEL)
				needUsers.add(((Number) e.info().get("user")).intValue());
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_groups_info", req)
				.with("group", group)
				.with("staffNoteCount", ctx.getModerationController().getActorStaffNoteCount(group))
				.with("actionLog", log)
				.with("users", ctx.getUsersController().getUsers(needUsers))
				.pageTitle(group.name)
				.headerBack(group);
		if(group.banInfo!=null){
			if(group.domain==null && (group.banStatus==GroupBanStatus.SUSPENDED || group.banStatus==GroupBanStatus.SELF_DEACTIVATED)){
				model.with("groupDeletionTime", group.banInfo.bannedAt().plus(UserBanInfo.ACCOUNT_DELETION_DAYS, ChronoUnit.DAYS));
			}
			if(group.banInfo.moderatorID()!=0){
				try{
					model.with("banModerator", ctx.getUsersController().getUserOrThrow(group.banInfo.moderatorID()));
				}catch(ObjectNotFoundException ignore){}
			}
		}
		return model;
	}

	public static Object ajaxGroupActionLog(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		String paginationID=req.queryParams("pagination");
		if(!isAjax(req) || StringUtils.isEmpty(paginationID)){
			resp.redirect("/groups/"+group.id+"/groupinfo");
			return "";
		}
		PaginatedList<GroupActionLogEntry> log=ctx.getModerationController().getGroupActionLog(group, offset(req), 50);
		HashSet<Integer> needUsers=new HashSet<>();
		for(GroupActionLogEntry e:log.list){
			needUsers.add(e.adminID());
			if(e.action()==GroupActionLogAction.CHANGE_MEMBER_ADMIN_LEVEL)
				needUsers.add(((Number) e.info().get("user")).intValue());
		}
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_groups_info_log", req)
				.paginate(log)
				.with("users", ctx.getUsersController().getUsers(needUsers));
		WebDeltaResponse r=new WebDeltaResponse(resp)
				.insertHTML(WebDeltaResponse.ElementInsertionMode.BEFORE_BEGIN, "ajaxPagination_"+paginationID, model.renderToString());
		if(log.offset+log.perPage>=log.total){
			r.remove("ajaxPagination_"+paginationID);
		}else{
			r.setAttribute("ajaxPaginationLink_"+paginationID, "href", req.pathInfo()+"?offset="+(log.offset+log.perPage))
					.setContent("ajaxPaginationLink_"+paginationID, lang(req).get("admin_show_X_more_group_log", Map.of("count", log.total-log.offset-log.list.size())));
		}
		return r;
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

	public static Object banGroupForm(Request req, Response resp, Account self, ApplicationContext ctx){
		ViolationReport report;
		boolean deleteReportContent;
		if(req.queryParams("report")!=null){
			report=ctx.getModerationController().getViolationReportByID(safeParseInt(req.queryParams("report")), false);
			deleteReportContent=req.queryParams("deleteContent")!=null;
		}else{
			report=null;
			deleteReportContent=false;
		}
		Group group=getGroup(req);
		Lang l=lang(req);
		String formAction="/groups/"+group.id+"/ban";
		if(report!=null){
			formAction+="?report="+report.id;
			if(deleteReportContent)
				formAction+="&deleteContent";
		}
		Object form=wrapForm(req, resp, "admin_groups_ban_form", formAction, l.get("admin_ban_group_title"),
				"save", "banUser", List.of("status", "message"), s->switch(s){
					case "status" -> group.banStatus;
					case "message" -> group.banInfo!=null ? group.banInfo.message() : null;
					default -> throw new IllegalStateException("Unexpected value: " + s);
				}, null, Map.of("group", group, "hideNone", report!=null, "deleteReportContent", deleteReportContent, "numDaysUntilDeletion", GroupBanInfo.GROUP_DELETION_DAYS));
		if(group.domain==null && form instanceof WebDeltaResponse wdr){
			wdr.runScript("""
					function groupBanForm_updateFieldVisibility(){
						var message=ge("formRow_message");
						var suspendChecked=ge("status1").checked;
						if(suspendChecked){
							message.show();
						}else{
							message.hide();
						}
					}
					for(var i=0;i<3;i++){
						var el=ge("status"+i);
						if(!el) continue;
						el.addEventListener("change", function(){groupBanForm_updateFieldVisibility();}, false);
					}
					groupBanForm_updateFieldVisibility();""");
		}
		return form;
	}

	public static Object banGroup(Request req, Response resp, Account self, ApplicationContext ctx){
		ViolationReport report;
		boolean deleteReportContent;
		if(req.queryParams("report")!=null){
			report=ctx.getModerationController().getViolationReportByID(safeParseInt(req.queryParams("report")), false);
			deleteReportContent=req.queryParams("deleteContent")!=null;
			if(deleteReportContent){
				if(!"on".equals(req.queryParams("confirmReportContentDeletion"))){
					throw new UserErrorException("Report content deletion not confirmed");
				}
			}
		}else{
			report=null;
			deleteReportContent=false;
		}
		Group group=getGroup(req);
		GroupBanStatus status=enumValue(req.queryParams("status"), GroupBanStatus.class);
		GroupBanInfo info;
		if(status!=GroupBanStatus.NONE){
			String message=null;
			if(status==GroupBanStatus.SUSPENDED){
				message=req.queryParams("message");
			}
			info=new GroupBanInfo(Instant.now(), message, self.user.id, report==null ? 0 : report.id, group.banInfo!=null && group.banInfo.suspendedOnRemoteServer());
		}else{
			if(report!=null)
				throw new BadRequestException();
			if(group.banInfo!=null && group.banInfo.suspendedOnRemoteServer())
				info=new GroupBanInfo(Instant.now(), null, 0, 0, true);
			else
				info=null;
		}
		ctx.getModerationController().setGroupBanStatus(self.user, group, status, info);
		if(report!=null){
			if(deleteReportContent){
				ctx.getModerationController().deleteViolationReportContent(report, Objects.requireNonNull(sessionInfo(req)), false);
			}
			ctx.getModerationController().resolveViolationReport(report, self.user, status, info);
		}
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object deleteGroupImmediatelyForm(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		if(group instanceof ForeignGroup || (group.banStatus!=GroupBanStatus.SELF_DEACTIVATED && group.banStatus!=GroupBanStatus.SUSPENDED))
			throw new BadRequestException();
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_delete_group_form", req)
				.with("group", group)
				.with("username", group.username+"@"+Config.domain);
		return wrapForm(req, resp, "admin_delete_group_form", "/groups/"+group.id+"/deleteImmediately", lang(req).get("admin_group_delete_title"), "delete", model);
	}

	public static Object deleteGroupImmediately(Request req, Response resp, Account self, ApplicationContext ctx){
		Group group=getGroup(req);
		if(group instanceof ForeignGroup || (group.banStatus!=GroupBanStatus.SELF_DEACTIVATED && group.banStatus!=GroupBanStatus.SUSPENDED))
			throw new BadRequestException();
		String usernameCheck=group.username+"@"+Config.domain;
		if(!usernameCheck.equalsIgnoreCase(req.queryParams("username"))){
			String msg=lang(req).get("admin_group_delete_wrong_username");
			if(isAjax(req))
				return new WebDeltaResponse(resp)
						.keepBox()
						.show("formMessage_deleteGroup")
						.setContent("formMessage_deleteGroup", msg);
			RenderedTemplateResponse model=new RenderedTemplateResponse("admin_delete_group_form", req)
					.with("group", group)
					.with("username", group.username+"@"+Config.domain)
					.with("message", msg);
			return wrapForm(req, resp, "admin_delete_group_form", "/groups/"+group.id+"/deleteImmediately", lang(req).get("admin_group_delete_title"), "delete", model);
		}
		ctx.getGroupsController().deleteLocalGroup(self.user, group);
		//req.session().attribute("adminSettingsUsersMessage", lang(req).get("admin_group_deleted_successfully"));
		if(isAjax(req))
			return new WebDeltaResponse(resp).replaceLocation(/*"/settings/admin/users"*/"/my/groups");
		resp.redirect(/*"/settings/admin/users"*/"/my/groups");
		return "";
	}
}
