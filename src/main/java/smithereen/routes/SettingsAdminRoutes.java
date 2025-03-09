package smithereen.routes;

import java.net.URLEncoder;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Mailer;
import smithereen.SmithereenApplication;
import smithereen.activitypub.objects.Actor;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.ActorStaffNote;
import smithereen.model.AuditLogEntry;
import smithereen.model.EmailDomainBlockRule;
import smithereen.model.EmailDomainBlockRuleFull;
import smithereen.model.FederationRestriction;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.IPBlockRule;
import smithereen.model.IPBlockRuleFull;
import smithereen.model.MailMessage;
import smithereen.model.OtherSession;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.Server;
import smithereen.model.SessionInfo;
import smithereen.model.SignupInvitation;
import smithereen.model.StatsPoint;
import smithereen.model.StatsType;
import smithereen.model.User;
import smithereen.model.UserBanInfo;
import smithereen.model.UserBanStatus;
import smithereen.model.UserRole;
import smithereen.model.ViolationReport;
import smithereen.model.ViolationReportAction;
import smithereen.model.WebDeltaResponse;
import smithereen.model.comments.Comment;
import smithereen.model.photos.Photo;
import smithereen.model.reports.ReportableContentObject;
import smithereen.model.viewmodel.AdminUserViewModel;
import smithereen.model.viewmodel.AuditLogEntryViewModel;
import smithereen.model.viewmodel.UserContentMetrics;
import smithereen.model.viewmodel.UserRelationshipMetrics;
import smithereen.model.viewmodel.UserRoleViewModel;
import smithereen.model.viewmodel.ViolationReportActionViewModel;
import smithereen.storage.ModerationStorage;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.text.TextProcessor;
import smithereen.util.InetAddressRange;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class SettingsAdminRoutes{
	public static Object index(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_server_info", req);
		Lang l=lang(req);
		model.with("title", l.get("profile_edit_basic")+" | "+l.get("menu_admin")).with("toolbarTitle", l.get("menu_admin"));
		model.with("serverName", Config.getServerDisplayName())
				.with("serverDescription", Config.serverDescription)
				.with("serverShortDescription", Config.serverShortDescription)
				.with("serverPolicy", Config.serverPolicy)
				.with("serverAdminEmail", Config.serverAdminEmail)
				.with("signupMode", Config.signupMode)
				.with("signupConfirmEmail", Config.signupConfirmEmail)
				.with("signupEnableCaptcha", Config.signupFormUseCaptcha);
		String msg=req.session().attribute("admin.serverInfoMessage");
		if(StringUtils.isNotEmpty(msg)){
			req.session().removeAttribute("admin.serverInfoMessage");
			model.with("adminServerInfoMessage", msg);
		}
		return model;
	}

	public static Object updateServerInfo(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		String name=req.queryParams("server_name");
		String descr=req.queryParams("server_description");
		String shortDescr=req.queryParams("server_short_description");
		String policy=req.queryParams("server_policy");
		String email=req.queryParams("server_admin_email");
		boolean confirmEmail="on".equals(req.queryParams("signup_confirm_email"));
		boolean signupCaptcha="on".equals(req.queryParams("signup_enable_captcha"));

		Config.serverDisplayName=name;
		Config.serverDescription=descr;
		Config.serverShortDescription=shortDescr;
		Config.serverPolicy=policy;
		Config.serverAdminEmail=email;
		Config.signupConfirmEmail=confirmEmail;
		Config.signupFormUseCaptcha=signupCaptcha;
		Config.updateInDatabase(Map.of(
				"ServerDisplayName", name,
				"ServerDescription", descr,
				"ServerShortDescription", shortDescr,
				"ServerPolicy", policy,
				"ServerAdminEmail", email,
				"SignupConfirmEmail", confirmEmail ? "1" : "0",
				"SignupFormUseCaptcha", signupCaptcha ? "1" : "0"
		));
		try{
			Config.SignupMode signupMode=Config.SignupMode.valueOf(req.queryParams("signup_mode"));
			Config.signupMode=signupMode;
			Config.updateInDatabase("SignupMode", signupMode.toString());
		}catch(IllegalArgumentException ignore){}

		if(isAjax(req))
			return new WebDeltaResponse(resp).show("formMessage_adminServerInfo").setContent("formMessage_adminServerInfo", lang(req).get("admin_server_info_updated"));
		req.session().attribute("admin.serverInfoMessage", lang(req).get("admin_server_info_updated"));
		resp.redirect("/settings/admin");
		return "";
	}

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
		PaginatedList<AdminUserViewModel> items=ctx.getModerationController().getAllUsers(offset(req), 100, q, localOnly, emailDomain, lastIP, role);
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
//
//	public static Object banUser(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
//		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
//		Account target=UserStorage.getAccount(accountID);
//		if(target==null || target.id==self.id || target.roleID!=0)
//			throw new ObjectNotFoundException("err_user_not_found");
//		Account.BanInfo banInfo=new Account.BanInfo();
//		banInfo.reason=req.queryParams("message");
//		banInfo.adminUserId=self.user.id;
//		banInfo.when=Instant.now();
//		UserStorage.putAccountBanInfo(accountID, banInfo);
//		if(isAjax(req))
//			return new WebDeltaResponse(resp).refresh();
//		resp.redirect(back(req));
//		return "";
//	}
//
//	public static Object confirmUnbanUser(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
//		req.attribute("noHistory", true);
//		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
//		Account target=UserStorage.getAccount(accountID);
//		if(target==null)
//			throw new ObjectNotFoundException("err_user_not_found");
//		Lang l=Utils.lang(req);
//		String back=Utils.back(req);
//		User user=target.user;
//		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("admin_unban_X_confirm", Map.of("name", user.getFirstLastAndGender()))).with("formAction", "/settings/admin/users/unban?accountID="+accountID+"&_redir="+URLEncoder.encode(back)).with("back", back);
//	}
//
//	public static Object unbanUser(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
//		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
//		Account target=UserStorage.getAccount(accountID);
//		if(target==null)
//			throw new ObjectNotFoundException("err_user_not_found");
//		UserStorage.putAccountBanInfo(accountID, null);
//		if(isAjax(req))
//			return new WebDeltaResponse(resp).refresh();
//		resp.redirect(back(req));
//		return "";
//	}

	public static Object otherSettings(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		Lang l=lang(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_other_settings", req);
		model.with("title", l.get("admin_other")+" | "+l.get("menu_admin")).with("toolbarTitle", l.get("menu_admin"));
		model.with("from", Config.mailFrom)
				.with("smtpServer", Config.smtpServerAddress)
				.with("smtpPort", Config.smtpPort)
				.with("smtpUser", Config.smtpUsername)
				.with("smtpPassword", Config.smtpPassword)
				.with("smtpUseTLS", Config.smtpUseTLS);
		String msg=req.session().attribute("admin.emailTestMessage");
		if(StringUtils.isNotEmpty(msg)){
			req.session().removeAttribute("admin.emailTestMessage");
			model.with("adminEmailTestMessage", msg);
		}
		msg=req.session().attribute("admin.emailSettingsMessage");
		if(StringUtils.isNotEmpty(msg)){
			req.session().removeAttribute("admin.emailSettingsMessage");
			model.with("adminEmailSettingsMessage", msg);
		}
		return model;
	}

	public static Object saveEmailSettings(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		String from=req.queryParams("from");
		int smtpPort=parseIntOrDefault(req.queryParams("smtp_port"), 25);
		String smtpServer=req.queryParams("smtp_server");
		String smtpUser=req.queryParams("smtp_user");
		String smtpPassword=req.queryParams("smtp_password");
		boolean smtpUseTLS="on".equals(req.queryParams("smtp_use_tls"));

		if(smtpPort<1 || smtpPort>65535)
			smtpPort=25;

		String result;
		if(!isValidEmail(from)){
			result="err_invalid_email";
		}else{
			Config.mailFrom=from;
			Config.smtpPort=smtpPort;
			Config.smtpServerAddress=smtpServer;
			Config.smtpUsername=smtpUser;
			Config.smtpPassword=smtpPassword;
			Config.smtpUseTLS=smtpUseTLS;

			Config.updateInDatabase(Map.of(
					"MailFrom", from,
					"Mail_SMTP_ServerPort", smtpPort+"",
					"Mail_SMTP_ServerAddress", smtpServer,
					"Mail_SMTP_Username", smtpUser,
					"Mail_SMTP_Password", smtpPassword,
					"Mail_SMTP_UseTLS", smtpUseTLS ? "1" : "0"
			));

			Mailer.getInstance().updateSession();

			result="settings_saved";
		}

		if(isAjax(req))
			return new WebDeltaResponse(resp).show("formMessage_adminEmailSettings").setContent("formMessage_adminEmailSettings", lang(req).get(result));
		req.session().attribute("admin.emailSettingsMessage", lang(req).get(result));
		resp.redirect("/settings/admin/other");
		return "";
	}

	public static Object sendTestEmail(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		String to=req.queryParams("email");
		String result;
		if(isValidEmail(to)){
			Mailer.getInstance().sendTest(req, to, self);
			result="admin_email_test_sent";
		}else{
			result="err_invalid_email";
		}

		if(isAjax(req))
			return new WebDeltaResponse(resp).show("formMessage_adminEmailTest").setContent("formMessage_adminEmailTest", lang(req).get(result));
		req.session().attribute("admin.emailTestMessage", lang(req).get(result));
		resp.redirect("/settings/admin/other");
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

	public static Object reportsList(Request req, Response resp, Account self, ApplicationContext ctx){
		boolean resolved=req.queryParams("resolved")!=null;
		RenderedTemplateResponse model=new RenderedTemplateResponse("report_list", req);
		model.with("tab", resolved ? "resolved" : "open");
		model.pageTitle(lang(req).get("menu_reports"));
		PaginatedList<ViolationReport> reports=ctx.getModerationController().getViolationReports(!resolved, offset(req), 50);
		model.paginate(reports);

		Set<Integer> userIDs=reports.list.stream().filter(r->r.targetID>0).map(r->r.targetID).collect(Collectors.toSet());
		userIDs.addAll(reports.list.stream().filter(r->r.reporterID!=0).map(r->r.reporterID).collect(Collectors.toSet()));
		Set<Integer> groupIDs=reports.list.stream().filter(r->r.targetID<0).map(r->-r.targetID).collect(Collectors.toSet());

		model.with("users", ctx.getUsersController().getUsers(userIDs))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(groupIDs));

		return model;
	}

	public static Object viewReport(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, true);
		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
		HashSet<Integer> needPosts=new HashSet<>();
		HashSet<Long> needMessages=new HashSet<>(), needPhotos=new HashSet<>(), needComments=new HashSet<>();

		if(report.targetID>0)
			needUsers.add(report.targetID);
		if(report.targetID<0)
			needGroups.add(-report.targetID);
		needUsers.add(report.reporterID);

		ArrayList<Map<String, Object>> contentForTemplate=new ArrayList<>();
		int i=0;
		for(ReportableContentObject co:report.content){
			switch(co){
				case Post p -> {
					needPosts.add(p.id);
					contentForTemplate.add(Map.of("type", p.getReplyLevel()>0 ? "comment" : "post", "id", p.id, "url", "/settings/admin/reports/"+id+"/content/"+i));
				}
				case MailMessage msg -> {
					needMessages.add(msg.id);
					contentForTemplate.add(Map.of("type", "message", "id", msg.id, "url", "/settings/admin/reports/"+id+"/content/"+i));
				}
				case Photo photo -> {
					needPhotos.add(photo.id);
					contentForTemplate.add(Map.of("type", "photo", "id", photo.id, "url", photo.getURL(), "pvData", photo.getSinglePhotoViewerData()));
				}
				case Comment comment -> {
					needComments.add(comment.id);
					contentForTemplate.add(Map.of("type", "actualComment", "id", comment.id, "url", "/settings/admin/reports/"+id+"/content/"+i));
				}
			}
			i++;
		}
		List<ViolationReportAction> actions=ctx.getModerationController().getViolationReportActions(report);
		needUsers.addAll(actions.stream().map(ViolationReportAction::userID).collect(Collectors.toSet()));

		Map<Integer, Post> posts=ctx.getWallController().getPosts(needPosts);
		Map<Long, MailMessage> messages=ctx.getMailController().getMessagesAsModerator(needMessages);
		Map<Long, Photo> photos=ctx.getPhotosController().getPhotosIgnoringPrivacy(needPhotos);
		Map<Long, Comment> comments=ctx.getCommentsController().getCommentsIgnoringPrivacy(needComments);
		Map<Integer, User> users=ctx.getUsersController().getUsers(needUsers);
		Map<Integer, Group> groups=ctx.getGroupsController().getGroupsByIdAsMap(needGroups);

		Actor target=report.targetID>0 ? users.get(report.targetID) : groups.get(-report.targetID);

		Lang l=lang(req);
		List<ViolationReportActionViewModel> actionViewModels=actions.stream().map(a->{
			User adminUser=users.get(a.userID());
			HashMap<String, Object> links=new HashMap<>();
			links.put("adminUser", Map.of("href", adminUser!=null ? adminUser.getProfileURL() : "/id"+a.userID()));
			HashMap<String, Object> langArgs=new HashMap<>();
			langArgs.put("name", adminUser!=null ? adminUser.getFullName() : "DELETED");
			langArgs.put("gender", adminUser!=null ? adminUser.gender : User.Gender.UNKNOWN);
			String mainText=switch(a.actionType()){
				default -> null;
				case REOPEN -> l.get("report_log_reopened", langArgs);
				case RESOLVE_REJECT -> l.get("report_log_rejected", langArgs);
				case COMMENT -> l.get("report_log_commented", langArgs);
				case RESOLVE_WITH_ACTION -> {
					if(report.targetID>0){
						User targetUser=(User)target;
						langArgs.put("targetName", targetUser!=null ? targetUser.getFirstLastAndGender() : "DELETED");
						links.put("targetUser", Map.of("href", targetUser!=null ? targetUser.getProfileURL() : "/id"+report.targetID));
					}
					yield l.get("admin_audit_log_changed_user_restrictions", langArgs);
				}
				case DELETE_CONTENT -> l.get("report_log_deleted_content", langArgs);
			};
			return new ViolationReportActionViewModel(a, TextProcessor.substituteLinks(mainText, links), switch(a.actionType()){
				default -> null;
				case COMMENT -> TextProcessor.postprocessPostHTMLForDisplay(a.text(), false, false);
				case RESOLVE_WITH_ACTION -> {
					User targetUser=users.get(report.targetID);
					String statusStr=switch(UserBanStatus.valueOf(a.extra().get("status").getAsString())){
						case NONE -> l.get("admin_user_state_no_restrictions");
						case FROZEN -> l.get("admin_user_state_frozen", Map.of("expirationTime", l.formatDate(Instant.ofEpochMilli(a.extra().get("expiresAt").getAsLong()), timeZoneForRequest(req), false)));
						case SUSPENDED -> {
							if(targetUser instanceof ForeignUser)
								yield l.get("admin_user_state_suspended_foreign");
							else
								yield l.get("admin_user_state_suspended", Map.of("deletionTime", l.formatDate(a.time().plus(UserBanInfo.ACCOUNT_DELETION_DAYS, ChronoUnit.DAYS), timeZoneForRequest(req), false)));
						}
						case HIDDEN -> l.get("admin_user_state_hidden");
						case SELF_DEACTIVATED -> null;
					};
					if(a.extra().has("message")){
						statusStr+="<br/>"+l.get("admin_user_ban_message")+": "+TextProcessor.escapeHTML(a.extra().get("message").getAsString());
					}
					yield statusStr;
				}
			});
		}).toList();

		RenderedTemplateResponse model=new RenderedTemplateResponse("report", req);
		model.pageTitle(lang(req).get("admin_report_title_X", Map.of("id", id)));
		model.with("report", report);
		model.with("users", users).with("groups", groups);
		model.with("posts", posts).with("messages", messages).with("photos", photos).with("comments", comments);
		model.with("canDeleteContent", (!posts.isEmpty() || !messages.isEmpty() || !photos.isEmpty() || !comments.isEmpty()) && target!=null);
		model.with("actions", actionViewModels);
		model.with("content", contentForTemplate);
		model.with("isLocalTarget", target!=null && StringUtils.isEmpty(target.domain));
		model.with("toolbarTitle", l.get("menu_reports"));
		return model;
	}

	public static Object reportMarkResolved(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, false);
		if(report.state!=ViolationReport.State.OPEN)
			throw new BadRequestException();
		ctx.getModerationController().rejectViolationReport(report, self.user);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object reportMarkUnresolved(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, false);
		if(report.state==ViolationReport.State.OPEN)
			throw new BadRequestException();
		ctx.getModerationController().markViolationReportUnresolved(report, self.user);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object reportAddComment(Request req, Response resp, Account self, ApplicationContext ctx){
		requireQueryParams(req, "text");
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, false);
		String text=req.queryParams("text");
		ctx.getModerationController().addViolationReportComment(report, self.user, text);
		if(isAjax(req))
			return new WebDeltaResponse(resp).setContent("commentText", "").refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object reportShowContent(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, true);
		int index=safeParseInt(req.params(":index"));
		if(index<0 || index>=report.content.size())
			throw new BadRequestException();
		ReportableContentObject cobj=report.content.get(index);
		RenderedTemplateResponse model=new RenderedTemplateResponse("report_content", req);
		Lang l=lang(req);
		String title;
		model.with("content", cobj);
		model.with("reportID", id);
		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
		switch(cobj){
			case Post post -> {
				title=l.get(post.getReplyLevel()>0 ? "admin_report_content_comment" : "admin_report_content_post", Map.of("id", post.id));
				model.with("contentType", "post");
				needUsers.add(post.authorID);
				if(post.ownerID>0)
					needUsers.add(post.ownerID);
				else
					needGroups.add(-post.ownerID);
			}
			case MailMessage msg -> {
				title=l.get("admin_report_content_message", Map.of("id", msg.id));
				model.with("contentType", "message");
				needUsers.addAll(msg.to);
				if(msg.cc!=null)
					needUsers.addAll(msg.cc);
				needUsers.add(msg.senderID);
			}
			case Photo photo -> throw new ObjectNotFoundException();
			case Comment comment -> {
				title=l.get("admin_report_content_comment", Map.of("id", comment.id));
				model.with("contentType", "comment");
				needUsers.add(comment.authorID);
				if(comment.ownerID>0)
					needUsers.add(comment.ownerID);
				else
					needGroups.add(-comment.ownerID);
			}
		}
		model.with("users", ctx.getUsersController().getUsers(needUsers));
		model.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(needGroups));
		if(isAjax(req)){
			return new WebDeltaResponse(resp).box(title, model.renderBlock("content"), null, !isMobile(req));
		}
		model.pageTitle(title);
		return model;
	}

	public static Object reportConfirmDeleteContent(Request req, Response resp, Account self, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ctx.getModerationController().getViolationReportByID(id, false);
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("report_delete_content_title"), l.get("report_confirm_delete_content"), "/settings/admin/reports/"+id+"/deleteContent");
	}

	public static Object reportDeleteContent(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		int id=safeParseInt(req.params(":id"));
		ViolationReport report=ctx.getModerationController().getViolationReportByID(id, true);
		ctx.getModerationController().deleteViolationReportContent(report, info, true);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object roles(Request req, Response resp, SessionInfo info, ApplicationContext ctx){
		jsLangKey(req, "admin_delete_role", "admin_delete_role_confirm", "yes", "no");
		List<UserRoleViewModel> roles=ctx.getModerationController().getRoles(info.permissions);
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_roles", req)
				.pageTitle(lang(req).get("admin_roles"))
				.with("toolbarTitle", lang(req).get("menu_admin"))
				.with("roles", roles);
		String msg=req.session().attribute("adminRolesMessage");
		if(StringUtils.isNotEmpty(msg)){
			req.session().removeAttribute("adminRolesMessage");
			model.with("message", msg);
		}
		return model;
	}

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

	public static Object auditLog(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_audit_log", req);
		PaginatedList<AuditLogEntry> log;
		if(req.queryParams("uid")!=null){
			User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.queryParams("uid")));
			model.with("user", user);
			model.with("staffNoteCount", ctx.getModerationController().getUserStaffNoteCount(user));
			log=ctx.getModerationController().getUserAuditLog(user, offset(req), 100);
		}else{
			log=ctx.getModerationController().getGlobalAuditLog(offset(req), 100);
		}
		Map<Integer, User> users=ctx.getUsersController().getUsers(
				IntStream.concat(log.list.stream().mapToInt(AuditLogEntry::ownerID), log.list.stream().mapToInt(AuditLogEntry::adminID))
						.filter(id->id>0)
						.boxed()
						.collect(Collectors.toSet())
		);
		Map<Integer, Group> groups=ctx.getGroupsController().getGroupsByIdAsMap(log.list.stream().map(AuditLogEntry::ownerID).filter(id->id<0).map(id->-id).collect(Collectors.toSet()));
		final Lang l=lang(req);
		List<AuditLogEntryViewModel> viewModels=log.list.stream().map(le->{
			User adminUser=users.get(le.adminID());
			HashMap<String, Object> links=new HashMap<>();
			links.put("adminUser", Map.of("href", adminUser!=null ? adminUser.getProfileURL() : "/id"+le.adminID()));
			HashMap<String, Object> langArgs=new HashMap<>();
			langArgs.put("name", adminUser!=null ? adminUser.getFullName() : "DELETED");
			langArgs.put("gender", adminUser!=null ? adminUser.gender : User.Gender.UNKNOWN);
			String mainText=switch(le.action()){
				case CREATE_ROLE -> {
					langArgs.put("roleName", le.extra().get("name"));
					yield l.get("admin_audit_log_created_role", langArgs);
				}
				case EDIT_ROLE -> {
					UserRole role=Config.userRoles.get((int)le.objectID());
					langArgs.put("roleName", role!=null ? role.name() : "#"+le.objectID());
					yield l.get("admin_audit_log_edited_role", langArgs);
				}
				case DELETE_ROLE -> {
					langArgs.put("roleName", le.extra().get("name"));
					yield l.get("admin_audit_log_deleted_role", langArgs);
				}
				case ASSIGN_ROLE -> {
					User targetUser=users.get(le.ownerID());
					langArgs.put("targetName", targetUser!=null ? targetUser.getFirstLastAndGender() : "DELETED");
					links.put("targetUser", Map.of("href", targetUser!=null ? targetUser.getProfileURL() : "/id"+le.ownerID()));
					if(le.objectID()==0){
						yield l.get("admin_audit_log_unassigned_role", langArgs);
					}else{
						UserRole role=Config.userRoles.get((int)le.objectID());
						langArgs.put("roleName", role!=null ? role.name() : "#"+le.objectID());
						yield l.get("admin_audit_log_assigned_role", langArgs);
					}
				}

				case SET_USER_EMAIL ->{
					User targetUser=users.get(le.ownerID());
					langArgs.put("targetName", targetUser!=null ? targetUser.getFirstLastAndGender() : "DELETED");
					links.put("targetUser", Map.of("href", targetUser!=null ? targetUser.getProfileURL() : "/id"+le.ownerID()));
					yield l.get("admin_audit_log_changed_email", langArgs);
				}
				case ACTIVATE_ACCOUNT -> {
					User targetUser=users.get(le.ownerID());
					langArgs.put("targetName", targetUser!=null ? targetUser.getFirstLastAndGender() : "DELETED");
					links.put("targetUser", Map.of("href", targetUser!=null ? targetUser.getProfileURL() : "/id"+le.ownerID()));
					yield l.get("admin_audit_log_activated_account", langArgs);
				}
				case RESET_USER_PASSWORD -> {
					User targetUser=users.get(le.ownerID());
					langArgs.put("targetName", targetUser!=null ? targetUser.getFirstLastAndGender() : "DELETED");
					links.put("targetUser", Map.of("href", targetUser!=null ? targetUser.getProfileURL() : "/id"+le.ownerID()));
					yield l.get("admin_audit_log_reset_password", langArgs);
				}
				case END_USER_SESSION -> {
					User targetUser=users.get(le.ownerID());
					langArgs.put("targetName", targetUser!=null ? targetUser.getFirstLastAndGender() : "DELETED");
					links.put("targetUser", Map.of("href", targetUser!=null ? targetUser.getProfileURL() : "/id"+le.ownerID()));
					yield l.get("admin_audit_log_ended_session", langArgs);
				}
				case BAN_USER -> {
					User targetUser=users.get(le.ownerID());
					langArgs.put("targetName", targetUser!=null ? targetUser.getFirstLastAndGender() : "DELETED");
					links.put("targetUser", Map.of("href", targetUser!=null ? targetUser.getProfileURL() : "/id"+le.ownerID()));
					yield l.get("admin_audit_log_changed_user_restrictions", langArgs);
				}
				case DELETE_USER -> {
					langArgs.put("targetName", le.extra().get("name"));
					links.put("targetUser", Map.of("href", "/id"+le.ownerID()));
					yield l.get("admin_audit_log_deleted_user_account", langArgs);
				}

				case CREATE_EMAIL_DOMAIN_RULE -> {
					langArgs.put("domain", le.extra().get("domain"));
					yield l.get("admin_audit_log_created_email_rule", langArgs);
				}
				case UPDATE_EMAIL_DOMAIN_RULE -> {
					langArgs.put("domain", le.extra().get("domain"));
					yield l.get("admin_audit_log_updated_email_rule", langArgs);
				}
				case DELETE_EMAIL_DOMAIN_RULE -> {
					langArgs.put("domain", le.extra().get("domain"));
					yield l.get("admin_audit_log_deleted_email_rule", langArgs);
				}
				case CREATE_IP_RULE -> {
					langArgs.put("ipOrSubnet", le.extra().get("addr"));
					yield l.get("admin_audit_log_created_ip_rule", langArgs);
				}
				case UPDATE_IP_RULE -> {
					langArgs.put("ipOrSubnet", le.extra().get("addr"));
					yield l.get("admin_audit_log_updated_ip_rule", langArgs);
				}
				case DELETE_IP_RULE -> {
					langArgs.put("ipOrSubnet", le.extra().get("addr"));
					yield l.get("admin_audit_log_deleted_ip_rule", langArgs);
				}

				case DELETE_SIGNUP_INVITE -> {
					User targetUser=users.get(le.ownerID());
					langArgs.put("targetName", targetUser!=null ? targetUser.getFirstLastAndGender() : "DELETED");
					links.put("targetUser", Map.of("href", targetUser!=null ? targetUser.getProfileURL() : "/id"+le.ownerID()));
					yield l.get("admin_audit_log_deleted_invite", langArgs);
				}
			};
			String extraText=switch(le.action()){
				case ASSIGN_ROLE, DELETE_ROLE, ACTIVATE_ACCOUNT, RESET_USER_PASSWORD, DELETE_USER -> null;

				case CREATE_ROLE -> {
					StringBuilder sb=new StringBuilder("<i>");
					EnumSet<UserRole.Permission> permissions=EnumSet.noneOf(UserRole.Permission.class);
					deserializeEnumSet(permissions, UserRole.Permission.class, Base64.getDecoder().decode((String) le.extra().get("permissions")));
					for(UserRole.Permission permission:permissions){
						sb.append("<div>+ ");
						sb.append(l.get(permission.getLangKey()));
						sb.append("</div>");
					}
					sb.append("</i>");
					yield sb.toString();
				}
				case EDIT_ROLE -> {
					StringBuilder sb=new StringBuilder("<i>");
					if(le.extra().containsKey("oldName")){
						sb.append("<div>");
						sb.append(l.get("admin_role_name"));
						sb.append(": \"");
						sb.append(le.extra().get("oldName"));
						sb.append("\" &rarr; \"");
						sb.append(le.extra().get("newName"));
						sb.append("\"</div>");
					}
					if(le.extra().containsKey("oldPermissions")){
						EnumSet<UserRole.Permission> oldPermissions=EnumSet.noneOf(UserRole.Permission.class);
						deserializeEnumSet(oldPermissions, UserRole.Permission.class, Base64.getDecoder().decode((String) le.extra().get("oldPermissions")));
						EnumSet<UserRole.Permission> newPermissions=EnumSet.noneOf(UserRole.Permission.class);
						deserializeEnumSet(newPermissions, UserRole.Permission.class, Base64.getDecoder().decode((String) le.extra().get("newPermissions")));
						for(UserRole.Permission permission:oldPermissions){
							if(!newPermissions.contains(permission)){
								sb.append("<div>- ");
								sb.append(l.get(permission.getLangKey()));
								sb.append("</div>");
							}
						}
						for(UserRole.Permission permission:newPermissions){
							if(!oldPermissions.contains(permission)){
								sb.append("<div>+ ");
								sb.append(l.get(permission.getLangKey()));
								sb.append("</div>");
							}
						}
					}
					sb.append("</i>");
					yield sb.toString();
				}

				case SET_USER_EMAIL -> TextProcessor.escapeHTML(le.extra().get("oldEmail").toString())+" &rarr; "+TextProcessor.escapeHTML(le.extra().get("newEmail").toString());
				case END_USER_SESSION -> l.get("ip_address")+": "+deserializeInetAddress(Base64.getDecoder().decode((String)le.extra().get("ip"))).getHostAddress();
				case BAN_USER -> {
					User targetUser=users.get(le.ownerID());
					String statusStr=switch(UserBanStatus.valueOf((String)le.extra().get("status"))){
						case NONE -> l.get("admin_user_state_no_restrictions");
						case FROZEN -> {
							Object expiresAt=le.extra().get("expiresAt");
							yield l.get("admin_user_state_frozen", Map.of("expirationTime", expiresAt==null ? l.get("email_account_frozen_until_first_login") : l.formatDate(Instant.ofEpochMilli(((Number)expiresAt).longValue()), timeZoneForRequest(req), false)));
						}
						case SUSPENDED -> {
							if(targetUser instanceof ForeignUser)
								yield l.get("admin_user_state_suspended_foreign");
							else
								yield l.get("admin_user_state_suspended", Map.of("deletionTime", l.formatDate(le.time().plus(UserBanInfo.ACCOUNT_DELETION_DAYS, ChronoUnit.DAYS), timeZoneForRequest(req), false)));
						}
						case HIDDEN -> l.get("admin_user_state_hidden");
						case SELF_DEACTIVATED -> null;
					};
					if(le.extra().get("message")!=null){
						statusStr+="<br/>"+l.get("admin_user_ban_message")+": "+TextProcessor.escapeHTML((String)le.extra().get("message"));
					}
					yield statusStr;
				}

				case CREATE_EMAIL_DOMAIN_RULE, DELETE_EMAIL_DOMAIN_RULE -> "<i>"+l.get("admin_rule_action")+": "+l.get(EmailDomainBlockRule.Action.valueOf((String)le.extra().get("action")).getLangKey())+"</i>";
				case UPDATE_EMAIL_DOMAIN_RULE -> "<i>"+l.get("admin_rule_action")+": "
							+l.get(EmailDomainBlockRule.Action.valueOf((String)le.extra().get("oldAction")).getLangKey())
							+" &rarr; "
							+l.get(EmailDomainBlockRule.Action.valueOf((String)le.extra().get("newAction")).getLangKey())
							+"</i>";
				case CREATE_IP_RULE, DELETE_IP_RULE -> "<i>"+l.get("admin_rule_action")+": "+l.get(IPBlockRule.Action.valueOf((String)le.extra().get("action")).getLangKey())+"<br/>"
						+l.get("admin_ip_rule_expiry")+": "+l.formatDate(Instant.ofEpochSecond(((Number)le.extra().get("expiry")).longValue()), timeZoneForRequest(req), true)+"</i>";
				case UPDATE_IP_RULE -> {
					ArrayList<String> lines=new ArrayList<>();
					if(le.extra().containsKey("oldAction")){
						lines.add(l.get("admin_rule_action")+": "+l.get(IPBlockRule.Action.valueOf((String)le.extra().get("oldAction")).getLangKey())
								+" &rarr; "+l.get(IPBlockRule.Action.valueOf((String)le.extra().get("newAction")).getLangKey()));
					}
					if(le.extra().containsKey("oldExpiry")){
						lines.add(l.get("admin_ip_rule_expiry")+": "+l.formatDate(Instant.ofEpochSecond(((Number)le.extra().get("oldExpiry")).longValue()), timeZoneForRequest(req), true)
								+" &rarr; "+l.formatDate(Instant.ofEpochSecond(((Number)le.extra().get("newExpiry")).longValue()), timeZoneForRequest(req), true));
					}
					yield "<i>"+String.join("<br/>", lines)+"</i>";
				}

				case DELETE_SIGNUP_INVITE -> {
					String r=l.get("invite_signup_count")+": "+((Number)le.extra().get("signups")).intValue();
					if(le.extra().containsKey("email"))
						r+="<br/>"+l.get("email")+": "+TextProcessor.escapeHTML(le.extra().get("email").toString());
					if(le.extra().containsKey("name"))
						r+="<br/>"+l.get("name")+": "+TextProcessor.escapeHTML(le.extra().get("name").toString());
					yield r;
				}
			};
			return new AuditLogEntryViewModel(le, TextProcessor.substituteLinks(mainText, links), extraText);
		}).toList();
		model.pageTitle(lang(req).get("admin_audit_log")).with("toolbarTitle", lang(req).get("menu_admin")).with("users", users).with("groups", groups);
		model.paginate(new PaginatedList<>(log, viewModels));
		return model;
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
		model.pageTitle(lang(req).get("admin_manage_user")+" | "+user.getFullName());
		model.with("staffNoteCount", ctx.getModerationController().getUserStaffNoteCount(user));
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
			info=new UserBanInfo(Instant.now(), expiresAt, message, forcePasswordChange, self.user.id, report==null ? 0 : report.id);
		}else{
			if(report!=null)
				throw new BadRequestException();
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

	public static Object reportsOfUser(Request req, Response resp, Account self, ApplicationContext ctx){
		return userReports(req, resp, self, ctx, true);
	}

	public static Object reportsByUser(Request req, Response resp, Account self, ApplicationContext ctx){
		return userReports(req, resp, self, ctx, false);
	}

	private static Object userReports(Request req, Response resp, Account self, ApplicationContext ctx, boolean ofUser){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		RenderedTemplateResponse model=new RenderedTemplateResponse("report_list", req);
		model.pageTitle(lang(req).get("menu_reports"));
		PaginatedList<ViolationReport> reports;
		if(ofUser){
			model.with("tab", "reportsOf");
			reports=ctx.getModerationController().getViolationReportsOfActor(user, offset(req), 50);
		}else{
			model.with("tab", "reportsBy");
			reports=ctx.getModerationController().getViolationReportsByUser(user, offset(req), 50);
		}
		model.paginate(reports);

		Set<Integer> userIDs=reports.list.stream().filter(r->r.targetID>0).map(r->r.targetID).collect(Collectors.toSet());
		userIDs.addAll(reports.list.stream().filter(r->r.reporterID!=0).map(r->r.reporterID).collect(Collectors.toSet()));
		Set<Integer> groupIDs=reports.list.stream().filter(r->r.targetID<0).map(r->-r.targetID).collect(Collectors.toSet());

		model.with("users", ctx.getUsersController().getUsers(userIDs))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(groupIDs))
				.with("filteredByUser", user);
		model.with("staffNoteCount", ctx.getModerationController().getUserStaffNoteCount(user));

		return model;
	}

	public static Object userStaffNotes(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_users_notes", req);
		PaginatedList<ActorStaffNote> notes=ctx.getModerationController().getUserStaffNotes(user, offset(req), 50);
		model.paginate(notes);
		model.with("users", ctx.getUsersController().getUsers(notes.list.stream().map(ActorStaffNote::authorID).collect(Collectors.toSet())));
		model.with("user", user).with("staffNoteCount", notes.total);
		return model;
	}

	public static Object userStaffNoteAdd(Request req, Response resp, Account self, ApplicationContext ctx){
		User user=ctx.getUsersController().getUserOrThrow(safeParseInt(req.params(":id")));
		requireQueryParams(req, "text");
		String text=req.queryParams("text");
		ctx.getModerationController().createUserStaffNote(self.user, user, text);
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
		int noteID=safeParseInt(req.params(":noteID"));
		ctx.getModerationController().deleteUserStaffNote(ctx.getModerationController().getUserStaffNoteOrThrow(noteID));
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object emailDomainRules(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_email_rules", req);
		List<EmailDomainBlockRuleFull> rules=ctx.getModerationController().getEmailDomainBlockRulesFull();
		model.pageTitle(lang(req).get("admin_email_domain_rules"))
				.with("rules", rules)
				.with("users", ctx.getUsersController().getUsers(rules.stream().map(EmailDomainBlockRuleFull::creatorID).collect(Collectors.toSet())))
				.addMessage(req, "adminEmailRulesMessage");
		return model;
	}

	public static Object emailDomainRuleCreateForm(Request req, Response resp, Account self, ApplicationContext ctx){
		return wrapForm(req, resp, "admin_email_rule_form", "/settings/admin/emailRules/create", lang(req).get("admin_email_rule_title"), "create", "adminCreateEmailRule", List.of(), key->null, null);
	}

	public static Object emailDomainRuleCreate(Request req, Response resp, Account self, ApplicationContext ctx){
		try{
			String domain=requireFormField(req, "domain", null);
			EmailDomainBlockRule.Action action=requireFormField(req, "ruleAction", null, EmailDomainBlockRule.Action.class);
			String note=req.queryParams("note");
			ctx.getModerationController().createEmailDomainBlockRule(self.user, domain, action, note);
		}catch(UserErrorException x){
			return wrapForm(req, resp, "admin_email_rule_form", "/settings/admin/emailRules/create", lang(req).get("admin_email_rule_title"), "create", "adminCreateEmailRule",
					List.of("domain", "ruleAction", "note"), req::queryParams, lang(req).get(x.getMessage()));
		}
		req.session().attribute("adminEmailRulesMessage", lang(req).get("admin_email_rule_created"));
		return ajaxAwareRedirect(req, resp, "/settings/admin/emailRules");
	}

	public static Object emailDomainRuleEdit(Request req, Response resp, Account self, ApplicationContext ctx){
		EmailDomainBlockRuleFull rule=ctx.getModerationController().getEmailDomainBlockRuleOrThrow(req.params(":domain"));
		return wrapForm(req, resp, "admin_email_rule_form", "/settings/admin/emailRules/"+rule.rule().domain()+"/update", lang(req).get("admin_email_rule_title"), "save", "adminCreateEmailRule",
				List.of("ruleAction", "note"), key->switch(key){
					case "ruleAction" -> rule.rule().action();
					case "note" -> rule.note();
					default -> throw new IllegalStateException("Unexpected value: " + key);
				}, null, Map.of("editing", true, "domain", rule.rule().domain()));
	}

	public static Object emailDomainRuleUpdate(Request req, Response resp, Account self, ApplicationContext ctx){
		EmailDomainBlockRuleFull rule=ctx.getModerationController().getEmailDomainBlockRuleOrThrow(req.params(":domain"));
		try{
			EmailDomainBlockRule.Action action=requireFormField(req, "ruleAction", null, EmailDomainBlockRule.Action.class);
			String note=req.queryParams("note");
			ctx.getModerationController().updateEmailDomainBlockRule(self.user, rule, action, note);
		}catch(UserErrorException x){
			return wrapForm(req, resp, "admin_email_rule_form", "/settings/admin/emailRules/"+rule.rule().domain()+"/update", lang(req).get("admin_email_rule_title"), "save", "adminCreateEmailRule",
					List.of("ruleAction", "note"), key->switch(key){
						case "ruleAction" -> rule.rule().action();
						case "note" -> rule.note();
						default -> throw new IllegalStateException("Unexpected value: " + key);
					}, lang(req).get(x.getMessage()), Map.of("editing", true, "domain", rule.rule().domain()));
		}
		return ajaxAwareRedirect(req, resp, "/settings/admin/emailRules");
	}

	public static Object emailDomainRuleConfirmDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		EmailDomainBlockRuleFull rule=ctx.getModerationController().getEmailDomainBlockRuleOrThrow(req.params(":domain"));
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("delete"), l.get("admin_confirm_delete_rule"), "/settings/admin/emailRules/"+rule.rule().domain()+"/delete");
	}

	public static Object emailDomainRuleDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		EmailDomainBlockRuleFull rule=ctx.getModerationController().getEmailDomainBlockRuleOrThrow(req.params(":domain"));
		ctx.getModerationController().deleteEmailDomainBlockRule(self.user, rule);
		return ajaxAwareRedirect(req, resp, "/settings/admin/emailRules");
	}

	public static Object ipRules(Request req, Response resp, Account self, ApplicationContext ctx){
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_ip_rules", req);
		List<IPBlockRuleFull> rules=ctx.getModerationController().getIPBlockRulesFull();
		model.pageTitle(lang(req).get("admin_ip_rules"))
				.with("rules", rules)
				.with("users", ctx.getUsersController().getUsers(rules.stream().map(IPBlockRuleFull::creatorID).collect(Collectors.toSet())))
				.addMessage(req, "adminIPRulesMessage");
		return model;
	}

	public static Object ipRuleCreateForm(Request req, Response resp, Account self, ApplicationContext ctx){
		return wrapForm(req, resp, "admin_ip_rule_form", "/settings/admin/ipRules/create", lang(req).get("admin_ip_rule_title"), "create", "adminCreateIPRule", List.of(), key->null, null);
	}

	public static Object ipRuleCreate(Request req, Response resp, Account self, ApplicationContext ctx){
		try{
			InetAddressRange address=InetAddressRange.parse(requireFormField(req, "ipAddress", null));
			if(address==null)
				throw new UserErrorException("err_admin_ip_format_invalid");
			IPBlockRule.Action action=requireFormField(req, "ruleAction", null, IPBlockRule.Action.class);
			String note=req.queryParams("note");
			int expiry=Math.min(129600, Math.max(60, safeParseInt(requireFormField(req, "expiry", null))));
			ctx.getModerationController().createIPBlockRule(self.user, address, action, expiry, note);
		}catch(UserErrorException x){
			return wrapForm(req, resp, "admin_email_rule_form", "/settings/admin/ipRules/create", lang(req).get("admin_ip_rule_title"), "create", "adminCreateIPRule",
					List.of("domain", "ruleAction", "note"), req::queryParams, lang(req).get(x.getMessage()));
		}
		req.session().attribute("adminIPRulesMessage", lang(req).get("admin_ip_rule_created"));
		return ajaxAwareRedirect(req, resp, "/settings/admin/ipRules");
	}

	public static Object ipRuleEdit(Request req, Response resp, Account self, ApplicationContext ctx){
		IPBlockRuleFull rule=ctx.getModerationController().getIPBlockRuleFull(safeParseInt(req.params(":id")));
		return wrapForm(req, resp, "admin_ip_rule_form", "/settings/admin/ipRules/"+rule.rule().id()+"/update", lang(req).get("admin_ip_rule_title"), "save", "adminCreateIPRule",
				List.of("ruleAction", "note", "expiry"), key->switch(key){
					case "ruleAction" -> rule.rule().action();
					case "note" -> rule.note();
					case "expiry" -> rule.rule().expiresAt();
					default -> throw new IllegalStateException("Unexpected value: " + key);
				}, null, Map.of("editing", true, "ipAddress", rule.rule().ipRange().toString()));
	}

	public static Object ipRuleUpdate(Request req, Response resp, Account self, ApplicationContext ctx){
		IPBlockRuleFull rule=ctx.getModerationController().getIPBlockRuleFull(safeParseInt(req.params(":id")));
		try{
			IPBlockRule.Action action=requireFormField(req, "ruleAction", null, IPBlockRule.Action.class);
			String note=req.queryParams("note");
			int expiry=Math.min(129600, safeParseInt(requireFormField(req, "expiry", null)));
			if(expiry<60)
				expiry=0;
			ctx.getModerationController().updateIPBlockRule(self.user, rule, action, expiry, note);
		}catch(UserErrorException x){
			return wrapForm(req, resp, "admin_email_rule_form", "/settings/admin/ipRules/"+rule.rule().id()+"/update", lang(req).get("admin_ip_rule_title"), "save", "adminCreateIPRule",
					List.of("ruleAction", "note", "expiry"), key->switch(key){
						case "ruleAction" -> rule.rule().action();
						case "note" -> rule.note();
						case "expiry" -> rule.rule().expiresAt();
						default -> throw new IllegalStateException("Unexpected value: " + key);
					}, null, Map.of("editing", true, "ipAddress", rule.rule().ipRange().toString()));
		}
		return ajaxAwareRedirect(req, resp, "/settings/admin/ipRules");
	}

	public static Object ipRuleConfirmDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		IPBlockRuleFull rule=ctx.getModerationController().getIPBlockRuleFull(safeParseInt(req.params(":id")));
		Lang l=lang(req);
		return wrapConfirmation(req, resp, l.get("delete"), l.get("admin_confirm_delete_rule"), "/settings/admin/ipRules/"+rule.rule().id()+"/delete");
	}

	public static Object ipRuleDelete(Request req, Response resp, Account self, ApplicationContext ctx){
		IPBlockRuleFull rule=ctx.getModerationController().getIPBlockRuleFull(safeParseInt(req.params(":id")));
		ctx.getModerationController().deleteIPBlockRule(self.user, rule);
		return ajaxAwareRedirect(req, resp, "/settings/admin/ipRules");
	}

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
}
