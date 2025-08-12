package smithereen.routes.admin;

import com.google.gson.reflect.TypeToken;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.Mailer;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.ServerRule;
import smithereen.model.User;
import smithereen.model.UserBanInfo;
import smithereen.model.UserBanStatus;
import smithereen.model.WebDeltaResponse;
import smithereen.model.admin.AuditLogEntry;
import smithereen.model.admin.EmailDomainBlockRule;
import smithereen.model.admin.IPBlockRule;
import smithereen.model.admin.UserRole;
import smithereen.model.viewmodel.AuditLogEntryViewModel;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.text.TextProcessor;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static smithereen.Utils.*;

public class AdminGeneralRoutes{
	public static Object serverInfo(Request req, Response resp, Account self, ApplicationContext ctx){
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
				.with("signupEnableCaptcha", Config.signupFormUseCaptcha)
				.with("unconfirmedFaspRequests", ctx.getFaspController().getUnconfirmedProviderCount());
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
		model.with("unconfirmedFaspRequests", ctx.getFaspController().getUnconfirmedProviderCount());
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

				case CREATE_SERVER_RULE -> {
					langArgs.put("title", le.extra().get("title"));
					yield l.get("admin_audit_log_created_server_rule", langArgs);
				}
				case UPDATE_SERVER_RULE -> {
					langArgs.put("title", le.extra().get("oldTitle"));
					yield l.get("admin_audit_log_updated_server_rule", langArgs);
				}
				case DELETE_SERVER_RULE -> {
					langArgs.put("title", le.extra().get("title"));
					yield l.get("admin_audit_log_deleted_server_rule", langArgs);
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

				case CREATE_SERVER_RULE, DELETE_SERVER_RULE -> {
					String r="<i>";
					String description=le.extra().get("description").toString();
					if(!description.isEmpty())
						r+=l.get("admin_server_rule_description")+": "+TextProcessor.escapeHTML(description)+"<br/>";
					r+=l.get("admin_server_rule_priority")+": "+((Number)le.extra().get("priority")).intValue();
					Map<String, ServerRule.Translation> translations=gson.fromJson(le.extra().get("translations").toString(), new TypeToken<>(){});
					for(Map.Entry<String, ServerRule.Translation> translation:translations.entrySet()){
						Locale locale=Locale.forLanguageTag(translation.getKey());
						ServerRule.Translation t=translation.getValue();
						r+="<br/>"+l.get("admin_server_rule_title")+" ("+locale.getDisplayLanguage(l.getLocale())+"): "+t.title();
						if(StringUtils.isNotEmpty(t.description()))
							r+="<br/>"+l.get("admin_server_rule_description")+" ("+locale.getDisplayLanguage(l.getLocale())+"): "+t.description();
					}
					yield r+"</i>";
				}
				case UPDATE_SERVER_RULE -> {
					ArrayList<String> lines=new ArrayList<>();
					String oldTitle=le.extra().get("oldTitle").toString();
					String newTitle=le.extra().get("newTitle").toString();
					if(!oldTitle.equals(newTitle))
						lines.add(l.get("admin_server_rule_title")+": "+TextProcessor.escapeHTML(oldTitle)+" &rarr; "+TextProcessor.escapeHTML(newTitle));
					String oldDescription=le.extra().get("oldDescription").toString();
					String newDescription=le.extra().get("newDescription").toString();
					if(!oldDescription.equals(newDescription))
						lines.add(l.get("admin_server_rule_description")+": "+TextProcessor.escapeHTML(oldDescription)+" &rarr; "+TextProcessor.escapeHTML(newDescription));
					int oldPriority=((Number) le.extra().get("oldPriority")).intValue();
					int newPriority=((Number) le.extra().get("newPriority")).intValue();
					if(oldPriority!=newPriority)
						lines.add(l.get("admin_server_rule_priority")+": "+oldPriority+" &rarr; "+newPriority);
					Map<String, ServerRule.Translation> oldTranslations=gson.fromJson(le.extra().get("oldTranslations").toString(), new TypeToken<>(){});
					Map<String, ServerRule.Translation> newTranslations=gson.fromJson(le.extra().get("newTranslations").toString(), new TypeToken<>(){});
					if(!Objects.equals(oldTranslations, newTranslations)){
						HashSet<String> allLocales=new HashSet<>(oldTranslations.keySet());
						allLocales.addAll(newTranslations.keySet());
						for(String lang:allLocales){
							Locale locale=Locale.forLanguageTag(lang);
							ServerRule.Translation old=oldTranslations.get(lang);
							ServerRule.Translation new_=newTranslations.get(lang);
							if(Objects.equals(old, new_))
								continue;
							if(old==null || new_==null || !old.title().equals(new_.title()))
								lines.add(l.get("admin_server_rule_title")+" ("+locale.getDisplayLanguage(l.getLocale())+"): "+(old==null ? "" : old.title())+" &rarr; "+(new_==null ? "" : new_.title()));
							if(old==null || new_==null || !old.description().equals(new_.description()))
								lines.add(l.get("admin_server_rule_description")+" ("+locale.getDisplayLanguage(l.getLocale())+"): "+(old==null ? "" : old.description())+" &rarr; "+(new_==null ? "" : new_.description()));
						}
					}
					yield "<i>"+String.join("<br/>", lines)+"</i>";
				}
			};
			return new AuditLogEntryViewModel(le, TextProcessor.substituteLinks(mainText, links), extraText);
		}).toList();
		model.pageTitle(lang(req).get("admin_audit_log")).with("toolbarTitle", lang(req).get("menu_admin")).with("users", users).with("groups", groups);
		model.paginate(new PaginatedList<>(log, viewModels));
		model.with("unconfirmedFaspRequests", ctx.getFaspController().getUnconfirmedProviderCount());
		return model;
	}
}
