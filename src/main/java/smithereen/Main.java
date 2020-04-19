package smithereen;

import org.jtwig.JtwigModel;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import smithereen.data.ForeignUser;
import smithereen.data.SessionInfo;
import smithereen.data.User;
import smithereen.jtwigext.LangDateFunction;
import smithereen.jtwigext.LangFunction;
import smithereen.jtwigext.LangGenderedFunction;
import smithereen.jtwigext.LangPluralFunction;
import smithereen.jtwigext.PhotoSizeFunction;
import smithereen.jtwigext.PictureForAvatarFunction;
import smithereen.jtwigext.RenderAttachmentsFunction;
import smithereen.routes.ActivityPubRoutes;
import smithereen.routes.NotificationsRoutes;
import smithereen.routes.PostRoutes;
import smithereen.routes.ProfileRoutes;
import smithereen.routes.SessionRoutes;
import smithereen.routes.SystemRoutes;
import smithereen.routes.WellKnownRoutes;
import smithereen.storage.SessionStorage;
import smithereen.routes.SettingsRoutes;
import smithereen.storage.UserStorage;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static spark.Spark.*;
import static smithereen.sparkext.SparkExtension.*;

public class Main{

	public static final EnvironmentConfiguration jtwigEnv;

	static{
		jtwigEnv=EnvironmentConfigurationBuilder.configuration()
				.functions()
					.add(new LangFunction())
					.add(new LangPluralFunction())
					.add(new LangDateFunction())
					.add(new LangGenderedFunction())
					.add(new PictureForAvatarFunction())
					.add(new RenderAttachmentsFunction())
					.add(new PhotoSizeFunction())
				.and()
				.build();
	}

	public static void main(String[] args){
		try{
			Config.load(args[0]);
		}catch(IOException x){
			throw new RuntimeException(x);
		}

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
//			String hs="";
//			for(String h:request.headers())
//				hs+="["+h+": "+request.headers(h)+"] ";
//			System.out.println(request.requestMethod()+" "+request.raw().getPathInfo()+" "+hs);
		});

		get("/", Main::indexPage);

		getLoggedIn("/feed", PostRoutes::feed);

		path("/account", ()->{
			post("/login", SessionRoutes::login);
			get("/login", SessionRoutes::login);
			get("/logout", SessionRoutes::logout);
			post("/register", SessionRoutes::register);
		});

		path("/settings", ()->{
			getLoggedIn("/", SettingsRoutes::settings);
			postWithCSRF("/createInvite", SettingsRoutes::createInvite);
			postWithCSRF("/updatePassword", SettingsRoutes::updatePassword);
			postWithCSRF("/updateName", SettingsRoutes::updateName);
			postLoggedIn("/updateProfilePicture", SettingsRoutes::updateProfilePicture);
			post("/setLanguage", SettingsRoutes::setLanguage);
			post("/setTimezone", SettingsRoutes::setTimezone);
		});

		path("/activitypub", ()->{
			post("/sharedInbox", ActivityPubRoutes::sharedInbox);
			getLoggedIn("/externalInteraction", ActivityPubRoutes::externalInteraction);
			get("/nodeinfo/2.0", ActivityPubRoutes::nodeInfo);
		});

		path("/.well-known", ()->{
			get("/webfinger", WellKnownRoutes::webfinger);
			get("/nodeinfo", WellKnownRoutes::nodeInfo);
		});

		path("/system", ()->{
			get("/downloadExternalMedia", SystemRoutes::downloadExternalMedia);
			getLoggedIn("/deleteDraftAttachment", SystemRoutes::deleteDraftAttachment);
			path("/upload", ()->{
				postLoggedIn("/postPhoto", SystemRoutes::uploadPostPhoto);
			});
		});

		path("/users/:id", ()->{
			get("", "application/activity+json", ActivityPubRoutes::userActor);
			get("", "application/ld+json", ActivityPubRoutes::userActor);
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

			post("/inbox", ActivityPubRoutes::inbox);
			get("/outbox", ActivityPubRoutes::outbox);
			post("/outbox", (req, resp)->{
				resp.status(405);
				return "";
			});
			get("/followers", ActivityPubRoutes::userFollowers);
			get("/following", ActivityPubRoutes::userFollowing);
		});

		path("/posts/:postID", ()->{
			get("", "application/activity+json", ActivityPubRoutes::post);
			get("", "application/ld+json", ActivityPubRoutes::post);
			get("", PostRoutes::standalonePost);

			getLoggedIn("/confirmDelete", PostRoutes::confirmDelete);
			postWithCSRF("/delete", PostRoutes::delete);
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
		});

		path("/:username", ()->{
			get("", "application/activity+json", ActivityPubRoutes::userActor);
			get("", "application/ld+json", ActivityPubRoutes::userActor);
			get("", ProfileRoutes::profile);
			postWithCSRF("/createWallPost", PostRoutes::createWallPost);

			postWithCSRF("/remoteFollow", ActivityPubRoutes::remoteFollow);

			getLoggedIn("/confirmSendFriendRequest", ProfileRoutes::confirmSendFriendRequest);
			postWithCSRF("/doSendFriendRequest", ProfileRoutes::doSendFriendRequest);
			postWithCSRF("/respondToFriendRequest", ProfileRoutes::respondToFriendRequest);
			postWithCSRF("/doRemoveFriend", ProfileRoutes::doRemoveFriend);
			getLoggedIn("/confirmRemoveFriend", ProfileRoutes::confirmRemoveFriend);
			get("/friends", ProfileRoutes::friends);
			get("/followers", ProfileRoutes::followers);
			get("/following", ProfileRoutes::following);
		});


		exception(Exception.class, (exception, req, res) -> {
			System.out.println("Exception while processing "+req.requestMethod()+" "+req.raw().getPathInfo());
			exception.printStackTrace();
			res.status(500);
			StringWriter sw=new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));
			res.body("<h1 style='color: red;'>Unhandled exception</h1><pre>"+sw.toString().replace("<", "&gt;")+"</pre>");
		});

		after((req, resp)->{
			Long l=req.attribute("start_time");
			if(l!=null){
				long t=(long)l;
				resp.header("X-Generated-In", (System.currentTimeMillis()-t)+"");
			}
			if(req.attribute("isTemplate")!=null){
				resp.header("Link", "</res/style.css?"+Utils.staticFileHash+">; rel=preload; as=style, </res/common.js?"+Utils.staticFileHash+">; rel=preload; as=script");
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
		SessionInfo info=req.session().attribute("info");
		if(info!=null && info.account!=null){
			resp.redirect("/feed");
			return "";
		}
		JtwigModel model=JtwigModel.newModel().with("title", "Smithereen");
		return Utils.renderTemplate(req, "index", model);
	}
}
