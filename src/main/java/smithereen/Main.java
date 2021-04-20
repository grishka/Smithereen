package smithereen;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Objects;

import smithereen.data.Account;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.SessionInfo;
import smithereen.data.User;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.routes.ActivityPubRoutes;
import smithereen.routes.GroupsRoutes;
import smithereen.routes.NotificationsRoutes;
import smithereen.routes.PostRoutes;
import smithereen.routes.ProfileRoutes;
import smithereen.routes.SessionRoutes;
import smithereen.routes.SettingsAdminRoutes;
import smithereen.routes.SystemRoutes;
import smithereen.routes.WellKnownRoutes;
import smithereen.storage.DatabaseSchemaUpdater;
import smithereen.storage.GroupStorage;
import smithereen.storage.SessionStorage;
import smithereen.routes.SettingsRoutes;
import smithereen.storage.UserStorage;
import smithereen.templates.RenderedTemplateResponse;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static spark.Spark.*;
import static smithereen.sparkext.SparkExtension.*;

public class Main{

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

		get("/", Main::indexPage);

		getLoggedIn("/feed", PostRoutes::feed);

		path("/account", ()->{
			post("/login", SessionRoutes::login);
			get("/login", SessionRoutes::login);
			get("/logout", SessionRoutes::logout);
			post("/register", SessionRoutes::register);
			get("/register", SessionRoutes::registerForm);
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

			path("/admin", ()->{
				getRequiringAccessLevel("", Account.AccessLevel.ADMIN, SettingsAdminRoutes::index);
				postRequiringAccessLevelWithCSRF("/updateServerInfo", Account.AccessLevel.ADMIN, SettingsAdminRoutes::updateServerInfo);
				getRequiringAccessLevel("/users", Account.AccessLevel.ADMIN, SettingsAdminRoutes::users);
				getRequiringAccessLevel("/users/accessLevelForm", Account.AccessLevel.ADMIN, SettingsAdminRoutes::accessLevelForm);
				postRequiringAccessLevelWithCSRF("/users/setAccessLevel", Account.AccessLevel.ADMIN, SettingsAdminRoutes::setUserAccessLevel);
			});
		});

		path("/activitypub", ()->{
			post("/sharedInbox", ActivityPubRoutes::sharedInbox);
			get("/sharedInbox", Main::methodNotAllowed);
			getLoggedIn("/externalInteraction", ActivityPubRoutes::externalInteraction);
			get("/nodeinfo/2.0", ActivityPubRoutes::nodeInfo);
			path("/objects", ()->{
				path("/likes/:likeID", ()->{
					get("", ActivityPubRoutes::likeObject);
					get("/undo", ActivityPubRoutes::undoLikeObject);
				});
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
		});

		path("/users/:id", ()->{
			getActivityPub("", ActivityPubRoutes::userActor);
			get("", (req, resp)->{
				int id=Utils.parseIntOrDefault(req.params(":id"), 0);
				User user=UserStorage.getById(id);
				if(user==null || user instanceof ForeignUser){
					resp.status(404);
				}else{
					resp.redirect("/"+user.username);
				}
				return "";
			});

			post("/inbox", ActivityPubRoutes::userInbox);
			get("/inbox", Main::methodNotAllowed);
			get("/outbox", ActivityPubRoutes::userOutbox);
			post("/outbox", Main::methodNotAllowed);
			getActivityPubCollection("/followers", 50, ActivityPubRoutes::userFollowers);
			getActivityPubCollection("/following", 50, ActivityPubRoutes::userFollowing);
			getActivityPubCollection("/wall", 50, ActivityPubRoutes::userWall);

			postWithCSRF("/createWallPost", PostRoutes::createUserWallPost);
			getLoggedIn("/confirmBlock", ProfileRoutes::confirmBlockUser);
			getLoggedIn("/confirmUnblock", ProfileRoutes::confirmUnblockUser);
			postWithCSRF("/block", ProfileRoutes::blockUser);
			postWithCSRF("/unblock", ProfileRoutes::unblockUser);
		});

		path("/groups/:id", ()->{
			get("", "application/activity+json", ActivityPubRoutes::groupActor);
			get("", "application/ld+json", ActivityPubRoutes::groupActor);
			get("", (req, resp)->{
				int id=Utils.parseIntOrDefault(req.params(":id"), 0);
				Group group=GroupStorage.getByID(id);
				if(group==null || group instanceof ForeignGroup){
					resp.status(404);
				}else{
					resp.redirect("/"+group.username);
				}
				return "";
			});

			postWithCSRF("/createWallPost", PostRoutes::createGroupWallPost);

			getWithCSRF("/join", GroupsRoutes::join);
			getWithCSRF("/leave", GroupsRoutes::leave);

			post("/inbox", ActivityPubRoutes::groupInbox);
			get("/inbox", Main::methodNotAllowed);
			getActivityPubCollection("/outbox", 50, ActivityPubRoutes::groupOutbox);
			post("/outbox", Main::methodNotAllowed);
			getActivityPubCollection("/followers", 50, ActivityPubRoutes::groupFollowers);
			getActivityPubCollection("/wall", 50, ActivityPubRoutes::groupWall);

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

			get("/members", GroupsRoutes::members);
			get("/admins", GroupsRoutes::admins);
		});

		path("/posts/:postID", ()->{
			getActivityPub("", ActivityPubRoutes::post);
			get("", PostRoutes::standalonePost);

			getLoggedIn("/confirmDelete", PostRoutes::confirmDelete);
			postWithCSRF("/delete", PostRoutes::delete);

			getWithCSRF("/like", PostRoutes::like);
			getWithCSRF("/unlike", PostRoutes::unlike);
			get("/likePopover", PostRoutes::likePopover);
			get("/likes", PostRoutes::likeList);

			getActivityPubCollection("/replies", 50, ActivityPubRoutes::postReplies);
		});

		get("/robots.txt", (req, resp)->{
			resp.type("text/plain");
			return "";
		});

		path("/my", ()->{
			getLoggedIn("/incomingFriendRequests", ProfileRoutes::incomingFriendRequests);
			get("/friends", ProfileRoutes::friends);
			get("/followers", ProfileRoutes::followers);
			get("/following", ProfileRoutes::following);
			getLoggedIn("/notifications", NotificationsRoutes::notifications);
			path("/groups", ()->{
				getLoggedIn("", GroupsRoutes::myGroups);
				getLoggedIn("/managed", GroupsRoutes::myManagedGroups);
				getLoggedIn("/create", GroupsRoutes::createGroup);
				postWithCSRF("/create", GroupsRoutes::doCreateGroup);
			});
		});

		path("/:username", ()->{
			// These also handle groups
			getActivityPub("", ActivityPubRoutes::userActor);
			get("", ProfileRoutes::profile);


			postWithCSRF("/remoteFollow", ActivityPubRoutes::remoteFollow);

			getLoggedIn("/confirmSendFriendRequest", ProfileRoutes::confirmSendFriendRequest);
			postWithCSRF("/doSendFriendRequest", ProfileRoutes::doSendFriendRequest);
			postWithCSRF("/respondToFriendRequest", ProfileRoutes::respondToFriendRequest);
			getWithCSRF("/respondToFriendRequest", ProfileRoutes::respondToFriendRequest);
			postWithCSRF("/doRemoveFriend", ProfileRoutes::doRemoveFriend);
			getLoggedIn("/confirmRemoveFriend", ProfileRoutes::confirmRemoveFriend);
			path("/friends", ()->{
				get("", ProfileRoutes::friends);
				getLoggedIn("/mutual", ProfileRoutes::mutualFriends);
			});
			get("/followers", ProfileRoutes::followers);
			get("/following", ProfileRoutes::following);
			path("/wall", ()->{
				get("", PostRoutes::wallAll);
				get("/own", PostRoutes::wallOwn);
				get("/with/:other_username", PostRoutes::wallToWall);
			});
		});


		exception(ObjectNotFoundException.class, (x, req, resp)->{
			resp.status(404);
			resp.body(Utils.wrapError(req, resp, Objects.requireNonNullElse(x.getMessage(), "err_not_found")));
		});
		exception(UserActionNotAllowedException.class, (x, req, resp)->{
			resp.status(403);
			resp.body(Utils.wrapError(req, resp, Objects.requireNonNullElse(x.getMessage(), "err_access")));
		});
		exception(BadRequestException.class, (x, req, resp)->{
			resp.status(400);
			String msg=x.getMessage();
			if(StringUtils.isNotEmpty(msg))
				resp.body("Bad request: "+msg.replace("<", "&lt;"));
			else
				resp.body("Bad request");
		});
		exception(Exception.class, (exception, req, res) -> {
			System.out.println("Exception while processing "+req.requestMethod()+" "+req.raw().getPathInfo());
			exception.printStackTrace();
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
			if(req.attribute("isTemplate")!=null){
				String cssName=req.attribute("mobile")!=null ? "mobile" : "desktop";
				resp.header("Link", "</res/"+cssName+".css?"+Utils.staticFileHash+">; rel=preload; as=style, </res/common.js?"+Utils.staticFileHash+">; rel=preload; as=script");
				resp.header("Vary", "User-Agent");
			}

			if(req.headers("accept")==null || !req.headers("accept").startsWith("application/")){
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
	}

	private static Object indexPage(Request req, Response resp){
		SessionInfo info=Utils.sessionInfo(req);
		if(info!=null && info.account!=null){
			resp.redirect("/feed");
			return "";
		}
		return new RenderedTemplateResponse("index").with("title", Config.serverDisplayName)
				.with("signupMode", Config.signupMode)
				.with("serverDisplayName", Config.serverDisplayName)
				.with("serverDescription", Config.serverDescription)
				.renderToString(req);
	}

	private static Object methodNotAllowed(Request req, Response resp){
		resp.status(405);
		return "";
	}
}
