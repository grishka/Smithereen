package smithereen;

import org.json.JSONObject;
import org.jtwig.JtwigModel;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import smithereen.jsonld.JLDDocument;
import smithereen.jtwigext.LangDateFunction;
import smithereen.jtwigext.LangFunction;
import smithereen.jtwigext.LangPluralFunction;
import smithereen.routes.ActivityPubRoutes;
import smithereen.routes.PostRoutes;
import smithereen.routes.ProfileRoutes;
import smithereen.routes.SessionRoutes;
import smithereen.routes.WellKnownRoutes;
import smithereen.storage.SessionStorage;
import smithereen.routes.SettingsRoutes;
import spark.Request;
import spark.Response;

import static spark.Spark.*;

public class Main{

	public static final EnvironmentConfiguration jtwigEnv;

	static{
		jtwigEnv=EnvironmentConfigurationBuilder.configuration()
				.functions()
					.add(new LangFunction())
					.add(new LangPluralFunction())
					.add(new LangDateFunction())
				.and()
				.build();
	}

	public static void main(String[] args){
		try{
			Config.load(args[0]);
		}catch(IOException x){
			throw new RuntimeException(x);
		}

		ipAddress("127.0.0.1");
		staticFileLocation("/public");
		staticFiles.expireTime(24*60*60);
		before((request, response) -> {
			if(request.session(false)==null || request.session().attribute("account")==null){
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

			path("/posts/:postID", ()->{
				get("", "application/activity+json", ActivityPubRoutes::post);
				get("", "application/ld+json", ActivityPubRoutes::post);
				get("", PostRoutes::standalonePost);
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
	}

	private static Object indexPage(Request req, Response resp){
		if(req.session().attribute("account")!=null){
			resp.redirect("/feed");
			return "";
		}
		JtwigModel model=JtwigModel.newModel().with("title", "Smithereen");
		return Utils.renderTemplate(req, "index", model);
	}

	private static Object test(Request req, Response resp){
		return "Test! "+req.params(":username");
	}
}
