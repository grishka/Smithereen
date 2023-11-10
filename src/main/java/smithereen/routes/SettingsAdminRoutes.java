package smithereen.routes;

import java.net.URLEncoder;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Mailer;
import smithereen.Utils;
import smithereen.model.Account;
import smithereen.model.FederationRestriction;
import smithereen.model.MailMessage;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.Server;
import smithereen.model.StatsPoint;
import smithereen.model.StatsType;
import smithereen.model.User;
import smithereen.model.UserRole;
import smithereen.model.ViolationReport;
import smithereen.model.WebDeltaResponse;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.lang.Lang;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
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
		int offset=parseIntOrDefault(req.queryParams("offset"), 0);
		List<Account> accounts=UserStorage.getAllAccounts(offset, 100);
		model.paginate(new PaginatedList<>(accounts, UserStorage.getLocalUserCount(), offset, 100));
		model.with("title", l.get("admin_users")+" | "+l.get("menu_admin")).with("toolbarTitle", l.get("menu_admin"));
		model.with("wideOnDesktop", true);
		jsLangKey(req, "cancel", "yes", "no");
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

	public static Object banUserForm(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		Lang l=lang(req);
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null || target.id==self.id || target.roleID!=0)
			throw new ObjectNotFoundException("err_user_not_found");
		RenderedTemplateResponse model=new RenderedTemplateResponse("admin_ban_user", req);
		model.with("targetAccount", target);
		return wrapForm(req, resp, "admin_ban_user", "/settings/admin/users/ban?accountID="+accountID, l.get("admin_ban"), "admin_ban", model);
	}

	public static Object banUser(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null || target.id==self.id || target.roleID!=0)
			throw new ObjectNotFoundException("err_user_not_found");
		Account.BanInfo banInfo=new Account.BanInfo();
		banInfo.reason=req.queryParams("message");
		banInfo.adminUserId=self.user.id;
		banInfo.when=Instant.now();
		UserStorage.putAccountBanInfo(accountID, banInfo);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

	public static Object confirmUnbanUser(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		req.attribute("noHistory", true);
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null)
			throw new ObjectNotFoundException("err_user_not_found");
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		User user=target.user;
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("admin_unban_X_confirm", Map.of("name", user.getFirstLastAndGender()))).with("formAction", "/settings/admin/users/unban?accountID="+accountID+"&_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object unbanUser(Request req, Response resp, Account self, ApplicationContext ctx) throws SQLException{
		int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
		Account target=UserStorage.getAccount(accountID);
		if(target==null)
			throw new ObjectNotFoundException("err_user_not_found");
		UserStorage.putAccountBanInfo(accountID, null);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));
		return "";
	}

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
		Lang l=Utils.lang(req);
		String back=Utils.back(req);
		User user=target.user;
		return new RenderedTemplateResponse("generic_confirm", req).with("message", l.get("admin_activate_X_confirm", Map.of("name", user.getFirstLastAndGender()))).with("formAction", "/settings/admin/users/activate?accountID="+accountID+"&_redir="+URLEncoder.encode(back)).with("back", back);
	}

	public static Object activateAccount(Request req, Response resp, Account self, ApplicationContext ctx){
		try{
			int accountID=parseIntOrDefault(req.queryParams("accountID"), 0);
			Account target=UserStorage.getAccount(accountID);
			if(target==null)
				throw new ObjectNotFoundException("err_user_not_found");
			SessionStorage.updateActivationInfo(accountID, null);
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
		resp.redirect(Utils.back(req));
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
			return new WebDeltaResponse(resp).setContent("ajaxUpdatable", model.renderBlock("ajaxPartialUpdate")).setAttribute("domainSearch", "data-base-url", baseURL);
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

		Set<Integer> userIDs=reports.list.stream().filter(r->r.targetType==ViolationReport.TargetType.USER).map(r->r.targetID).collect(Collectors.toSet());
		userIDs.addAll(reports.list.stream().filter(r->r.reporterID!=0).map(r->r.reporterID).collect(Collectors.toSet()));
		Set<Integer> groupIDs=reports.list.stream().filter(r->r.targetType==ViolationReport.TargetType.GROUP).map(r->r.targetID).collect(Collectors.toSet());
		Set<Integer> postIDs=reports.list.stream().filter(r->r.contentType==ViolationReport.ContentType.POST).map(r->(int)r.contentID).collect(Collectors.toSet());
		Set<Long> messageIDs=reports.list.stream().filter(r->r.contentType==ViolationReport.ContentType.MESSAGE).map(r->r.contentID).collect(Collectors.toSet());

		Map<Integer, Post> posts=ctx.getWallController().getPosts(postIDs);
		Map<Long, MailMessage> messages=ctx.getMailController().getMessagesAsModerator(messageIDs);
		for(Post post:posts.values()){
			userIDs.add(post.authorID);
			if(post.ownerID!=post.authorID){
				if(post.ownerID>0)
					userIDs.add(post.ownerID);
				else
					groupIDs.add(-post.ownerID);
			}
		}
		for(MailMessage msg:messages.values()){
			userIDs.add(msg.senderID);
		}

		model.with("users", ctx.getUsersController().getUsers(userIDs))
				.with("groups", ctx.getGroupsController().getGroupsByIdAsMap(groupIDs))
				.with("posts", posts)
				.with("messages", messages);

		return model;
	}

	public static Object reportAction(Request req, Response resp, Account self, ApplicationContext ctx){
		ViolationReport report=ctx.getModerationController().getViolationReportByID(safeParseInt(req.params(":id")));
		if(report.actionTime!=null)
			throw new BadRequestException("already resolved");
		if(req.queryParams("resolve")!=null){
			ctx.getModerationController().setViolationReportResolved(report, self.user);
			if(isAjax(req))
				return new WebDeltaResponse(resp).refresh();
			resp.redirect(back(req));
		}else if(req.queryParams("deleteContent")!=null){
			// TODO notify user
			if(report.contentType==ViolationReport.ContentType.POST){
				Post post=ctx.getWallController().getPostOrThrow((int)report.contentID);
				ctx.getWallController().deletePostAsServerModerator(sessionInfo(req), post);
			}else{
				throw new BadRequestException();
			}

			ctx.getModerationController().setViolationReportResolved(report, self.user);
			if(isAjax(req))
				return new WebDeltaResponse(resp).refresh();
			resp.redirect(back(req));
		}else if(req.queryParams("addCW")!=null){
			if(report.contentType==ViolationReport.ContentType.POST){
				Post post=ctx.getWallController().getPostOrThrow((int)report.contentID);
				if(post.hasContentWarning())
					throw new BadRequestException();

				Lang l=lang(req);
				return wrapForm(req, resp, "admin_add_cw", "/settings/admin/reports/"+report.id+"/doAddCW", l.get("post_form_cw"), "save", "addCW", List.of(), Function.identity(), null);
			}else{
				throw new BadRequestException();
			}
		}
		return "";
	}

	public static Object reportAddCW(Request req, Response resp, Account self, ApplicationContext ctx){
		ViolationReport report=ctx.getModerationController().getViolationReportByID(safeParseInt(req.params(":id")));
		if(report.actionTime!=null)
			throw new BadRequestException("already resolved");
		requireQueryParams(req, "cw");

		// TODO notify user
		if(report.contentType==ViolationReport.ContentType.POST){
			Post post=ctx.getWallController().getPostOrThrow((int)report.contentID);
			ctx.getWallController().setPostCWAsModerator(sessionInfo(req).permissions, post, req.queryParams("cw"));
		}else{
			throw new BadRequestException();
		}

		ctx.getModerationController().setViolationReportResolved(report, self.user);
		if(isAjax(req))
			return new WebDeltaResponse(resp).refresh();
		resp.redirect(back(req));

		return "";
	}
}
