package smithereen.routes.admin;

import java.net.URLEncoder;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.SmithereenApplication;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.ForeignUser;
import smithereen.model.OtherSession;
import smithereen.model.PaginatedList;
import smithereen.model.SessionInfo;
import smithereen.model.User;
import smithereen.model.UserBanInfo;
import smithereen.model.UserBanStatus;
import smithereen.model.WebDeltaResponse;
import smithereen.model.admin.ActorStaffNote;
import smithereen.model.admin.AuditLogEntry;
import smithereen.model.admin.UserRole;
import smithereen.model.admin.ViolationReport;
import smithereen.model.photos.Photo;
import smithereen.model.viewmodel.AdminUserViewModel;
import smithereen.model.viewmodel.CommentViewModel;
import smithereen.model.viewmodel.PostViewModel;
import smithereen.model.viewmodel.UserContentMetrics;
import smithereen.model.viewmodel.UserRelationshipMetrics;
import smithereen.storage.ModerationStorage;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class AdminUsersRoutes{
	public static Object users(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_users", req);
		Lang l=lang(req);
		String q=req.queryParams("q");
		Boolean localOnly=switch(req.queryParams("location")){
			case "local" -> true;
			case "remote" -> false;
			case null, default -> null;
		};
		String emailDomain=req.queryParams("emailDomain");
		String lastIP=req.queryParams("lastIP");
		int role=safeParseInt(req.queryParams("role"));
		String banStatusParam=req.queryParams("banStatus");
		UserBanStatus banStatus=enumValueOpt(banStatusParam, UserBanStatus.class);
		PaginatedList<AdminUserViewModel> items=ctx.getModerationController().getAllUsers(offset(req), 100, q, localOnly, emailDomain, lastIP, role, banStatus, "REMOTE_SUSPENDED".equals(banStatusParam));
		model.paginate(items);
		model.with("users", ctx.getUsersController().getUsers(items.list.stream().map(AdminUserViewModel::userID).collect(Collectors.toSet())));
		model.with("accounts", ctx.getModerationController().getAccounts(items.list.stream().map(AdminUserViewModel::accountID).filter(i->i>0).collect(Collectors.toSet())));
		model.with("title", l.get("admin_users")+" | "+l.get("menu_admin")).with("toolbarTitle", l.get("menu_admin"));
		model.with("allRoles", Config.userRoles.values().stream().sorted(Comparator.comparingInt(UserRole::id)).toList());
		model.with("rolesMap", Config.userRoles);
		String baseURL=getRequestPathAndQuery(req);
		model.with("urlPath", baseURL)
				.with("location", req.queryParams("location"))
				.with("emailDomain", emailDomain)
				.with("lastIP", lastIP)
				.with("roleID", role)
				.with("banStatus", banStatusParam)
				.with("query", q)
				.with("hasFilters", StringUtils.isNotEmpty(q) || localOnly!=null || StringUtils.isNotEmpty(emailDomain) || StringUtils.isNotEmpty(lastIP) || role>0);
		jsLangKey(req, "cancel", "yes", "no");
		String msg=req.session().attribute("adminSettingsUsersMessage");
		if(msg!=null){
			req.session().removeAttribute("adminSettingsUsersMessage");
			model.with("message", msg);
		}
		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.setContent("ajaxUpdatable", model.renderBlock("ajaxPartialUpdate"))
					.setAttribute("userSearch", "data-base-url", baseURL)
					.setURL(baseURL);
		}
		return model;
	}

	public static Object roleForm(Request req, Response resp, Account self, ApplicationContext ctx){
		Lang l=lang(req);
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=ctx.getUsersController().getAccountOrThrow(accountID);
		if(target.id==self.id)
			return wrapError(req, resp, "err_user_not_found");
		UserRole myRole=sessionInfo(req).permissions.role;
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_users_role", req);
		model.with("targetAccount", target);
		model.with("roles", Config.userRoles.values().stream()
				.filter(r->myRole.permissions().contains(UserRole.Permission.SUPERUSER) || r.permissions().containsAll(myRole.permissions()))
				.sorted(Comparator.comparingInt(UserRole::id))
				.toList());
		return wrapForm(req, resp, "admin_users_role", "/settings/admin/users/setRole", l.get("role"), "save", model);
	}

	public static Object setUserRole(Request req, Response resp, Account self, ApplicationContext ctx){
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		int role=safeParseInt(req.queryParams("role"));
		Account target=ctx.getUsersController().getAccountOrThrow(accountID);
		if(target.id==self.id)
			return wrapError(req, resp, "err_user_not_found");
		ctx.getModerationController().setAccountRole(self, target, role);
		if(isAjax(req)){
			resp.type("application/json");
			return "[]";
		}
		return "";
	}

	public static Object confirmActivateAccount(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		req.attribute("noHistory", true);
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null)
			throw new ObjectNotFoundException("err_user_not_found");
		Lang l=lang(req);
		String back=back(req);
		User user=target.user;
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("admin_activate_X_confirm", Map.of("name", user.getFirstLastAndGender()))).with("formAction", "/settings/admin/users/activate?accountID="+accountID+"&_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object activateAccount(Request req, Response resp, Account self, ApplicationContext ctx){
		try{
			int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
			Account target=UserStorage.getAccount(accountID);
			if(target==null)
				throw new ObjectNotFoundException("err_user_not_found");
			if(target.activationInfo==null || target.activationInfo.emailState!=Account.ActivationInfo.EmailConfirmationState.NOT_CONFIRMED){
				if(!isAjax(req))
					resp.redirect(back(req));
				return "";
			}
			ModerationStorage.createAuditLogEntry(self.user.id, AuditLogEntry.Action.ACTIVATE_ACCOUNT, target.user.id, 0, null, null);
			SessionStorage.updateActivationInfo(accountID, null);
			UserStorage.removeAccountFromCache(accountID);
			SmithereenApplication.invalidateAllSessionsForAccount(accountID);
			if(isAjax(req))
				return new WebDeltaResponse(resp).refresh();
			resp.redirect(back(req));
			return "";
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public static Object userInfo(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_users_info", req);
		model.with("user", user);
		Account account;
		if(!(user instanceof ForeignUser)){
			account=ctx.getUsersController().getAccountForUser(user);
			model.with("account", account);
			if(account.roleID>0){
				UserRole role=Config.userRoles.get(account.roleID);
				String roleKey=role.getLangKey();
				model.with("roleTitle", StringUtils.isNotEmpty(roleKey) ? lang(req).get(roleKey) : role.name());
			}
			if(account.inviterAccountID>0){
				try{
					model.with("inviter", ctx.getUsersController().getAccountOrThrow(account.inviterAccountID).user);
				}catch(ObjectNotFoundException ignore){}
			}
			model.with("sessions", ctx.getUsersController().getAccountSessions(account));
			if(user.banInfo!=null){
				if(user.domain==null && (user.banStatus==UserBanStatus.SUSPENDED || user.banStatus==UserBanStatus.SELF_DEACTIVATED)){
					model.with("accountDeletionTime", user.banInfo.bannedAt().plus(UserBanInfo.ACCOUNT_DELETION_DAYS, ChronoUnit.DAYS));
				}
				try{
					model.with("banModerator", ctx.getUsersController().getUserOrThrow(user.banInfo.moderatorID()));
				}catch(ObjectNotFoundException ignore){}
			}
		}else{
			account=null;
		}
		UserRelationshipMetrics relMetrics=ctx.getUsersController().getRelationshipMetrics(user);
		UserContentMetrics contentMetrics=ctx.getUsersController().getContentMetrics(user);
		model.with("relationshipMetrics", relMetrics).with("contentMetrics", contentMetrics);
		model.pageTitle(lang(req).get("admin_manage_user")+" | "+user.getFullName())
				.headerBack(user);
		model.with("staffNoteCount", ctx.getModerationController().getActorStaffNoteCount(user));
		return model;
	}

	public static Object changeUserEmailForm(Request req, Response resp, Account self, ApplicationContext ctx){
		Account target=ctx.getUsersController().getAccountOrThrow(safeParseInt(req.queryParams("accountID")));
		RenderedTemplateResponse model=new RenderedTemplateResponse("change_email_form", req);
		model.with("email", target.email);
		return wrapForm(req, resp, "change_email_form", "/settings/admin/users/changeEmail?accountID="+target.id, lang(req).get("change_email_title"), "save", model);
	}

	public static Object changeUserEmail(Request req, Response resp, Account self, ApplicationContext ctx){
		Account target=ctx.getUsersController().getAccountOrThrow(safeParseInt(req.queryParams("accountID")));
		String email=req.queryParams("email");
		if(!isValidEmail(email))
			throw new BadRequestException();
		ctx.getModerationController().setUserEmail(self.user, target, email);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object endUserSession(Request req, Response resp, Account self, ApplicationContext ctx){
		Account target=ctx.getUsersController().getAccountOrThrow(safeParseInt(req.queryParams("accountID")));
		int sessionID=safeParseInt(req.queryParams("sessionID"));
		List<OtherSession> sessions=ctx.getUsersController().getAccountSessions(target);
		OtherSession sessionToRevoke=null;
		for(OtherSession session:sessions){
			if(session.id()==sessionID){
				sessionToRevoke=session;
				break;
			}
		}
		if(sessionToRevoke==null)
			throw new ObjectNotFoundException();

		ctx.getModerationController().terminateUserSession(self.user, target, sessionToRevoke);

		if(isAjax(req)){
			return new WebDeltaResponse(resp)
					.addClass("adminSessionRow"+sessionID, "transparent")
					.addClass("adminSessionRow"+sessionID, "disabled")
					.showSnackbar(lang(req).get("admin_session_terminated"))
					.hide("boxLoader");
		}
		resp.redirect(back(req));
		return "";
	}

	public static Object banUserForm(Request req, Response resp, Account self, ApplicationContext ctx){
		ViolationReport report;
		boolean deleteReportContent;
		if(req.queryParams("report")!=null){
			report=ctx.getModerationController().getViolationReportByID(safeParseInt(req.queryParams("report")), false);
			deleteReportContent=req.queryParams("deleteContent")!=null;
		}else{
			report=null;
			deleteReportContent=false;
		}
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		Lang l=lang(req);
		String formAction="/users/"+user.id+"/ban";
		if(report!=null){
			formAction+="?report="+report.id;
			if(deleteReportContent)
				formAction+="&deleteContent";
		}
		Object form=wrapForm(req, resp, "admin_users_ban_form", formAction, l.get("admin_ban_user_title"),
				"save", "banUser", List.of("status", "message", /*"duration",*/ "forcePasswordChange"), s->switch(s){
					case "status" -> user.banStatus;
					case "message" -> user.banInfo!=null ? user.banInfo.message() : null;
					case "forcePasswordChange" -> user.banInfo!=null && user.banInfo.requirePasswordChange();
					default -> throw new IllegalStateException("Unexpected value: " + s);
				}, null, Map.of("user", user, "hideNone", report!=null, "deleteReportContent", deleteReportContent, "numDaysUntilDeletion", UserBanInfo.ACCOUNT_DELETION_DAYS));
		if(user.domain==null && form instanceof WebDeltaResponse wdr){
			wdr.runScript("""
					function userBanForm_updateFieldVisibility(){
						var message=ge("formRow_message");
						var duration=ge("formRow_duration");
						var forcePasswordChange=ge("formRow_forcePasswordChange");
						var freezeChecked=ge("status1").checked;
						var suspendChecked=ge("status2").checked;
						if(freezeChecked || suspendChecked){
							message.show();
						}else{
							message.hide();
						}
						if(freezeChecked){
							duration.show();
							forcePasswordChange.show();
						}else{
							duration.hide();
							forcePasswordChange.hide();
						}
					}
					for(var i=0;i<4;i++){
						var el=ge("status"+i);
						if(!el) continue;
						el.addEventListener("change", function(){userBanForm_updateFieldVisibility();}, false);
					}
					userBanForm_updateFieldVisibility();""");
		}
		return form;
	}

	public static Object banUser(Request req, Response resp, Account self, ApplicationContext ctx){
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
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		UserBanStatus status=enumValue(req.queryParams("status"), UserBanStatus.class);
		UserBanInfo info;
		if(status!=UserBanStatus.NONE){
			String message=null;
			Instant expiresAt=null;
			boolean forcePasswordChange=false;
			if(status==UserBanStatus.FROZEN || status==UserBanStatus.SUSPENDED){
				message=req.queryParams("message");
			}
			if(status==UserBanStatus.FROZEN){
				int duration=safeParseInt(req.queryParams("duration"));
				if(duration!=0){
					expiresAt=Instant.now().plus(duration, ChronoUnit.HOURS);
				}
				forcePasswordChange="on".equals(req.queryParams("forcePasswordChange"));
			}
			info=new UserBanInfo(Instant.now(), expiresAt, message, forcePasswordChange, self.user.id, report==null ? 0 : report.id, user.banInfo!=null && user.banInfo.suspendedOnRemoteServer());
		}else{
			if(report!=null)
				throw new BadRequestException();
			if(user.banInfo!=null && user.banInfo.suspendedOnRemoteServer())
				info=new UserBanInfo(Instant.now(), null, null, false, 0, 0, true);
			else
				info=null;
		}
		ctx.getModerationController().setUserBanStatus(self.user, user, user instanceof ForeignUser ? null : ctx.getUsersController().getAccountForUser(user), status, info);
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

	public static Object deleteAccountImmediatelyForm(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		if(user instanceof ForeignUser || (user.banStatus!=UserBanStatus.SELF_DEACTIVATED && user.banStatus!=UserBanStatus.SUSPENDED))
			throw new BadRequestException();
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_delete_user_form", req)
				.with("user", user)
				.with("username", user.username+"@"+Config.domain);
		return wrapForm(req, resp, "admin_delete_user_form", "/users/"+user.id+"/deleteImmediately", lang(req).get("admin_user_delete_account_title"), "delete", model);
	}

	public static Object deleteAccountImmediately(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		if(user instanceof ForeignUser || (user.banStatus!=UserBanStatus.SELF_DEACTIVATED && user.banStatus!=UserBanStatus.SUSPENDED))
			throw new BadRequestException();
		String usernameCheck=user.username+"@"+Config.domain;
		if(!usernameCheck.equalsIgnoreCase(req.queryParams("username"))){
			String msg=lang(req).get("admin_user_delete_wrong_username");
			if(isAjax(req))
				return new WebDeltaResponse(resp)
						.keepBox()
						.show("formMessage_deleteUser")
						.setContent("formMessage_deleteUser", msg);
			RenderedTemplateResponse model=new RenderedTemplateResponse("admin_delete_user_form", req)
					.with("user", user)
					.with("username", user.username+"@"+Config.domain)
					.with("message", msg);
			return wrapForm(req, resp, "admin_delete_user_form", "/users/"+user.id+"/deleteImmediately", lang(req).get("admin_user_delete_account_title"), "delete", model);
		}
		ctx.getUsersController().deleteLocalUser(self.user, user);
		req.session().attribute("adminSettingsUsersMessage", lang(req).get("admin_user_deleted_successfully"));
		if(isAjax(req))
			return new WebDeltaResponse(resp).replaceLocation("/settings/admin/users");
		resp.redirect("/settings/admin/users");
		return "";
	}

	public static Object userStaffNotes(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_users_notes", req);
		PaginatedList<ActorStaffNote> notes=ctx.getModerationController().getActorStaffNotes(user, offset(req), 50);
		model.paginate(notes);
		model.with("users", ctx.getUsersController().getUsers(notes.list.stream().map(ActorStaffNote::authorID).collect(Collectors.toSet())));
		model.with("user", user).with("staffNoteCount", notes.total).headerBack(user);
		return model;
	}

	public static Object userStaffNoteAdd(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		requireQueryParams(req, "text");
		String text=req.queryParams("text");
		ctx.getModerationController().createActorStaffNote(self.user, user, text);
		if(isAjax(req))
			return new WebDeltaResponse(resp).setContent("commentText", "").refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object userStaffNoteConfirmDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		int userID=safeParseInt(req.params(":id"));
		int noteID=safeParseInt(req.params(":noteID"));
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("delete"), l.get("admin_user_staff_note_confirm_delete"), "/users/"+userID+"/staffNotes/"+noteID+"/delete");
	}

	public static Object userStaffNoteDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		int noteID=safeParseInt(req.params(":noteID"));
		ctx.getModerationController().deleteActorStaffNote(user, ctx.getModerationController().getActorStaffNoteOrThrow(user, noteID));
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object userContent(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_user_content", req)
				.with("user", user);

		Lang l=lang(req);
		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();

		String rawType=req.queryParamOrDefault("type", "wall");
		String type;
		String countKey;
		if("photos".equals(rawType)){
			type="photos";
			PaginatedList<Photo> photos=ctx.getPhotosController().getAllPhotosByAuthor(user, offset(req), 50);
			HashSet<Long> needAlbums=new HashSet<>();
			for(Photo p:photos.list){
				if(p.ownerID>0)
					needUsers.add(p.ownerID);
				else
					needGroups.add(-p.ownerID);
				needAlbums.add(p.albumID);
			}
			model.paginate(photos);
			model.with("summary", l.get("content_type_X_photos", Map.of("count", photos.total)));
			countKey="content_type_X_photos";
			model.with("albums", ctx.getPhotosController().getAlbumsIgnoringPrivacy(needAlbums));
		}else if("comments".equals(rawType)){
			type="comments";
			PaginatedList<CommentViewModel> comments=ctx.getCommentsController().getAllCommentsByAuthor(user, offset(req), 50);
			CommentViewModel.collectUserIDs(comments.list, needUsers);
			for(CommentViewModel cvm:comments.list){
				if(cvm.post.ownerID>0)
					needUsers.add(cvm.post.ownerID);
				else
					needGroups.add(-cvm.post.ownerID);
			}
			model.paginate(comments);
			model.with("summary", l.get("X_comments", Map.of("count", comments.total)));
			countKey="X_comments";
		}else{
			type="wall";
			PaginatedList<PostViewModel> posts=ctx.getWallController().getAllPostsByAuthor(user, offset(req), 50);
			ctx.getWallController().populateReposts(user, posts.list, 2);
			PostViewModel.collectActorIDs(posts.list, needUsers, needGroups);
			model.paginate(posts);
			model.with("summary", l.get("X_posts", Map.of("count", posts.total)));
			countKey="X_posts";
		}

		model.with("countKey", countKey);
		jsLangKey(req, countKey);
		model.with("contentType", type);
		model.pageTitle(l.get("admin_report_content")+" | "+user.getFullName())
				.headerBack(user);
		model.with("users", ctx.getUsersController().getUsers(needUsers))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(needGroups));
		model.with("staffNoteCount", ctx.getModerationController().getActorStaffNoteCount(user));

		int reportID=safeParseInt(req.queryParams("report"));
		if(reportID>0)
			model.with("reportID", reportID);

		return model;
	}
}
