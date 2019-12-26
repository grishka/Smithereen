package smithereen;

import org.jtwig.JtwigModel;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import smithereen.data.SessionInfo;
import smithereen.jtwigext.LangDateFunction;
import smithereen.jtwigext.LangFunction;
import smithereen.jtwigext.LangPluralFunction;
import smithereen.jtwigext.PictureForAvatarFunction;
import smithereen.jtwigext.RenderAttachmentsFunction;
import smithereen.routes.ActivityPubRoutes;
import smithereen.routes.PostRoutes;
import smithereen.routes.ProfileRoutes;
import smithereen.routes.SessionRoutes;
import smithereen.routes.SystemRoutes;
import smithereen.routes.WellKnownRoutes;
import smithereen.storage.SessionStorage;
import smithereen.routes.SettingsRoutes;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;

import static spark.Spark.*;

public class Main{

	public static final EnvironmentConfiguration jtwigEnv;

	static{
		jtwigEnv=EnvironmentConfigurationBuilder.configuration()
				.functions()
					.add(new LangFunction())
					.add(new LangPluralFunction())
					.add(new LangDateFunction())
					.add(new PictureForAvatarFunction())
					.add(new RenderAttachmentsFunction())
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
		staticFileLocation("/public");
		staticFiles.expireTime(24*60*60);
		before((request, response) -> {
			request.attribute("start_time", System.currentTimeMillis());
			if(request.session(false)==null || request.session().attribute("info")==null){
				String psid=request.cookie("psid");
				if(psid!=null){
					if(!SessionStorage.fillSession(psid, request.session(true))){
						response.removeCookie("/", "psid");
					}else{
						response.cookie("/", "psid", psid, 10*365*24*60*60, false);
					}
				}
			}

		});

		get("/", Main::indexPage);

		get("/feed", PostRoutes::feed);

		path("/account", ()->{
			post("/login", SessionRoutes::login);
			get("/logout", SessionRoutes::logout);
			post("/register", SessionRoutes::register);
		});

		path("/settings", ()->{
			get("/", SettingsRoutes::settings);
			post("/createInvite", SettingsRoutes::createInvite);
			post("/updatePassword", SettingsRoutes::updatePassword);
			post("/updateName", SettingsRoutes::updateName);
			post("/updateProfilePicture", SettingsRoutes::updateProfilePicture);
		});

		path("/activitypub", ()->{
			post("/sharedInbox", ActivityPubRoutes::sharedInbox);
			get("/externalInteraction", ActivityPubRoutes::externalInteraction);
			get("/nodeinfo/2.0", ActivityPubRoutes::nodeInfo);
		});

		path("/.well-known", ()->{
			get("/webfinger", WellKnownRoutes::webfinger);
			get("/nodeinfo", WellKnownRoutes::nodeInfo);
		});

		path("/system", ()->{
			get("/downloadExternalMedia", SystemRoutes::downloadExternalMedia);
		});

		path("/:username", ()->{
			get("", "application/activity+json", ActivityPubRoutes::userActor);
			get("", "application/ld+json", ActivityPubRoutes::userActor);
			get("", ProfileRoutes::profile);
			post("/createWallPost", PostRoutes::createWallPost);

			post("/remoteFollow", ActivityPubRoutes::remoteFollow);

			get("/confirmSendFriendRequest", ProfileRoutes::confirmSendFriendRequest);
			post("/doSendFriendRequest", ProfileRoutes::doSendFriendRequest);
			post("/respondToFriendRequest", ProfileRoutes::respondToFriendRequest);
			post("/doRemoveFriend", ProfileRoutes::doRemoveFriend);
			get("/confirmRemoveFriend", ProfileRoutes::confirmRemoveFriend);
			get("/friends", ProfileRoutes::friends);
			get("/incomingFriendRequests", ProfileRoutes::incomingFriendRequests);
			get("/followers", ProfileRoutes::followers);
			get("/following", ProfileRoutes::following);

			path("/posts/:postID", ()->{
				get("", "application/activity+json", ActivityPubRoutes::post);
				get("", "application/ld+json", ActivityPubRoutes::post);
				get("", PostRoutes::standalonePost);

				get("/confirmDelete", PostRoutes::confirmDelete);
				post("/delete", PostRoutes::delete);
			});

			path("/activitypub", ()->{
				post("/inbox", ActivityPubRoutes::inbox);
				get("/outbox", ActivityPubRoutes::outbox);
				get("/followers", ActivityPubRoutes::userFollowers);
				get("/following", ActivityPubRoutes::userFollowing);
				after((req, resp)->{
					if(req.requestMethod().equalsIgnoreCase("get"))
						resp.type("application/ld+json; profile=\"https://www.w3.org/ns/activitystreams\"");
				});
			});
		});


		exception(Exception.class, (exception, req, res) -> {
			res.status(500);
			StringWriter sw=new StringWriter();
			sw.append("<h1 style='color: red;'>Unhandled exception</h1><pre>");
			exception.printStackTrace(new PrintWriter(sw));
			sw.append("</pre>");
			res.body(sw.toString());
		});

		after((req, resp)->{
			Long l=req.attribute("start_time");
			if(l!=null){
				long t=(long)l;
				resp.header("X-Generated-In", (System.currentTimeMillis()-t)+"");
			}

			if(req.headers("accept")==null || !req.headers("accept").startsWith("application/")){
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
			}
		});
	}

	private static Object indexPage(Request req, Response resp){
		SessionInfo info=req.session().attribute("info");
		if(info.account!=null){
			resp.redirect("/feed");
			return "";
		}
		JtwigModel model=JtwigModel.newModel().with("title", "Smithereen");
		return Utils.renderTemplate(req, "index", model);
	}
}
