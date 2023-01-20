package smithereen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.data.Account;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.SessionInfo;
import smithereen.data.User;
import smithereen.data.WebDeltaResponse;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.FloodControlViolationException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.exceptions.UserErrorException;
import smithereen.routes.ActivityPubRoutes;
import smithereen.routes.ApiRoutes;
import smithereen.routes.GroupsRoutes;
import smithereen.routes.NotificationsRoutes;
import smithereen.routes.PostRoutes;
import smithereen.routes.ProfileRoutes;
import smithereen.routes.SessionRoutes;
import smithereen.routes.SettingsAdminRoutes;
import smithereen.routes.SystemRoutes;
import smithereen.routes.WellKnownRoutes;
import smithereen.sparkext.ActivityPubCollectionPageResponse;
import smithereen.sparkext.ExtendedStreamingSerializer;
import smithereen.storage.DatabaseSchemaUpdater;
import smithereen.storage.GroupStorage;
import smithereen.storage.SessionStorage;
import smithereen.routes.SettingsRoutes;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import smithereen.templates.Templates;
import smithereen.util.BackgroundTaskRunner;
import smithereen.util.FloodControl;
import smithereen.util.MaintenanceScheduler;
import smithereen.util.TopLevelDomainList;
import spark.Request;
import spark.Response;
import spark.Service;
import spark.Spark;
import spark.embeddedserver.jetty.EmbeddedJettyServer;
import spark.embeddedserver.jetty.JettyHandler;
import spark.http.matching.MatcherFilter;
import spark.serialization.Serializer;
import spark.serialization.SerializerChain;
import spark.utils.StringUtils;

import static spark.Spark.*;
import static smithereen.sparkext.SparkExtension.*;

public class SmithereenApplication{
	private static final Logger LOG;
	private static final ApplicationContext context;

	static{
		System.setProperty("org.slf4j.simpleLogger.logFile", "System.out");
		System.setProperty("org.slf4j.simpleLogger.showShortLogName", "true");
		if(Config.DEBUG)
			System.setProperty("org.slf4j.simpleLogger.log.smithereen", "trace");
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
			return;
		}

		ActivityPubRoutes.registerActivityHandlers();

		ipAddress(Config.serverIP);
		port(Config.serverPort);
		if(Config.staticFilesPath!=null)
			externalStaticFileLocation(Config.staticFilesPath);
		else
			staticFileLocation("/public");
		staticFiles.expireTime(7*24*60*60);
		before((request, response) -> {
			request.attribute("context", context);

			if(request.pathInfo().startsWith("/api/"))
				return;
			request.attribute("start_time", System.currentTimeMillis());
			if(request.session(false)==null || request.session().attribute("info")==null){
				String psid=request.cookie("psid");
				if(psid!=null){
					if(!SessionStorage.fillSession(psid, request.session(true), request)){
						response.removeCookie("/", "psid");
					}else{
						response.cookie("/", "psid", psid, 10*365*24*60*60, false);
					}
				}
			}
			SessionInfo info=Utils.sessionInfo(request);
			if(info!=null && info.account!=null){
				info.account=UserStorage.getAccount(info.account.id);
				info.permissions=SessionStorage.getUserPermissions(info.account);

				if(System.currentTimeMillis()-info.account.lastActive.toEpochMilli()>=10*60*1000){
					info.account.lastActive=Instant.now();
					SessionStorage.setLastActive(info.account.id, request.cookie("psid"), info.account.lastActive);
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
			if(StringUtils.isNotEmpty(ua) && Utils.isMobileUserAgent(ua)){
				request.attribute("mobile", Boolean.TRUE);
			}
		});

		get("/", SmithereenApplication::indexPage);

		path("/feed", ()->{
			getLoggedIn("", PostRoutes::feed);
			getLoggedIn("/comments", PostRoutes::commentsFeed);
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
		});

		path("/settings", ()->{
			path("/profile", ()->{
				getLoggedIn("/general", SettingsRoutes::profileEditGeneral);
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

			path("/admin", ()->{
				getRequiringAccessLevel("", Account.AccessLevel.ADMIN, SettingsAdminRoutes::index);
				postRequiringAccessLevelWithCSRF("/updateServerInfo", Account.AccessLevel.ADMIN, SettingsAdminRoutes::updateServerInfo);
				getRequiringAccessLevel("/users", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::users);
				getRequiringAccessLevel("/users/accessLevelForm", Account.AccessLevel.ADMIN, SettingsAdminRoutes::accessLevelForm);
				postRequiringAccessLevelWithCSRF("/users/setAccessLevel", Account.AccessLevel.ADMIN, SettingsAdminRoutes::setUserAccessLevel);
				getRequiringAccessLevel("/other", Account.AccessLevel.ADMIN, SettingsAdminRoutes::otherSettings);
				postRequiringAccessLevelWithCSRF("/updateEmailSettings", Account.AccessLevel.ADMIN, SettingsAdminRoutes::saveEmailSettings);
				postRequiringAccessLevelWithCSRF("/sendTestEmail", Account.AccessLevel.ADMIN, SettingsAdminRoutes::sendTestEmail);
				getRequiringAccessLevel("/users/banForm", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::banUserForm);
				getRequiringAccessLevel("/users/confirmUnban", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::confirmUnbanUser);
				getRequiringAccessLevel("/users/confirmActivate", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::confirmActivateAccount);
				postRequiringAccessLevelWithCSRF("/users/ban", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::banUser);
				postRequiringAccessLevelWithCSRF("/users/unban", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::unbanUser);
				postRequiringAccessLevelWithCSRF("/users/activate", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::activateAccount);
				getRequiringAccessLevel("/signupRequests", Account.AccessLevel.ADMIN, SettingsAdminRoutes::signupRequests);
				postRequiringAccessLevelWithCSRF("/signupRequests/:id/respond", Account.AccessLevel.ADMIN, SettingsAdminRoutes::respondToSignupRequest);
				getRequiringAccessLevel("/reports", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::reportsList);
				postRequiringAccessLevelWithCSRF("/reports/:id", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::reportAction);
				postRequiringAccessLevelWithCSRF("/reports/:id/doAddCW", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::reportAddCW);
				getRequiringAccessLevel("/federation", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::federationServerList);
				getRequiringAccessLevel("/federation/:domain", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::federationServerDetails);
				getRequiringAccessLevel("/federation/:domain/restrictionForm", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::federationServerRestrictionForm);
				postRequiringAccessLevelWithCSRF("/federation/:domain/restrict", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::federationRestrictServer);
				getRequiringAccessLevelWithCSRF("/federation/:domain/resetAvailability", Account.AccessLevel.MODERATOR, SettingsAdminRoutes::federationResetServerAvailability);
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
			getWithCSRF("/deleteDraftAttachment", SystemRoutes::deleteDraftAttachment);
			path("/upload", ()->{
				postWithCSRF("/postPhoto", SystemRoutes::uploadPostPhoto);
			});
			get("/about", SystemRoutes::aboutServer);
			getLoggedIn("/qsearch", SystemRoutes::quickSearch);
			postLoggedIn("/loadRemoteObject", SystemRoutes::loadRemoteObject);
			postWithCSRF("/votePoll", SystemRoutes::votePoll);
			getLoggedIn("/reportForm", SystemRoutes::reportForm);
			postWithCSRF("/submitReport", SystemRoutes::submitReport);
		});

		path("/users/:id", ()->{
			getActivityPub("", ActivityPubRoutes::userActor);
			get("", (req, resp)->{
				int id=Utils.parseIntOrDefault(req.params(":id"), 0);
				User user=UserStorage.getById(id);
				if(user==null || user instanceof ForeignUser){
					throw new ObjectNotFoundException("err_user_not_found");
				}else{
					resp.redirect("/"+user.username);
				}
				return "";
			});

			post("/inbox", ActivityPubRoutes::userInbox);
			get("/inbox", SmithereenApplication::methodNotAllowed);
			get("/outbox", ActivityPubRoutes::userOutbox);
			post("/outbox", SmithereenApplication::methodNotAllowed);
			getActivityPubCollection("/followers", 100, ActivityPubRoutes::userFollowers);
			getActivityPubCollection("/following", 100, ActivityPubRoutes::userFollowing);
			getActivityPubCollection("/wall", 100, ActivityPubRoutes::userWall);
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
				get("", ProfileRoutes::friends);
				getLoggedIn("/mutual", ProfileRoutes::mutualFriends);
			});
			path("/wall", ()->{
				get("", PostRoutes::userWallAll);
				get("/own", PostRoutes::userWallOwn);
				get("/with/:otherUserID", PostRoutes::wallToWall);
			});
			get("/followers", ProfileRoutes::followers);
			get("/following", ProfileRoutes::following);
			postWithCSRF("/respondToFriendRequest", ProfileRoutes::respondToFriendRequest);
			getWithCSRF("/respondToFriendRequest", ProfileRoutes::respondToFriendRequest);

			getLoggedIn("/confirmSendFriendRequest", ProfileRoutes::confirmSendFriendRequest);
			postWithCSRF("/doSendFriendRequest", ProfileRoutes::doSendFriendRequest);
			postWithCSRF("/doRemoveFriend", ProfileRoutes::doRemoveFriend);
			getLoggedIn("/confirmRemoveFriend", ProfileRoutes::confirmRemoveFriend);

			getRequiringAccessLevelWithCSRF("/syncRelCollections", Account.AccessLevel.ADMIN, ProfileRoutes::syncRelationshipsCollections);
			getRequiringAccessLevelWithCSRF("/syncContentCollections", Account.AccessLevel.ADMIN, ProfileRoutes::syncContentCollections);
			getRequiringAccessLevelWithCSRF("/syncProfile", Account.AccessLevel.ADMIN, ProfileRoutes::syncProfile);
		});

		path("/groups/:id", ()->{
			get("", "application/activity+json", ActivityPubRoutes::groupActor);
			get("", "application/ld+json", ActivityPubRoutes::groupActor);
			get("", (req, resp)->{
				int id=Utils.parseIntOrDefault(req.params(":id"), 0);
				Group group=GroupStorage.getById(id);
				if(group==null || group instanceof ForeignGroup){
					throw new ObjectNotFoundException("err_group_not_found");
				}else{
					resp.redirect("/"+group.username);
				}
				return "";
			});

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

			getRequiringAccessLevelWithCSRF("/syncRelCollections", Account.AccessLevel.ADMIN, GroupsRoutes::syncRelationshipsCollections);
			getRequiringAccessLevelWithCSRF("/syncContentCollections", Account.AccessLevel.ADMIN, GroupsRoutes::syncContentCollections);
			getRequiringAccessLevelWithCSRF("/syncProfile", Account.AccessLevel.ADMIN, GroupsRoutes::syncProfile);
		});

		path("/posts/:postID", ()->{
			getActivityPub("", ActivityPubRoutes::post);
			get("/activityCreate", ActivityPubRoutes::postCreateActivity);
			get("", PostRoutes::standalonePost);

			getLoggedIn("/confirmDelete", PostRoutes::confirmDelete);
			postWithCSRF("/delete", PostRoutes::delete);

			getWithCSRF("/like", PostRoutes::like);
			getWithCSRF("/unlike", PostRoutes::unlike);
			get("/likePopover", PostRoutes::likePopover);
			get("/likes", PostRoutes::likeList);
			get("/ajaxCommentPreview", PostRoutes::ajaxCommentPreview);
			get("/ajaxCommentBranch", PostRoutes::ajaxCommentBranch);

			getActivityPubCollection("/replies", 50, ActivityPubRoutes::postReplies);
			getActivityPubCollection("/likes", 50, ActivityPubRoutes::postLikes);

			get("/pollVoters/:optionID", PostRoutes::pollOptionVoters);
			get("/pollVoters/:optionID/popover", PostRoutes::pollOptionVotersPopover);
			getLoggedIn("/edit", PostRoutes::editPostForm);
			postWithCSRF("/edit", PostRoutes::editPost);
		});

		get("/robots.txt", (req, resp)->{
			resp.type("text/plain");
			return "";
		});

		path("/my", ()->{
			getLoggedIn("/incomingFriendRequests", ProfileRoutes::incomingFriendRequests);
			getLoggedIn("/friends", ProfileRoutes::ownFriends);
			get("/followers", ProfileRoutes::followers);
			get("/following", ProfileRoutes::following);
			getLoggedIn("/notifications", NotificationsRoutes::notifications);
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
		});

		path("/api/v1", ()->{
			getApi("/instance", ApiRoutes::instance);
			getApi("/instance/peers", ApiRoutes::instancePeers);

			before("/*", (req, resp)->{
				resp.type("application/json");
			});
		});

		get("/healthz", (req, resp)->"");

		path("/:username", ()->{
			// These also handle groups
			getActivityPub("", ActivityPubRoutes::userActor);
			get("", ProfileRoutes::profile);

			postWithCSRF("/remoteFollow", ActivityPubRoutes::remoteFollow);
		});


		exception(ObjectNotFoundException.class, (x, req, resp)->{
			resp.status(404);
			resp.body(Utils.wrapErrorString(req, resp, Objects.requireNonNullElse(x.getMessage(), "err_not_found")));
		});
		exception(UserActionNotAllowedException.class, (x, req, resp)->{
			if(Config.DEBUG)
				LOG.warn("403: {}", req.pathInfo(), x);
			resp.status(403);
			resp.body(Utils.wrapErrorString(req, resp, Objects.requireNonNullElse(x.getMessage(), "err_access")));
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
			resp.body(Utils.wrapErrorString(req, resp, Objects.requireNonNullElse(x.getMessage(), "err_flood_control")));
		});
		exception(UserErrorException.class, (x, req, resp)->{
			resp.body(Utils.wrapErrorString(req, resp, x.getMessage()));
		});
		exception(Exception.class, (exception, req, res) -> {
			LOG.warn("Exception while processing {} {}", req.requestMethod(), req.raw().getPathInfo(), exception);
			res.status(500);
			StringWriter sw=new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));
			res.body("<h1 style='color: red;'>Unhandled exception</h1><pre>"+sw.toString().replace("<", "&lt;")+"</pre>");
		});

		after((req, resp)->{
			Long l=req.attribute("start_time");
			if(l!=null){
				long t=(long)l;
				resp.header("X-Generated-In", (System.currentTimeMillis()-t)+"");
			}
			if(req.attribute("isTemplate")!=null && !Utils.isAjax(req)){
				String cssName=req.attribute("mobile")!=null ? "mobile.css" : "desktop.css";
				resp.header("Link", "</res/"+cssName+"?"+Templates.getStaticFileVersion(cssName)+">; rel=preload; as=style, </res/common.js?"+Templates.getStaticFileVersion("common.js")+">; rel=preload; as=script");
				resp.header("Vary", "User-Agent, Accept-Language");
				resp.header("X-Powered-By", "frustration with attention economy");
			}

			if(req.attribute("isTemplate")!=null){
				try{
					if(req.session().attribute("info")==null)
						req.session().attribute("info", new SessionInfo());
					if(req.requestMethod().equalsIgnoreCase("get") && req.attribute("noHistory")==null){
						SessionInfo info=req.session().attribute("info");
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

		awaitInitialization();
		setupCustomSerializer();

		responseTypeSerializer(ActivityPubObject.class, (out, obj) -> {
			OutputStreamWriter writer=new OutputStreamWriter(out, StandardCharsets.UTF_8);
			Utils.gson.toJson(obj.asRootActivityPubObject(), writer);
			writer.flush();
		});

		responseTypeSerializer(RenderedTemplateResponse.class, (out, obj) -> {
			OutputStreamWriter writer=new OutputStreamWriter(out, StandardCharsets.UTF_8);
			obj.renderToWriter(writer);
			writer.flush();
		});

		responseTypeSerializer(WebDeltaResponse.class, (out, obj) -> {
			OutputStreamWriter writer=new OutputStreamWriter(out, StandardCharsets.UTF_8);
			Utils.gson.toJson(obj.commands(), writer);
			writer.flush();
		});

		MaintenanceScheduler.runDaily(()->{
			try{
				SessionStorage.deleteExpiredEmailCodes();
			}catch(SQLException ignore){}
			FloodControl.PASSWORD_RESET.gc();
			TopLevelDomainList.updateIfNeeded();
		});

		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			LOG.info("Stopping Spark");
			Spark.awaitStop();
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
		SessionInfo info=Utils.sessionInfo(req);
		if(info!=null && info.account!=null){
			resp.redirect("/feed");
			return "";
		}
		return new RenderedTemplateResponse("index", req).with("title", Config.serverDisplayName)
				.with("signupMode", Config.signupMode)
				.with("serverDisplayName", Config.serverDisplayName)
				.with("serverDescription", Config.serverDescription)
				.addNavBarItem(Utils.lang(req).get("index_welcome"));
	}

	private static Object methodNotAllowed(Request req, Response resp){
		resp.status(405);
		return "";
	}

	private static void setupCustomSerializer(){
		try{
			Method m=Spark.class.getDeclaredMethod("getInstance");
			m.setAccessible(true);
			Service svc=(Service) m.invoke(null);
			Field serverFld=svc.getClass().getDeclaredField("server");
			serverFld.setAccessible(true);
			EmbeddedJettyServer server=(EmbeddedJettyServer) serverFld.get(svc);
			Field handlerFld=server.getClass().getDeclaredField("handler");
			handlerFld.setAccessible(true);
			JettyHandler handler=(JettyHandler) handlerFld.get(server);
			Field filterFld=handler.getClass().getDeclaredField("filter");
			filterFld.setAccessible(true);
			MatcherFilter matcher=(MatcherFilter) filterFld.get(handler);
			Field serializerChainFld=matcher.getClass().getDeclaredField("serializerChain");
			serializerChainFld.setAccessible(true);
			SerializerChain chain=(SerializerChain) serializerChainFld.get(matcher);
			Field rootFld=chain.getClass().getDeclaredField("root");
			rootFld.setAccessible(true);
			Serializer serializer=(Serializer) rootFld.get(chain);
			ExtendedStreamingSerializer mySerializer=new ExtendedStreamingSerializer();
			mySerializer.setNext(serializer);
			rootFld.set(chain, mySerializer);
		}catch(Exception x){
			LOG.error("Exception while setting up custom serializer", x);
		}
	}
}
