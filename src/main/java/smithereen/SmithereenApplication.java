package smithereen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.pebbletemplates.pebble.template.PebbleTemplate;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.controllers.MailController;
import smithereen.controllers.UsersController;
import smithereen.debug.DebugLog;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.FloodControlViolationException;
import smithereen.exceptions.InaccessibleProfileException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UnauthorizedRequestException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserContentUnavailableException;
import smithereen.exceptions.UserErrorException;
import smithereen.lang.Lang;
import smithereen.model.Account;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.SessionInfo;
import smithereen.model.User;
import smithereen.model.UserBanInfo;
import smithereen.model.UserBanStatus;
import smithereen.model.UserPresence;
import smithereen.model.UserRole;
import smithereen.model.WebDeltaResponse;
import smithereen.model.fasp.FASPCapability;
import smithereen.routes.ActivityPubRoutes;
import smithereen.routes.BoardRoutes;
import smithereen.routes.FaspApiRoutes;
import smithereen.routes.MastodonApiRoutes;
import smithereen.routes.BookmarksRoutes;
import smithereen.routes.CommentsRoutes;
import smithereen.routes.FriendsRoutes;
import smithereen.routes.GroupsRoutes;
import smithereen.routes.MailRoutes;
import smithereen.routes.NewsfeedRoutes;
import smithereen.routes.NotificationsRoutes;
import smithereen.routes.NotifierWebSocket;
import smithereen.routes.PhotosRoutes;
import smithereen.routes.PostRoutes;
import smithereen.routes.ProfileRoutes;
import smithereen.routes.SessionRoutes;
import smithereen.routes.SettingsAdminFaspRoutes;
import smithereen.routes.SettingsAdminRoutes;
import smithereen.routes.SettingsRoutes;
import smithereen.routes.SystemRoutes;
import smithereen.routes.WellKnownRoutes;
import smithereen.sparkext.ActivityPubCollectionPageResponse;
import smithereen.sparkext.ExtendedStreamingSerializer;
import smithereen.storage.DatabaseSchemaUpdater;
import smithereen.storage.GroupStorage;
import smithereen.storage.MediaStorageUtils;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
import smithereen.text.TextProcessor;
import smithereen.util.BackgroundTaskRunner;
import smithereen.util.FloodControl;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.MaintenanceScheduler;
import smithereen.util.PublicSuffixList;
import smithereen.util.TopLevelDomainList;
import spark.Filter;
import spark.Request;
import spark.Response;
import spark.Session;
import spark.utils.StringUtils;

import static smithereen.Utils.*;
import static smithereen.sparkext.SparkExtension.*;
import static spark.Spark.*;

public class SmithereenApplication{
	private static final Logger LOG;
	private static final ApplicationContext context;
	private static HashMap<String, Integer> accountIdsBySession=new HashMap<>();
	private static HashMap<Integer, Set<HttpSession>> sessionsByAccount=new HashMap<>();
	private static HashMap<String, String> notFoundPages=new HashMap<>();
	private static HashMap<String, String> serverErrorPages=new HashMap<>();

	static{
		System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
		System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");
		if(Config.DEBUG){
			System.setProperty("org.slf4j.simpleLogger.log.smithereen", "trace");
		}else{
			// Gets rid of "The requested route ... has not been mapped in Spark"
			System.setProperty("org.slf4j.simpleLogger.log.spark.http.matching", "warn");
		}
		// Gets rid of "Missing chars table for block: ..."
		System.setProperty("org.slf4j.simpleLogger.log.cz.jirutka.unidecode", "warn");
		String addProperties=System.getenv("SMITHEREEN_SET_PROPS");
		if(addProperties!=null){
			Arrays.stream(addProperties.split("&")).forEach(s->{
				String[] kv=s.split("=", 2);
				System.setProperty(kv[0], URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
			});
		}
		LOG=LoggerFactory.getLogger(SmithereenApplication.class);

		context=new ApplicationContext();
	}

	public static void main(String[] args){
		if(args.length==0){
			System.err.println("You need to specify the path to the config file as the first argument:\njava -jar smithereen.jar config.properties");
			System.exit(1);
		}

		System.setProperty("user.timezone", "UTC");

		try{
			Config.load(args[0]);
			Config.loadFromDatabase();
			DatabaseSchemaUpdater.maybeUpdate();
			Config.reloadRoles();
		}catch(IOException|SQLException x){
			throw new RuntimeException(x);
		}

		if(args.length>1){
			if(args[1].equalsIgnoreCase("init_admin")){
				CLI.initializeAdmin();
			}else{
				System.err.println("Unknown argument: '"+args[1]+"'");
				System.exit(1);
			}
			System.exit(0);
			return;
		}

		ActivityPubRoutes.registerActivityHandlers();
		prerenderErrorPages();

		ipAddress(Config.serverIP);
		port(Config.serverPort);
		useVirtualThreadPool();
		if(Config.staticFilesPath!=null)
			externalStaticFileLocation(Config.staticFilesPath);
		else
			staticFileLocation("/public");
		staticFiles.expireTime(7*24*60*60);

		webSocket("/system/ws/notifier", NotifierWebSocket.class);

		if(Config.DEBUG){
			before((req, resp)->{
				DebugLog.get().start();
			});
		}

		before((request, response) -> {
			request.attribute("context", context);

			if(request.pathInfo().startsWith("/api/"))
				return;
			Session session=request.session(false);
			if(session==null || session.attribute("info")==null){
				String psid=request.cookie("psid");
				if(psid!=null){
					if(!SessionStorage.fillSession(psid, request.session(true), request)){
						response.removeCookie("/", "psid");
					}else{
						response.cookie("/", "psid", psid, 10*365*24*60*60, false);
						SessionInfo info=sessionInfo(request);
						if(info.account!=null){
							synchronized(SmithereenApplication.class){
								accountIdsBySession.put(request.session().id(), info.account.id);
								sessionsByAccount.computeIfAbsent(info.account.id, HashSet::new).add(request.session().raw());
							}
						}
					}
				}
			}
			SessionInfo info=sessionInfo(request);
			if(info!=null && info.account!=null){
				info.account=UserStorage.getAccount(info.account.id);
				if(info.account==null){
					response.removeCookie("/", "psid");
					request.session().invalidate();
				}else{
					info.permissions=SessionStorage.getUserPermissions(info.account);

					String ua=Objects.requireNonNull(request.userAgent(), "");
					long uaHash=hashUserAgent(ua);
					InetAddress ip=getRequestIP(request);
					if(System.currentTimeMillis()-info.account.lastActive.toEpochMilli()>=10*60*1000 || !Objects.equals(info.account.lastIP, ip) || !Objects.equals(info.ip, ip) || info.userAgentHash!=uaHash){
						info.account.lastActive=Instant.now();
						info.userAgentHash=uaHash;
						info.ip=ip;
						BackgroundTaskRunner.getInstance().submit(()->{
							try{
								SessionStorage.setLastActive(info.account.id, request.cookie("psid"), info.account.lastActive, ip, ua, uaHash);
							}catch(SQLException x){
								LOG.warn("Error updating account session", x);
							}
						});
					}
				}
			}else{
				if(session!=null && session.attribute("bannedBot")!=null){
					throw new UserActionNotAllowedException();
				}
			}
//			String hs="";
//			for(String h:request.headers())
//				hs+="["+h+": "+request.headers(h)+"] ";
//			System.out.println(request.requestMethod()+" "+request.raw().getPathInfo()+" "+hs);
			if(request.pathInfo().startsWith("/activitypub/")){
				request.attribute("popup", Boolean.TRUE);
			}
			String ua=request.userAgent();
			if(StringUtils.isNotEmpty(ua) && isMobileUserAgent(ua)){
				request.attribute("mobile", Boolean.TRUE);
			}
		});
		before("/system/ws/*", (req, resp)->{
			// Websockets don't have access to the real request object, so let's put the session info somewhere where it can be accessed
			SessionInfo info=sessionInfo(req);
			if(info==null){
				throw new UserActionNotAllowedException();
			}
			req.attribute("sessionInfo", info);
			req.attribute("lang", lang(req));
		});
		before(SmithereenApplication::enforceAccountLimitationsIfAny);

		get("/", SmithereenApplication::indexPage);

		path("/feed", ()->{
			getLoggedIn("", NewsfeedRoutes::feed);
			getLoggedIn("/comments", NewsfeedRoutes::commentsFeed);
			getLoggedIn("/groups", NewsfeedRoutes::groupsFeed);
			postWithCSRF("/setFilters", NewsfeedRoutes::setFeedFilters);
			postWithCSRF("/groups/setFilters", NewsfeedRoutes::setGroupsFeedFilters);
			postWithCSRF("/comments/setFilters", NewsfeedRoutes::setCommentsFeedFilters);
		});

		path("/account", ()->{
			post("/login", SessionRoutes::login);
			get("/login", SessionRoutes::login);
			get("/logout", SessionRoutes::logout);
			post("/register", SessionRoutes::register);
			get("/register", SessionRoutes::registerForm);
			get("/resetPassword", SessionRoutes::resetPasswordForm);
			post("/resetPassword", SessionRoutes::resetPassword);
			get("/actuallyResetPassword", SessionRoutes::actuallyResetPasswordForm);
			post("/actuallyResetPassword", SessionRoutes::actuallyResetPassword);
			getWithCSRF("/resendConfirmationEmail", SessionRoutes::resendEmailConfirmation);
			getLoggedIn("/changeEmailForm", SessionRoutes::changeEmailForm);
			postWithCSRF("/changeEmail", SessionRoutes::changeEmail);
			getLoggedIn("/activate", SessionRoutes::activateAccount);
			post("/requestInvite", SessionRoutes::requestSignupInvite);
			getLoggedIn("/unfreezeBox", SessionRoutes::unfreezeBox);
			postWithCSRF("/unfreeze", SessionRoutes::unfreeze);
			postWithCSRF("/unfreezeChangePassword", SessionRoutes::unfreezeChangePassword);
			getLoggedIn("/reactivateBox", SessionRoutes::reactivateBox);
			postWithCSRF("/reactivate", SessionRoutes::reactivate);
		});

		path("/settings", ()->{
			path("/profile", ()->{
				getLoggedIn("/general", SettingsRoutes::profileEditGeneral);
				getLoggedIn("/interests", SettingsRoutes::profileEditInterests);
				getLoggedIn("/personal", SettingsRoutes::profileEditPersonal);
				getLoggedIn("/contacts", SettingsRoutes::profileEditContacts);
			});
			getLoggedIn("/", SettingsRoutes::settings);
			postWithCSRF("/createInvite", SettingsRoutes::createInvite);
			postWithCSRF("/updatePassword", SettingsRoutes::updatePassword);
			postWithCSRF("/updateProfileGeneral", SettingsRoutes::updateProfileGeneral);
			postWithCSRF("/updateProfilePicture", SettingsRoutes::updateProfilePicture);
			postWithCSRF("/removeProfilePicture", SettingsRoutes::removeProfilePicture);
			getLoggedIn("/confirmRemoveProfilePicture", SettingsRoutes::confirmRemoveProfilePicture);
			post("/setLanguage", SettingsRoutes::setLanguage);
			post("/setTimezone", SettingsRoutes::setTimezone);
			getLoggedIn("/blocking", SettingsRoutes::blocking);
			getLoggedIn("/blockDomainForm", SettingsRoutes::blockDomainForm);
			postWithCSRF("/blockDomain", SettingsRoutes::blockDomain);
			getLoggedIn("/confirmUnblockDomain", SettingsRoutes::confirmUnblockDomain);
			postWithCSRF("/unblockDomain", SettingsRoutes::unblockDomain);
			postWithCSRF("/updateEmail", SettingsRoutes::updateEmail);
			getWithCSRF("/cancelEmailChange", SettingsRoutes::cancelEmailChange);
			getWithCSRF("/resendEmailConfirmation", SettingsRoutes::resendEmailConfirmation);
			path("/invites", ()->{
				getLoggedIn("", SettingsRoutes::invites);
				getLoggedIn("/createEmailInviteForm", SettingsRoutes::createEmailInviteForm);
				postWithCSRF("/createEmailInvite", SettingsRoutes::createEmailInvite);
				getWithCSRF("/:id/resendEmail", SettingsRoutes::resendEmailInvite);
				getWithCSRF("/:id/delete", SettingsRoutes::deleteInvite);
				postWithCSRF("/:id/delete", SettingsRoutes::deleteInvite);
				getLoggedIn("/createInviteLinkForm", SettingsRoutes::createInviteLinkForm);
				postWithCSRF("/createInviteLink", SettingsRoutes::createInviteLink);
				getLoggedIn("/invitedUsers", SettingsRoutes::invitedUsers);
			});
			getLoggedIn("/privacy", SettingsRoutes::privacySettings);
			postWithCSRF("/privacy", SettingsRoutes::savePrivacySettings);
			getLoggedIn("/privacy/mobileEditSetting", SettingsRoutes::mobileEditPrivacy);
			getLoggedIn("/privacy/mobileBox", SettingsRoutes::mobilePrivacyBox);
			getLoggedIn("/privacy/mobileFeedTypes", SettingsRoutes::mobileFeedTypes);
			getLoggedIn("/deactivateAccountForm", SettingsRoutes::deactivateAccountForm);
			postWithCSRF("/deactivateAccount", SettingsRoutes::deactivateAccount);
			postWithCSRF("/updateAppearanceBehavior", SettingsRoutes::saveAppearanceBehaviorSettings);
			postWithCSRF("/updateUsername", SettingsRoutes::updateUsername);
			getLoggedIn("/sessions", SettingsRoutes::sessions);
			getLoggedIn("/confirmEndOtherSessions", SettingsRoutes::confirmEndOtherSessions);
			postWithCSRF("/endOtherSessions", SettingsRoutes::endOtherSessions);
			postWithCSRF("/updateProfileInterests", SettingsRoutes::updateProfileInterests);
			postWithCSRF("/updateProfilePersonal", SettingsRoutes::updateProfilePersonal);
			postWithCSRF("/updateProfileContacts", SettingsRoutes::updateProfileContacts);
			getLoggedIn("/notifications", SettingsRoutes::notificationsSettings);
			postWithCSRF("/updateNotifier", SettingsRoutes::updateNotifierSettings);
			path("/filters", ()->{
				getLoggedIn("", SettingsRoutes::filters);
				getLoggedIn("/create", SettingsRoutes::createFilterForm);
				postWithCSRF("/create", SettingsRoutes::createFilter);
				path("/:id", ()->{
					getLoggedIn("/edit", SettingsRoutes::editFilterForm);
					postWithCSRF("/edit", SettingsRoutes::editFilter);
					getLoggedIn("/confirmDelete", SettingsRoutes::confirmDeleteFilter);
					postWithCSRF("/delete", SettingsRoutes::deleteFilter);
				});
			});
			getLoggedIn("/moveAccountOptions", SettingsRoutes::moveAccountOptions);
			getLoggedIn("/alsoKnownAsLinks", SettingsRoutes::moveAccountLinks);
			postWithCSRF("/addAlsoKnownAs", SettingsRoutes::addAlsoKnownAs);
			getLoggedIn("/confirmDeleteAkaLink", SettingsRoutes::confirmDeleteAlsoKnownAs);
			postWithCSRF("/deleteAkaLink", SettingsRoutes::deleteAlsoKnownAs);
			getLoggedIn("/transferFollowersForm", SettingsRoutes::transferFollowersForm);
			postWithCSRF("/transferFollowers", SettingsRoutes::transferFollowers);
			getLoggedIn("/confirmRemoveMoveRedirect", SettingsRoutes::confirmRemoveMoveRedirect);
			postWithCSRF("/removeMoveRedirect", SettingsRoutes::removeMoveRedirect);
			postWithCSRF("/updateEmailNotifications", SettingsRoutes::updateEmailNotificationSettings);
			get("/notifications/emailUnsubscribe/:key", SettingsRoutes::emailUnsubscribe);
			post("/notifications/emailUnsubscribe/:key", SettingsRoutes::doEmailUnsubscribe);
			postWithCSRF("/updateStatus", SettingsRoutes::updateSelfStatus);
			getLoggedIn("/mobileStatusForm", SettingsRoutes::mobileStatusForm);

			path("/admin", ()->{
				getRequiringPermission("", UserRole.Permission.MANAGE_SERVER_SETTINGS, SettingsAdminRoutes::index);
				postRequiringPermissionWithCSRF("/updateServerInfo", UserRole.Permission.MANAGE_SERVER_SETTINGS, SettingsAdminRoutes::updateServerInfo);
				path("/users", ()->{
					getRequiringPermission("", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::users);
					getRequiringPermission("/roleForm", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::roleForm);
					postRequiringPermissionWithCSRF("/setRole", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::setUserRole);
					getRequiringPermission("/banForm", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::banUserForm);
					getRequiringPermission("/confirmActivate", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::confirmActivateAccount);
					postRequiringPermissionWithCSRF("/activate", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::activateAccount);
					getRequiringPermission("/changeEmailForm", UserRole.Permission.MANAGE_USER_ACCESS, SettingsAdminRoutes::changeUserEmailForm);
					postRequiringPermissionWithCSRF("/changeEmail", UserRole.Permission.MANAGE_USER_ACCESS, SettingsAdminRoutes::changeUserEmail);
					getRequiringPermissionWithCSRF("/endSession", UserRole.Permission.MANAGE_USER_ACCESS, SettingsAdminRoutes::endUserSession);
				});
				path("/reports", ()->{
					getRequiringPermission("", UserRole.Permission.MANAGE_REPORTS, SettingsAdminRoutes::reportsList);
					path("/:id", ()->{
						getRequiringPermission("", UserRole.Permission.MANAGE_REPORTS, SettingsAdminRoutes::viewReport);
						getRequiringPermissionWithCSRF("/markResolved", UserRole.Permission.MANAGE_REPORTS, SettingsAdminRoutes::reportMarkResolved);
						getRequiringPermissionWithCSRF("/markUnresolved", UserRole.Permission.MANAGE_REPORTS, SettingsAdminRoutes::reportMarkUnresolved);
						postRequiringPermissionWithCSRF("/addComment", UserRole.Permission.MANAGE_REPORTS, SettingsAdminRoutes::reportAddComment);
						getRequiringPermission("/content/:index", UserRole.Permission.MANAGE_REPORTS, SettingsAdminRoutes::reportShowContent);
						getRequiringPermission("/deleteContentForm", UserRole.Permission.MANAGE_REPORTS, SettingsAdminRoutes::reportConfirmDeleteContent);
						postRequiringPermissionWithCSRF("/deleteContent", UserRole.Permission.MANAGE_REPORTS, SettingsAdminRoutes::reportDeleteContent);
					});
				});
				path("/federation", ()->{
					getRequiringPermission("", UserRole.Permission.MANAGE_FEDERATION, SettingsAdminRoutes::federationServerList);
					getRequiringPermission("/:domain", UserRole.Permission.MANAGE_FEDERATION, SettingsAdminRoutes::federationServerDetails);
					getRequiringPermission("/:domain/restrictionForm", UserRole.Permission.MANAGE_FEDERATION, SettingsAdminRoutes::federationServerRestrictionForm);
					postRequiringPermissionWithCSRF("/:domain/restrict", UserRole.Permission.MANAGE_FEDERATION, SettingsAdminRoutes::federationRestrictServer);
					getRequiringPermissionWithCSRF("/:domain/resetAvailability", UserRole.Permission.MANAGE_FEDERATION, SettingsAdminRoutes::federationResetServerAvailability);
				});
				path("/roles", ()->{
					getRequiringPermission("", UserRole.Permission.MANAGE_ROLES, SettingsAdminRoutes::roles);
					getRequiringPermission("/create", UserRole.Permission.MANAGE_ROLES, SettingsAdminRoutes::createRoleForm);
					postRequiringPermissionWithCSRF("/create", UserRole.Permission.MANAGE_ROLES, SettingsAdminRoutes::saveRole);
					getRequiringPermission("/:id", UserRole.Permission.MANAGE_ROLES, SettingsAdminRoutes::editRole);
					postRequiringPermissionWithCSRF("/:id", UserRole.Permission.MANAGE_ROLES, SettingsAdminRoutes::saveRole);
					postRequiringPermissionWithCSRF("/:id/delete", UserRole.Permission.MANAGE_ROLES, SettingsAdminRoutes::deleteRole);
				});
				path("/signupRequests", ()->{
					getRequiringPermission("", UserRole.Permission.MANAGE_INVITES, SettingsAdminRoutes::signupRequests);
					postRequiringPermissionWithCSRF("/:id/respond", UserRole.Permission.MANAGE_INVITES, SettingsAdminRoutes::respondToSignupRequest);
				});
				getRequiringPermission("/other", UserRole.Permission.MANAGE_SERVER_SETTINGS, SettingsAdminRoutes::otherSettings);
				postRequiringPermissionWithCSRF("/updateEmailSettings", UserRole.Permission.MANAGE_SERVER_SETTINGS, SettingsAdminRoutes::saveEmailSettings);
				postRequiringPermissionWithCSRF("/sendTestEmail", UserRole.Permission.MANAGE_SERVER_SETTINGS, SettingsAdminRoutes::sendTestEmail);
				getRequiringPermission("/auditLog", UserRole.Permission.VIEW_SERVER_AUDIT_LOG, SettingsAdminRoutes::auditLog);
				path("/emailRules", ()->{
					getRequiringPermission("", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::emailDomainRules);
					getRequiringPermission("/createForm", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::emailDomainRuleCreateForm);
					postRequiringPermissionWithCSRF("/create", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::emailDomainRuleCreate);
					path("/:domain", ()->{
						getRequiringPermission("/edit", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::emailDomainRuleEdit);
						postRequiringPermissionWithCSRF("/update", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::emailDomainRuleUpdate);
						getRequiringPermission("/confirmDelete", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::emailDomainRuleConfirmDelete);
						postRequiringPermissionWithCSRF("/delete", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::emailDomainRuleDelete);
					});
				});
				path("/ipRules", ()->{
					getRequiringPermission("", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::ipRules);
					getRequiringPermission("/createForm", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::ipRuleCreateForm);
					postRequiringPermissionWithCSRF("/create", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::ipRuleCreate);
					path("/:id", ()->{
						getRequiringPermission("/edit", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::ipRuleEdit);
						postRequiringPermissionWithCSRF("/update", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::ipRuleUpdate);
						getRequiringPermission("/confirmDelete", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::ipRuleConfirmDelete);
						postRequiringPermissionWithCSRF("/delete", UserRole.Permission.MANAGE_BLOCKING_RULES, SettingsAdminRoutes::ipRuleDelete);
					});
				});
				path("/invites", ()->{
					getRequiringPermission("", UserRole.Permission.MANAGE_INVITES, SettingsAdminRoutes::invites);
					path("/:id", ()->{
						getRequiringPermission("/confirmDelete", UserRole.Permission.MANAGE_INVITES, SettingsAdminRoutes::confirmDeleteInvite);
						postRequiringPermissionWithCSRF("/delete", UserRole.Permission.MANAGE_INVITES, SettingsAdminRoutes::deleteInvite);
					});
				});
				path("/fasp", ()->{
					getRequiringPermission("", UserRole.Permission.MANAGE_FASPS, SettingsAdminFaspRoutes::activeFasps);
					getRequiringPermission("/requests", UserRole.Permission.MANAGE_FASPS, SettingsAdminFaspRoutes::faspRequests);
					path("/:id", ()->{
						getRequiringPermission("/confirm", UserRole.Permission.MANAGE_FASPS, SettingsAdminFaspRoutes::confirmFaspRegistration);
						postRequiringPermissionWithCSRF("/confirm", UserRole.Permission.MANAGE_FASPS, SettingsAdminFaspRoutes::doConfirmFaspRegistration);
						getRequiringPermissionWithCSRF("/reject", UserRole.Permission.MANAGE_FASPS, SettingsAdminFaspRoutes::rejectFaspRegistration);
						getRequiringPermission("/capabilities", UserRole.Permission.MANAGE_FASPS, SettingsAdminFaspRoutes::faspCapabilities);
						postRequiringPermissionWithCSRF("/setCapabilities", UserRole.Permission.MANAGE_FASPS, SettingsAdminFaspRoutes::setFaspCapabilities);
						getRequiringPermission("/confirmDelete", UserRole.Permission.MANAGE_FASPS, SettingsAdminFaspRoutes::confirmDeleteFasp);
						postRequiringPermissionWithCSRF("/delete", UserRole.Permission.MANAGE_FASPS, SettingsAdminFaspRoutes::deleteFasp);
						path("/capabilities", ()->{
							getRequiringPermission("/callback", UserRole.Permission.MANAGE_FASPS, SettingsAdminFaspRoutes::faspDebugCallbackLog);
							postRequiringPermissionWithCSRF("/callback/send", UserRole.Permission.MANAGE_FASPS, SettingsAdminFaspRoutes::faspDebugCallback);
						});
					});
				});
			});
		});

		path("/activitypub", ()->{
			post("/sharedInbox", ActivityPubRoutes::sharedInbox);
			get("/sharedInbox", SmithereenApplication::methodNotAllowed);
			getLoggedIn("/externalInteraction", ActivityPubRoutes::externalInteraction);
			get("/nodeinfo/2.0", ActivityPubRoutes::nodeInfo);
			get("/nodeinfo/2.1", ActivityPubRoutes::nodeInfo);
			path("/objects", ()->{
				path("/likes/:likeID", ()->{
					get("", ActivityPubRoutes::likeObject);
					get("/undo", ActivityPubRoutes::undoLikeObject);
				});
				path("/polls/:pollID", ()->{
					getActivityPubCollection("/options/:optionID/votes", 100, ActivityPubRoutes::pollVoters);
				});
			});
			path("/serviceActor", ()->{
				get("", ActivityPubRoutes::serviceActor);
				post("/inbox", ActivityPubRoutes::sharedInbox);
				get("/inbox", SmithereenApplication::methodNotAllowed);
				getActivityPubCollection("/outbox", 1, (req, resp, offset, count)->ActivityPubCollectionPageResponse.forObjects(Collections.emptyList(), 0));
				post("/outbox", SmithereenApplication::methodNotAllowed);
			});
		});

		path("/.well-known", ()->{
			get("/webfinger", WellKnownRoutes::webfinger);
			get("/nodeinfo", WellKnownRoutes::nodeInfo);
		});

		path("/system", ()->{
			get("/downloadExternalMedia", SystemRoutes::downloadExternalMedia);
			path("/upload", ()->{
				postWithCSRF("/postPhoto", SystemRoutes::uploadPostPhoto);
				postWithCSRF("/messagePhoto", SystemRoutes::uploadMessagePhoto);
			});
			get("/about", SystemRoutes::aboutServer);
			getLoggedIn("/qsearch", SystemRoutes::quickSearch);
			postLoggedIn("/loadRemoteObject", SystemRoutes::loadRemoteObject);
			postWithCSRF("/votePoll", SystemRoutes::votePoll);
			getLoggedIn("/reportForm", SystemRoutes::reportForm);
			postWithCSRF("/submitReport", SystemRoutes::submitReport);
			get("/captcha", SystemRoutes::captcha);
			get("/oembed", SystemRoutes::oEmbed);
			post("/redirectForRemoteInteraction", SystemRoutes::redirectForRemoteInteraction);
			getLoggedIn("/mentionCompletions", SystemRoutes::mentionCompletions);
			getLoggedIn("/simpleUserCompletions", SystemRoutes::simpleUserCompletions);
			get("/privacyPolicy", SystemRoutes::privacyPolicy);
			get("/languageChooser", SystemRoutes::languageChooser);

			if(Config.DEBUG){
				path("/debug", ()->{
					get("/deleteAbandonedFilesNow", (req, resp)->{
						MediaStorageUtils.deleteAbandonedFiles();
						return "ok";
					});
				});
			}
		});

		path("/users/:id", ()->{
			get("", (req, resp)->{
				int id=parseIntOrDefault(req.params(":id"), 0);
				User user=UserStorage.getById(id);
				if(user==null || user instanceof ForeignUser){
					throw new ObjectNotFoundException("err_user_not_found");
				}else{
					resp.redirect("/"+user.username);
				}
				return "";
			});
			getActivityPub("", ActivityPubRoutes::userActor);

			post("/inbox", ActivityPubRoutes::userInbox);
			get("/inbox", SmithereenApplication::methodNotAllowed);
			get("/outbox", ActivityPubRoutes::userOutbox);
			post("/outbox", SmithereenApplication::methodNotAllowed);
			getActivityPubCollection("/followers", 100, ActivityPubRoutes::userFollowers);
			getActivityPubCollection("/following", 100, ActivityPubRoutes::userFollowing);
			getActivityPubCollection("/wall", 100, ActivityPubRoutes::userWall);
			getActivityPubCollection("/wallComments", 50, ActivityPubRoutes::userWallComments);
			getActivityPubCollection("/friends", 100, ActivityPubRoutes::userFriends);
			getActivityPubCollection("/groups", 100, ActivityPubRoutes::userGroups);
			post("/collectionQuery", ActivityPubRoutes::userCollectionQuery);

			postWithCSRF("/createWallPost", PostRoutes::createUserWallPost);
			getLoggedIn("/confirmBlock", ProfileRoutes::confirmBlockUser);
			getLoggedIn("/confirmUnblock", ProfileRoutes::confirmUnblockUser);
			postWithCSRF("/block", ProfileRoutes::blockUser);
			postWithCSRF("/unblock", ProfileRoutes::unblockUser);

			get("/groups", GroupsRoutes::userGroups);
			path("/friends", ()->{
				get("", FriendsRoutes::friends);
				get("/online", FriendsRoutes::friendsOnline);
				getLoggedIn("/mutual", FriendsRoutes::mutualFriends);
			});
			path("/wall", ()->{
				get("", PostRoutes::userWallAll);
				get("/own", PostRoutes::userWallOwn);
				get("/with/:otherUserID", PostRoutes::wallToWall);
			});
			get("/followers", FriendsRoutes::followers);
			get("/following", FriendsRoutes::following);
			postWithCSRF("/respondToFriendRequest", FriendsRoutes::respondToFriendRequest);
			getWithCSRF("/respondToFriendRequest", FriendsRoutes::respondToFriendRequest);

			getLoggedIn("/confirmSendFriendRequest", FriendsRoutes::confirmSendFriendRequest);
			postWithCSRF("/doSendFriendRequest", FriendsRoutes::doSendFriendRequest);
			postWithCSRF("/doRemoveFriend", FriendsRoutes::doRemoveFriend);
			getLoggedIn("/confirmRemoveFriend", FriendsRoutes::confirmRemoveFriend);

			getRequiringPermissionWithCSRF("/syncRelCollections", UserRole.Permission.MANAGE_USERS, ProfileRoutes::syncRelationshipsCollections);
			getRequiringPermissionWithCSRF("/syncContentCollections", UserRole.Permission.MANAGE_USERS, ProfileRoutes::syncContentCollections);
			getRequiringPermissionWithCSRF("/syncProfile", UserRole.Permission.MANAGE_USERS, ProfileRoutes::syncProfile);
			getRequiringPermission("/meminfo", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::userInfo);
			getRequiringPermission("/banForm", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::banUserForm);
			postRequiringPermissionWithCSRF("/ban", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::banUser);
			getRequiringPermission("/deleteImmediatelyForm", UserRole.Permission.DELETE_USERS_IMMEDIATE, SettingsAdminRoutes::deleteAccountImmediatelyForm);
			postRequiringPermissionWithCSRF("/deleteImmediately", UserRole.Permission.DELETE_USERS_IMMEDIATE, SettingsAdminRoutes::deleteAccountImmediately);
			getRequiringPermission("/reports", UserRole.Permission.MANAGE_REPORTS, SettingsAdminRoutes::reportsOfUser);
			getRequiringPermission("/reports/authored", UserRole.Permission.MANAGE_REPORTS, SettingsAdminRoutes::reportsByUser);
			getRequiringPermission("/staffNotes", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::userStaffNotes);
			postRequiringPermissionWithCSRF("/addStaffNote", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::userStaffNoteAdd);
			getRequiringPermission("/staffNotes/:noteID/confirmDelete", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::userStaffNoteConfirmDelete);
			postRequiringPermissionWithCSRF("/staffNotes/:noteID/delete", UserRole.Permission.MANAGE_USERS, SettingsAdminRoutes::userStaffNoteDelete);

			get("/hoverCard", ProfileRoutes::mentionHoverCard);

			getWithCSRF("/addBookmark", BookmarksRoutes::addUserBookmark);
			getWithCSRF("/removeBookmark", BookmarksRoutes::removeUserBookmark);

			getActivityPubCollection("/albums", 100, ActivityPubRoutes::userAlbums);
			get("/albums", PhotosRoutes::userAlbums);
			get("/allPhotos", PhotosRoutes::allUserPhotos);
			getActivityPubCollection("/tagged", 100, ActivityPubRoutes::userTaggedPhotos);
			get("/tagged", PhotosRoutes::userTaggedPhotos);

			getWithCSRF("/mute", ProfileRoutes::muteUser);
			getWithCSRF("/unmute", ProfileRoutes::unmuteUser);

			postWithCSRF("/setFriendLists", FriendsRoutes::setUserFriendLists);
			getLoggedIn("/setListsMobileBox", FriendsRoutes::setUserListsMobileBox);
			get("/statuses/:statusID", ActivityPubRoutes::userStatus);
		});

		path("/groups/:id", ()->{
			get("", (req, resp)->{
				int id=parseIntOrDefault(req.params(":id"), 0);
				Group group=GroupStorage.getById(id);
				if(group==null || group instanceof ForeignGroup){
					throw new ObjectNotFoundException("err_group_not_found");
				}else{
					resp.redirect("/"+group.username);
				}
				return "";
			});
			get("", "application/activity+json", ActivityPubRoutes::groupActor);
			get("", "application/ld+json", ActivityPubRoutes::groupActor);

			postWithCSRF("/createWallPost", PostRoutes::createGroupWallPost);

			getWithCSRF("/join", GroupsRoutes::join);
			getWithCSRF("/leave", GroupsRoutes::leave);

			post("/inbox", ActivityPubRoutes::groupInbox);
			get("/inbox", SmithereenApplication::methodNotAllowed);
			getActivityPubCollection("/outbox", 50, ActivityPubRoutes::groupOutbox);
			post("/outbox", SmithereenApplication::methodNotAllowed);
			getActivityPubCollection("/members", 50, ActivityPubRoutes::groupMembers);
			getActivityPubCollection("/tentativeMembers", 50, ActivityPubRoutes::groupTentativeMembers);
			getActivityPubCollection("/wall", 50, ActivityPubRoutes::groupWall);
			getActivityPubCollection("/wallComments", 50, ActivityPubRoutes::groupWallComments);
			get("/actorToken", ActivityPubRoutes::groupActorToken);
			post("/collectionQuery", ActivityPubRoutes::groupCollectionQuery);

			getLoggedIn("/edit", GroupsRoutes::editGeneral);
			postWithCSRF("/saveGeneral", GroupsRoutes::saveGeneral);
			getLoggedIn("/editAdmins", GroupsRoutes::editAdmins);
			getLoggedIn("/editMembers", GroupsRoutes::editMembers);
			getLoggedIn("/editAdminForm", GroupsRoutes::editAdminForm);
			postWithCSRF("/saveAdmin", GroupsRoutes::saveAdmin);
			getLoggedIn("/confirmDemoteAdmin", GroupsRoutes::confirmDemoteAdmin);
			postWithCSRF("/removeAdmin", GroupsRoutes::removeAdmin);
			postWithCSRF("/editAdminReorder", GroupsRoutes::editAdminReorder);

			getLoggedIn("/editBlocking", GroupsRoutes::blocking);
			getLoggedIn("/confirmBlockUser", GroupsRoutes::confirmBlockUser);
			getLoggedIn("/confirmUnblockUser", GroupsRoutes::confirmUnblockUser);
			getLoggedIn("/blockDomainForm", GroupsRoutes::blockDomainForm);
			getLoggedIn("/confirmUnblockDomain", GroupsRoutes::confirmUnblockDomain);
			postWithCSRF("/blockUser", GroupsRoutes::blockUser);
			postWithCSRF("/unblockUser", GroupsRoutes::unblockUser);
			postWithCSRF("/blockDomain", GroupsRoutes::blockDomain);
			postWithCSRF("/unblockDomain", GroupsRoutes::unblockDomain);
			getLoggedIn("/confirmRemoveUser", GroupsRoutes::confirmRemoveUser);
			postWithCSRF("/removeUser", GroupsRoutes::removeUser);
			getLoggedIn("/editJoinRequests", GroupsRoutes::editJoinRequests);
			getWithCSRF("/acceptJoinRequest", GroupsRoutes::acceptJoinRequest);
			getWithCSRF("/rejectJoinRequest", GroupsRoutes::rejectJoinRequest);
			getLoggedIn("/editInvitations", GroupsRoutes::editInvitations);
			getWithCSRF("/cancelInvite", GroupsRoutes::editCancelInvitation);

			get("/members", GroupsRoutes::members);
			get("/tentativeMembers", GroupsRoutes::tentativeMembers);
			get("/admins", GroupsRoutes::admins);
			path("/wall", ()->{
				get("", PostRoutes::groupWall);
			});
			getWithCSRF("/invite", GroupsRoutes::inviteFriend);
			postWithCSRF("/respondToInvite", GroupsRoutes::respondToInvite);

			getRequiringPermissionWithCSRF("/syncRelCollections", UserRole.Permission.MANAGE_GROUPS, GroupsRoutes::syncRelationshipsCollections);
			getRequiringPermissionWithCSRF("/syncContentCollections", UserRole.Permission.MANAGE_GROUPS, GroupsRoutes::syncContentCollections);
			getRequiringPermissionWithCSRF("/syncProfile", UserRole.Permission.MANAGE_GROUPS, GroupsRoutes::syncProfile);

			getWithCSRF("/addBookmark", BookmarksRoutes::addGroupBookmark);
			getWithCSRF("/removeBookmark", BookmarksRoutes::removeGroupBookmark);

			getActivityPubCollection("/albums", 100, ActivityPubRoutes::groupAlbums);
			get("/albums", PhotosRoutes::groupAlbums);
			get("/allPhotos", PhotosRoutes::allGroupPhotos);
			get("/statuses/:statusID", ActivityPubRoutes::groupStatus);
			postWithCSRF("/updateStatus", GroupsRoutes::updateGroupStatus);

			getLoggedIn("/createTopic", BoardRoutes::createTopicForm);
			postWithCSRF("/createTopic", BoardRoutes::createTopic);
			get("/board", BoardRoutes::groupTopics);
		});

		path("/posts/:postID", ()->{
			get("", PostRoutes::standalonePost);
			getActivityPub("", ActivityPubRoutes::post);
			get("/activityCreate", ActivityPubRoutes::postCreateActivity);
			get("/quoteAuth/:quoteID", ActivityPubRoutes::postQuoteAuthorization);

			getLoggedIn("/confirmDelete", PostRoutes::confirmDelete);
			postWithCSRF("/delete", PostRoutes::delete);

			get("/like", PostRoutes::like);
			getWithCSRF("/unlike", PostRoutes::unlike);
			get("/likePopover", PostRoutes::likePopover);
			get("/likes", PostRoutes::likeList);
			get("/ajaxCommentPreview", PostRoutes::ajaxCommentPreview);
			get("/ajaxCommentBranch", PostRoutes::ajaxCommentBranch);
			get("/sharePopover", PostRoutes::sharePopover);
			get("/share", PostRoutes::repostForm);
			get("/reposts", PostRoutes::repostList);
			get("/embedBox", PostRoutes::embedBox);

			getActivityPubCollection("/replies", 50, ActivityPubRoutes::postReplies);
			getActivityPubCollection("/likes", 50, ActivityPubRoutes::postLikes);

			get("/pollVoters/:optionID", PostRoutes::pollOptionVoters);
			get("/pollVoters/:optionID/popover", PostRoutes::pollOptionVotersPopover);
			getLoggedIn("/edit", PostRoutes::editPostForm);
			postWithCSRF("/edit", PostRoutes::editPost);
			get("/embedURL", PostRoutes::postEmbedURL);
			options("/embedURL", SmithereenApplication::allowCorsPreflight);
			get("/embed", PostRoutes::postEmbed);
			get("/hoverCard", PostRoutes::commentHoverCard);
			get("/layerPrevComments", PostRoutes::ajaxLayerPrevComments);
		});

		path("/albums/:id", ()->{
			getActivityPubCollection("", 100, ActivityPubRoutes::photoAlbum);
			get("", PhotosRoutes::album);
			postWithCSRF("/upload", PhotosRoutes::uploadPhoto);
			getLoggedIn("/edit", PhotosRoutes::editAlbumForm);
			postWithCSRF("/edit", PhotosRoutes::editAlbum);
			getLoggedIn("/confirmDelete", PhotosRoutes::confirmDeleteAlbum);
			postWithCSRF("/delete", PhotosRoutes::deleteAlbum);
			getActivityPubCollection("/comments", 50, ActivityPubRoutes::photoAlbumComments);
		});

		path("/photos", ()->{
			get("/ajaxViewerInfo", PhotosRoutes::ajaxViewerInfo);
			getRequiringPermission("/ajaxViewerInfoForReport", UserRole.Permission.MANAGE_REPORTS, PhotosRoutes::ajaxViewerInfoForReport);
			getWithCSRF("/saveAttachmentToAlbum", PhotosRoutes::saveAttachmentToAlbum);
			getLoggedIn("/attachBox", PhotosRoutes::attachPhotosBox);
			getLoggedIn("/attachBoxAll", PhotosRoutes::attachPhotosBoxAll);
			getLoggedIn("/attachBoxAlbum", PhotosRoutes::attachPhotosBoxAlbum);
			getLoggedIn("/friendListForTagging", PhotosRoutes::getFriendsForTagging);
			getLoggedIn("/newTags", PhotosRoutes::newTags);
			path("/:id", ()->{
				getActivityPub("", ActivityPubRoutes::photo);
				get("", PhotosRoutes::photo);
				postWithCSRF("/updateDescription", PhotosRoutes::updatePhotoDescription);
				getLoggedIn("/confirmDelete", PhotosRoutes::confirmDeletePhoto);
				postWithCSRF("/delete", PhotosRoutes::deletePhoto);
				get("/like", PhotosRoutes::like);
				getWithCSRF("/unlike", PhotosRoutes::unlike);
				get("/likes", PhotosRoutes::likeList);
				get("/likePopover", PhotosRoutes::likePopover);
				getWithCSRF("/setAsAlbumCover", PhotosRoutes::setPhotoAsAlbumCover);
				getActivityPubCollection("/replies", 50, ActivityPubRoutes::photoComments);
				getActivityPubCollection("/likes", 100, ActivityPubRoutes::photoLikes);
				getLoggedIn("/ajaxEditDescription", PhotosRoutes::ajaxEditDescription);
				getWithCSRF("/saveToAlbum", PhotosRoutes::saveToAlbum);
				getWithCSRF("/rotate", PhotosRoutes::rotatePhoto);
				postWithCSRF("/updateAvatarCrop", PhotosRoutes::updateAvatarCrop);
				getWithCSRF("/addTag", PhotosRoutes::addTag);
				postWithCSRF("/deleteTag", PhotosRoutes::deleteTag);
				getWithCSRF("/deleteTag", PhotosRoutes::deleteTag);
				getWithCSRF("/approveTag", PhotosRoutes::approveTag);
			});
		});

		path("/comments", ()->{
			postWithCSRF("/createComment", CommentsRoutes::createComment);
			get("/ajaxCommentPreview", CommentsRoutes::ajaxCommentPreview);
			path("/:id", ()->{
				get("", CommentsRoutes::comment);
				getActivityPub("", ActivityPubRoutes::comment);
				getActivityPubCollection("/replies", 50, ActivityPubRoutes::commentReplies);
				getActivityPubCollection("/likes", 100, ActivityPubRoutes::commentLikes);
				get("/ajaxCommentBranch", CommentsRoutes::ajaxCommentBranch);
				getLoggedIn("/confirmDelete", CommentsRoutes::confirmDeleteComment);
				postWithCSRF("/delete", CommentsRoutes::deleteComment);
				getLoggedIn("/edit", CommentsRoutes::editCommentForm);
				postWithCSRF("/edit", CommentsRoutes::editComment);
				get("/hoverCard", CommentsRoutes::commentHoverCard);
				get("/like", CommentsRoutes::like);
				getWithCSRF("/unlike", CommentsRoutes::unlike);
				get("/likes", CommentsRoutes::likeList);
				get("/likePopover", CommentsRoutes::likePopover);
			});
		});

		path("/topics/:id", ()->{
			get("", BoardRoutes::topic);
			postWithCSRF("/delete", BoardRoutes::deleteTopic);
			getLoggedIn("/renameForm", BoardRoutes::renameTopicForm);
			postWithCSRF("/rename", BoardRoutes::renameTopic);
			postWithCSRF("/open", BoardRoutes::openTopic);
			postWithCSRF("/close", BoardRoutes::closeTopic);
			postWithCSRF("/pin", BoardRoutes::pinTopic);
			postWithCSRF("/unpin", BoardRoutes::unpinTopic);
		});

		path("/my", ()->{
			getLoggedIn("/incomingFriendRequests", FriendsRoutes::incomingFriendRequests);
			path("/friends", ()->{
				getLoggedIn("", FriendsRoutes::ownFriends);
				postWithCSRF("/createList", FriendsRoutes::createFriendList);
				getLoggedIn("/confirmDeleteList", FriendsRoutes::confirmDeleteFriendList);
				postWithCSRF("/deleteList", FriendsRoutes::deleteFriendList);
				getLoggedIn("/ajaxListUserIDs", FriendsRoutes::ajaxFriendListMemberIDs);
				postWithCSRF("/updateList", FriendsRoutes::updateFriendList);
			});
			get("/followers", FriendsRoutes::followers);
			get("/following", FriendsRoutes::following);
			getLoggedIn("/notifications", NotificationsRoutes::notifications);
			postWithCSRF("/notifications/ajaxReadLast", NotificationsRoutes::ajaxReadLastNotifications);
			path("/groups", ()->{
				getLoggedIn("", GroupsRoutes::myGroups);
				getLoggedIn("/managed", GroupsRoutes::myManagedGroups);
				getLoggedIn("/create", GroupsRoutes::createGroup);
				postWithCSRF("/create", GroupsRoutes::doCreateGroup);
				getLoggedIn("/invites", GroupsRoutes::groupInvitations);
			});
			path("/events", ()->{
				getLoggedIn("", GroupsRoutes::myEvents);
				getLoggedIn("/past", GroupsRoutes::myPastEvents);
				getLoggedIn("/create", GroupsRoutes::createEvent);
				getLoggedIn("/calendar", GroupsRoutes::eventCalendar);
				getLoggedIn("/dayEventsPopup", GroupsRoutes::eventCalendarDayPopup);
				getLoggedIn("/invites", GroupsRoutes::eventInvitations);
			});
			getLoggedIn("/friends/ajaxFriendsForPrivacyBoxes", FriendsRoutes::ajaxFriendsForPrivacyBoxes);
			path("/mail", ()->{
				getLoggedIn("", MailRoutes::inbox);
				getLoggedIn("/outbox", MailRoutes::outbox);
				getLoggedIn("/compose", MailRoutes::compose);
				postWithCSRF("/send", MailRoutes::sendMessage);
				getLoggedIn("/history", MailRoutes::history);
				path("/messages/:id", ()->{
					Filter idParserFilter=(req, resp)->{
						long id=decodeLong(req.params(":id"));
						if(id==0)
							throw new ObjectNotFoundException();
						req.attribute("id", id);
					};
					before("", idParserFilter);
					before("/*", idParserFilter);
					getLoggedIn("", MailRoutes::viewMessage);
					getWithCSRF("/delete", MailRoutes::delete);
					getWithCSRF("/deleteForEveryone", MailRoutes::deleteForEveryone);
					getWithCSRF("/restore", MailRoutes::restore);
				});
			});
			path("/bookmarks", ()->{
				getLoggedIn("", BookmarksRoutes::users);
				getLoggedIn("/groups", BookmarksRoutes::groups);
				getLoggedIn("/posts", BookmarksRoutes::posts);
				getLoggedIn("/photos", BookmarksRoutes::photos);
			});
			getLoggedIn("/albums", PhotosRoutes::myAlbums);
			getLoggedIn("/albums/create", PhotosRoutes::createAlbumForm);
			postWithCSRF("/albums/create", PhotosRoutes::createAlbum);
		});

		path("/api", ()->{
			path("/v1", ()->{ // Mastodon API compatibility for crawlers
				getApi("/instance", MastodonApiRoutes::instance);
				getApi("/instance/peers", MastodonApiRoutes::instancePeers);

				before("/*", (req, resp)->resp.type("application/json"));
			});
			path("/fasp", ()->{
				post("/registration", FaspApiRoutes::registration);
				postFaspAPI("/debug/v0/callback/responses", FASPCapability.DEBUG, FaspApiRoutes::debugCallback);
			});
		});

		get("/healthz", (req, resp)->"");

		path("/:username", ()->{
			get("", ProfileRoutes::profile);
			// These also handle groups
			getActivityPub("", ActivityPubRoutes::userActor);

			postWithCSRF("/remoteFollow", ActivityPubRoutes::remoteFollow);
		});


		exception(ObjectNotFoundException.class, (x, req, resp)->{
			resp.status(404);
			resp.body(wrapErrorString(req, resp, Objects.requireNonNullElse(x.getMessage(), "err_not_found")));
		});
		exception(UserActionNotAllowedException.class, (x, req, resp)->{
			if(Config.DEBUG)
				LOG.warn("403: {}", req.pathInfo(), x);
			resp.status(403);
			String key;
			if(x instanceof UserContentUnavailableException){
				key="err_access_user_content";
			}else{
				key="err_access";
			}
			resp.body(wrapErrorString(req, resp, Objects.requireNonNullElse(x.getMessage(), key)));
		});
		exception(BadRequestException.class, (x, req, resp)->{
			if(Config.DEBUG)
				LOG.warn("400: {}", req.pathInfo(), x);
			resp.status(400);
			String msg=x.getMessage();
			if(StringUtils.isNotEmpty(msg))
				resp.body("Bad request: "+msg.replace("<", "&lt;"));
			else
				resp.body("Bad request");
		});
		exception(FloodControlViolationException.class, (x, req, resp)->{
			resp.status(429);
			resp.body(wrapErrorString(req, resp, Objects.requireNonNullElse(x.getMessage(), "err_flood_control")));
		});
		exception(UserErrorException.class, (x, req, resp)->{
			resp.body(wrapErrorString(req, resp, x.getMessage(), x.langArgs, x.includeCauseMessage && x.getCause()!=null ? x.getCause().getMessage() : null));
		});
		exception(InaccessibleProfileException.class, (x, req, resp)->{
			RenderedTemplateResponse model=new RenderedTemplateResponse("hidden_profile", req);
			model.with("user", x.user);
			resp.body(model.renderToString());
		});
		exception(UnauthorizedRequestException.class, (x, req, resp)->{
			if(Config.DEBUG)
				LOG.warn("401: {}", req.pathInfo(), x);
			resp.status(401);
			resp.body(x.getMessage()==null ? "Unauthorized" : x.getMessage());
		});
		exception(Exception.class, (exception, req, res) -> {
			String path=req.raw().getPathInfo();
			LOG.warn("Exception while processing {} {}", req.requestMethod(), path, exception);
			if(req.requestMethod().equals("POST") && (path.equals("/activitypub/sharedInbox") || path.endsWith("/inbox"))){
				LOG.warn("Failed activity: {}", req.body());
			}
			res.status(500);
			StringWriter sw=new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));
			res.body("<h1 style='color: red;'>Unhandled exception</h1><pre>"+sw.toString().replace("<", "&lt;")+"</pre>");
		});

		after((req, resp)->{
			if(req.attribute("isTemplate")!=null && req.attribute("noPreload")==null && !isAjax(req) && !isAjaxLayout(req)){
				String cssName=req.attribute("mobile")!=null ? "mobile.css" : "desktop.css";
				resp.header("Link", "</res/"+cssName+"?"+Templates.getStaticFileVersion(cssName)+">; rel=preload; as=style, </res/common.js?"+Templates.getStaticFileVersion("common.js")+">; rel=preload; as=script");
				resp.header("Vary", "User-Agent, Accept-Language");
				resp.header("X-Powered-By", "frustration with attention economy");
			}

			if(req.attribute("isTemplate")!=null){
				try{
					if(req.session().attribute("info")==null)
						req.session().attribute("info", new SessionInfo());

					SessionInfo info=req.session().attribute("info");
					if(info.account!=null){
						context(req).getUsersController().setOnline(info.account.user, isMobile(req) ? UserPresence.PresenceType.MOBILE_WEB : UserPresence.PresenceType.WEB, req.cookie("psid").hashCode());
					}

					if(req.requestMethod().equalsIgnoreCase("get") && req.attribute("noHistory")==null){
						String path=req.pathInfo();
						String query=req.raw().getQueryString();
						if(StringUtils.isNotEmpty(query)){
							path+='?'+query;
						}
						info.history.add(path);
					}
				}catch(Throwable ignore){}
			}
		});

		if(Config.DEBUG){
			afterAfter((req, resp)->{
				DebugLog dl=DebugLog.get();
				LOG.info("{}: total {}ms, route match {}ms, {} DB queries {}ms", req.pathInfo(), dl.getDuration()/1000_000.0, (dl.routeMatchTime-dl.startTime)/1000_000.0, dl.numDatabaseQueries, dl.totalDatabaseQueryDuration/1000_000.0);
			});
		}

		notFound((req, resp)->{
			if(isAjax(req))
				return lang(req).get("page_not_found");
			return notFoundPages.get(lang(req).getLocale().toLanguageTag());
		});
		internalServerError((req, resp)->{
			if(isAjax(req))
				return lang(req).get("server_error");
			return serverErrorPages.get(lang(req).getLocale().toLanguageTag());
		});

		awaitInitialization();
		setupCustomSerializer();

		responseTypeSerializer(ActivityPubObject.class, (out, obj, req, resp) -> {
			resp.raw().setCharacterEncoding(null);
			resp.type(ActivityPub.CONTENT_TYPE);
			OutputStreamWriter writer=new OutputStreamWriter(out, StandardCharsets.UTF_8);
			gson.toJson(obj.asRootActivityPubObject(context(req), ()->{
				if(req.headers("signature")!=null){
					try{
						Actor requester=ActivityPub.verifyHttpSignature(req, null);
						context(req).getObjectLinkResolver().storeOrUpdateRemoteObject(requester, requester);
						String requesterDomain=requester.domain;
						LOG.trace("Requester domain for {} is {}", req.pathInfo(), requesterDomain);
						return requesterDomain;
					}catch(Exception x){
						LOG.trace("Exception while verifying HTTP signature for {}", req.pathInfo(), x);
					}
				}
				return null;
			}), writer);
			writer.flush();
		});

		responseTypeSerializer(RenderedTemplateResponse.class, (out, obj, req, resp) -> {
			OutputStreamWriter writer=new OutputStreamWriter(out, StandardCharsets.UTF_8);
			if(req.queryParams("_al")!=null && !isMobile(req)){
				// TODO figure out how to stream these
				String js=obj.renderBlock("bottomScripts");
				Set<String> k=req.attribute("jsLang");
				if(k!=null){
					Lang l=lang(req);
					js="addLang({"+k.stream().map(key->"\""+key+"\":"+l.getAsJS(key)).collect(Collectors.joining(","))+"});\n"+js;
				}
				if(obj.get("headerBackHref") instanceof String headerBackHref && !headerBackHref.isEmpty()){
					js+="\nshowHeaderBack(\""+TextProcessor.escapeJS(headerBackHref)+"\", \""+TextProcessor.escapeJS((String) obj.get("headerBackTitle"))+"\");";
				}else{
					js+="\nhideHeaderBack();";
				}

				JsonObjectBuilder alResp=new JsonObjectBuilder()
						.add("h", obj.renderBlock("outerContent"))
						.add("s", js)
						.add("t", (String) obj.get("title"));
				String redirURL=req.attribute("alFinalURL");
				if(StringUtils.isNotEmpty(redirURL)){
					alResp.add("url", redirURL);
				}
				SessionInfo info=sessionInfo(req);
				if(info!=null && info.account!=null){
					alResp.add("c", context(req).getNotificationsController().getUserCountersJson(info.account));
				}
				Set<String> extraScriptFiles=req.attribute("extraScriptFiles");
				if(extraScriptFiles!=null){
					JsonObjectBuilder scripts=new JsonObjectBuilder();
					for(String name:extraScriptFiles){
						scripts.add(name, Templates.staticHashes.get(name));
					}
					alResp.add("sc", scripts);
				}
				resp.header("Content-Type", "application/json");
				gson.toJson(alResp.build(), writer);
			}else{
				obj.renderToWriter(writer);
			}
			writer.flush();
		});

		responseTypeSerializer(WebDeltaResponse.class, (out, obj, req, resp) -> {
			OutputStreamWriter writer=new OutputStreamWriter(out, StandardCharsets.UTF_8);
			gson.toJson(obj.commands(), writer);
			writer.flush();
		});

		addServletEventListener(new HttpSessionListener(){
			@Override
			public void sessionDestroyed(HttpSessionEvent se){
				synchronized(SmithereenApplication.class){
					String sid=se.getSession().getId();
					int accountID=accountIdsBySession.getOrDefault(sid, 0);
					if(accountID==0)
						return;
					Set<HttpSession> sessions=sessionsByAccount.get(accountID);
					if(sessions==null)
						return;
					sessions.remove(se.getSession());
					if(sessions.isEmpty())
						sessionsByAccount.remove(accountID);
				}
			}
		});

		MaintenanceScheduler.runDaily(()->{
			try{
				SessionStorage.deleteExpiredEmailCodes();
			}catch(SQLException ignore){}
			FloodControl.PASSWORD_RESET.gc();
			TopLevelDomainList.updateIfNeeded();
			PublicSuffixList.updateIfNeeded();
		});
		MaintenanceScheduler.runPeriodically(DatabaseConnectionManager::closeUnusedConnections, 10, TimeUnit.MINUTES);
		MaintenanceScheduler.runPeriodically(MailController::deleteRestorableMessages, 1, TimeUnit.HOURS);
		MaintenanceScheduler.runPeriodically(MediaStorageUtils::deleteAbandonedFiles, 1, TimeUnit.HOURS);
		MaintenanceScheduler.runPeriodically(()->UsersController.doPendingAccountDeletions(context), 1, TimeUnit.DAYS);
		context.getUsersController().loadPresenceFromDatabase();

		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			context.getFriendsController().doPendingHintsUpdates();
			context.getGroupsController().doPendingHintsUpdates();
			LOG.info("Stopping Spark");
			awaitStop();
			LOG.info("Stopped Spark");
			// These try-catch blocks are needed because these classes might not have been loaded by the time the process is shut down,
			// and the JVM refuses to load any new classes from within a shutdown hook.
			try{
				context.getActivityPubWorker().shutDown();
			}catch(NoClassDefFoundError ignore){}
			try{
				MaintenanceScheduler.shutDown();
			}catch(NoClassDefFoundError ignore){}
			try{
				BackgroundTaskRunner.shutDown();
			}catch(NoClassDefFoundError ignore){}
			// Set the exit code to 0 so systemd doesn't say "Failed with result 'exit-code'".
			Runtime.getRuntime().halt(0);
		}));
	}

	private static Object indexPage(Request req, Response resp){
		SessionInfo info=sessionInfo(req);
		if(info!=null && info.account!=null){
			resp.redirect("/feed");
			return "";
		}
		Config.SignupMode signupMode=context(req).getModerationController().getEffectiveSignupMode(req);
		RenderedTemplateResponse model=new RenderedTemplateResponse("index", req).with("title", Config.serverDisplayName)
				.with("signupMode", signupMode)
				.with("serverDisplayName", Config.serverDisplayName)
				.with("serverDescription", Config.serverDescription)
				.addNavBarItem(lang(req).get("index_welcome"));
		if((signupMode==Config.SignupMode.OPEN || signupMode==Config.SignupMode.MANUAL_APPROVAL) && Config.signupFormUseCaptcha){
			model.with("captchaSid", randomAlphanumericString(16));
		}
		return model;
	}

	private static Object methodNotAllowed(Request req, Response resp){
		resp.status(405);
		return "";
	}

	private static Object allowCorsPreflight(Request req, Response resp){
		resp.status(204);
		resp.header("Access-Control-Allow-Origin", "*");
		return resp;
	}

	private static void setupCustomSerializer(){
		getSerializerChain().insertBeforeRoot(new ExtendedStreamingSerializer());
	}

	private static boolean isAllowedForRestrictedAccounts(Request req){
		String path=req.pathInfo();
		return Set.of(
				"/account/logout",
				"/account/resendConfirmationEmail",
				"/account/changeEmailForm",
				"/account/changeEmail",
				"/account/activate",
				"/account/unfreezeBox",
				"/account/unfreeze",
				"/account/unfreezeChangePassword",
				"/account/reactivateBox",
				"/account/reactivate",
				"/system/languageChooser",
				"/settings/setLanguage",
				"/system/privacyPolicy",
				"/system/about"
		).contains(path);
	}

	private static boolean isAllowedForMovedAccounts(Request req){
		String path=req.pathInfo();
		return Set.of(
				"/settings/deactivateAccountForm",
				"/settings/deactivateAccount",
				"/settings/confirmRemoveMoveRedirect",
				"/settings/removeMoveRedirect"
		).contains(path);
	}

	private static void enforceAccountLimitationsIfAny(Request req, Response resp){
		if(isAllowedForRestrictedAccounts(req))
			return;
		SessionInfo info=sessionInfo(req);
		if(info!=null && info.account!=null){
			Account acc=info.account;
			// Mandatory email confirmation
			if(acc.activationInfo!=null && acc.activationInfo.emailState==Account.ActivationInfo.EmailConfirmationState.NOT_CONFIRMED){
				Lang l=lang(req);
				halt(new RenderedTemplateResponse("email_confirm_required", req).with("email", acc.email).pageTitle(l.get("account_activation")).renderToString());
				return;
			}
			// Account ban or self-deactivation
			UserBanStatus status=info.account.user.banStatus;
			if(status!=UserBanStatus.NONE && status!=UserBanStatus.HIDDEN){
				Lang l=lang(req);
				RenderedTemplateResponse model=new RenderedTemplateResponse("account_banned", req)
						.with("noLeftMenu", true);
				model.pageTitle(l.get(switch(status){
					case FROZEN -> "account_frozen";
					case SUSPENDED -> "account_suspended";
					case SELF_DEACTIVATED -> "account_deactivated";
					default -> throw new IllegalStateException("Unexpected value: "+status);
				}));
				model.with("status", status).with("banInfo", acc.user.banInfo).with("contactEmail", Config.serverAdminEmail);
				switch(status){
					case FROZEN -> {
						if(acc.user.banInfo.expiresAt()!=null && acc.user.banInfo.expiresAt().isAfter(Instant.now())){
							model.with("unfreezeTime", acc.user.banInfo.expiresAt());
						}
					}
					case SUSPENDED, SELF_DEACTIVATED -> model.with("deletionTime", acc.user.banInfo.bannedAt().plus(UserBanInfo.ACCOUNT_DELETION_DAYS, ChronoUnit.DAYS));
				}
				halt(model.renderToString());
				return;
			}
			// Account moved
			if(acc.user.movedTo>0){
				if(isAllowedForMovedAccounts(req))
					return;
				boolean isSuperuser=acc.roleID>0 && Config.userRoles.get(acc.roleID).permissions().contains(UserRole.Permission.SUPERUSER);
				if(isSuperuser && !req.pathInfo().equals("/"+acc.user.username))
					return;
				Lang l=lang(req);
				RenderedTemplateResponse model=new RenderedTemplateResponse("account_moved", req)
						.with("noLeftMenu", !isSuperuser)
						.with("pageTitle", l.get("account_deactivated_redirect_title"))
						.with("movedTo", context(req).getUsersController().getUserOrThrow(acc.user.movedTo));
				halt(model.renderToString());
				return;
			}
		}
	}

	public static synchronized void invalidateAllSessionsForAccount(int id){
		Set<HttpSession> sessions=sessionsByAccount.get(id);
		if(sessions==null)
			return;
		for(HttpSession session:new HashSet<>(sessions)){
			session.invalidate();
		}
	}

	public static synchronized void addAccountSession(int accountID, Request req){
		accountIdsBySession.put(req.session().id(), accountID);
		sessionsByAccount.computeIfAbsent(accountID, HashSet::new).add(req.session().raw());
	}

	private static void prerenderErrorPages(){
		LOG.debug("Pre-rendering error pages");
		try{
			PebbleTemplate template=Templates.getTemplate("error_page");
			for(Lang lang:Lang.list){
				Locale locale=lang.getLocale();

				StringWriter writer=new StringWriter();
				template.evaluate(writer, Map.of(
						"serverName", Config.serverDisplayName==null ? Config.domain : Config.serverDisplayName,
						"errorCode", 404,
						"errorLangKey", "page_not_found",
						"locale", locale
				), locale);
				notFoundPages.put(locale.toLanguageTag(), writer.toString());

				writer=new StringWriter();
				template.evaluate(writer, Map.of(
						"serverName", Config.serverDisplayName==null ? Config.domain : Config.serverDisplayName,
						"errorCode", 500,
						"errorLangKey", "internal_server_error",
						"locale", locale
				), locale);
				serverErrorPages.put(locale.toLanguageTag(), writer.toString());
			}
		}catch(IOException x){
			throw new InternalServerErrorException(x);
		}
	}
}
