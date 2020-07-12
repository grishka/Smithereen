package smithereen.templates;

import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.loader.DelegatingLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import org.json.JSONObject;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.Utils;
import smithereen.data.Account;
import smithereen.data.SessionInfo;
import smithereen.data.UserNotifications;
import smithereen.lang.Lang;
import smithereen.storage.NotificationsStorage;
import spark.Request;

/*package*/ class Templates{
	private static final PebbleEngine desktopEngine=makeEngineInstance("desktop", "common");
	private static final PebbleEngine mobileEngine=makeEngineInstance("mobile", "common");
	private static final PebbleEngine popupEngine=makeEngineInstance("popup");

	private static ClasspathLoader makeClasspathLoader(String dir){
		ClasspathLoader loader=new ClasspathLoader();
		loader.setSuffix(".twig");
		loader.setPrefix("templates/"+dir+"/");
		return loader;
	}

	private static PebbleEngine makeEngineInstance(String... dirs){
		return new PebbleEngine.Builder()
				.loader(new DelegatingLoader(Arrays.stream(dirs).map(Templates::makeClasspathLoader).collect(Collectors.toList())))
				.defaultLocale(Locale.US)
				.defaultEscapingStrategy("html")
				.extension(new SmithereenExtension())
				.build();
	}

	public static void addGlobalParamsToTemplate(Request req, RenderedTemplateResponse model){
		JSONObject jsConfig=new JSONObject();
		if(req.session(false)!=null){
			SessionInfo info=req.session().attribute("info");
			if(info==null){
				info=new SessionInfo();
				req.session().attribute("info", info);
			}
			Account account=info.account;
			if(account!=null){
				model.with("currentUser", account.user);
				model.with("csrf", info.csrfToken);
				model.with("userAccessLevel", account.accessLevel);
				jsConfig.put("csrf", info.csrfToken);
				jsConfig.put("uid", info.account.user.id);
				try{
					UserNotifications notifications=NotificationsStorage.getNotificationsForUser(account.user.id, account.prefs.lastSeenNotificationID);
					model.with("userNotifications", notifications);
				}catch(SQLException x){
					throw new RuntimeException(x);
				}
			}
		}
		TimeZone tz=Utils.timeZoneForRequest(req);
		jsConfig.put("timeZone", tz!=null ? tz.getID() : null);
		JSONObject jsLang=new JSONObject();
		ArrayList<String> k=req.attribute("jsLang");
		if(k!=null){
			Lang lang=Utils.lang(req);
			for(String key:k){
				jsLang.put(key, lang.raw(key));
			}
		}
		model.with("locale", Utils.localeForRequest(req)).with("timeZone", tz!=null ? tz : TimeZone.getDefault()).with("jsConfig", jsConfig.toString()).with("jsLangKeys", jsLang).with("staticHash", Utils.staticFileHash).with("serverName", Config.getServerDisplayName());
	}

	public static PebbleTemplate getTemplate(Request req, String name){
		PebbleEngine engine=desktopEngine;
		if(req.attribute("popup")!=null)
			engine=popupEngine;
		return engine.getTemplate(name);
	}

	/*package*/ static int asInt(Object o){
		if(o instanceof Integer)
			return (Integer)o;
		if(o instanceof Long)
			return (int)(long)(Long)o;
		throw new IllegalArgumentException("Can't cast "+o+" to int");
	}
}
