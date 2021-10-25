package smithereen.templates;

import com.google.gson.JsonObject;
import com.mitchellbosecke.pebble.PebbleEngine;
import com.mitchellbosecke.pebble.loader.ClasspathLoader;
import com.mitchellbosecke.pebble.loader.DelegatingLoader;
import com.mitchellbosecke.pebble.template.PebbleTemplate;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.Utils;
import smithereen.data.Account;
import smithereen.data.BirthdayReminder;
import smithereen.data.SessionInfo;
import smithereen.data.UserNotifications;
import smithereen.lang.Lang;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.UserStorage;
import spark.Request;

public class Templates{
	private static final PebbleEngine desktopEngine=makeEngineInstance("desktop", "common");
	private static final PebbleEngine mobileEngine=makeEngineInstance("mobile", "common");
	private static final PebbleEngine popupEngine=makeEngineInstance("popup");

	private static ClasspathLoader makeClasspathLoader(String dir){
		ClasspathLoader loader=new ClasspathLoader();
		loader.setSuffix(".twig");
		loader.setPrefix("templates/"+dir+"/");
		return loader;
	}

	public static PebbleEngine makeEngineInstance(String... dirs){
		return new PebbleEngine.Builder()
				.loader(new DelegatingLoader(Arrays.stream(dirs).map(Templates::makeClasspathLoader).collect(Collectors.toList())))
				.defaultLocale(Locale.US)
				.defaultEscapingStrategy("html")
				.extension(new SmithereenExtension())
				.build();
	}

	public static void addGlobalParamsToTemplate(Request req, RenderedTemplateResponse model){
		JsonObject jsConfig=new JsonObject();
		TimeZone tz=Utils.timeZoneForRequest(req);
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
				model.with("userPermissions", info.permissions);
				jsConfig.addProperty("csrf", info.csrfToken);
				jsConfig.addProperty("uid", info.account.user.id);
				try{
					UserNotifications notifications=NotificationsStorage.getNotificationsForUser(account.user.id, account.prefs.lastSeenNotificationID);
					model.with("userNotifications", notifications);

					LocalDate today=LocalDate.now(tz.toZoneId());
					BirthdayReminder reminder=UserStorage.getBirthdayReminderForUser(account.user.id, today);
					if(!reminder.userIDs.isEmpty()){
						model.with("birthdayUsers", UserStorage.getByIdAsList(reminder.userIDs));
						model.with("birthdaysAreToday", reminder.day.equals(today));
					}
				}catch(SQLException x){
					throw new RuntimeException(x);
				}
			}
		}
		jsConfig.addProperty("timeZone", tz!=null ? tz.getID() : null);
		ArrayList<String> jsLang=new ArrayList<>();
		ArrayList<String> k=req.attribute("jsLang");
		Lang lang=Utils.lang(req);
		jsConfig.addProperty("locale", lang.getLocale().toLanguageTag());
		jsConfig.addProperty("langPluralRulesName", lang.getPluralRulesName());
		if(k!=null){
			for(String key:k){
				jsLang.add("\""+key+"\":"+lang.getAsJS(key));
			}
		}
		for(String key: List.of("error", "ok", "network_error", "close")){
			jsLang.add("\""+key+"\":"+lang.getAsJS(key));
		}
		if(req.attribute("mobile")!=null){
			for(String key: List.of("search", "qsearch_hint")){
				jsLang.add("\""+key+"\":"+lang.getAsJS(key));
			}
		}
		model.with("locale", Utils.localeForRequest(req)).with("timeZone", tz!=null ? tz : TimeZone.getDefault()).with("jsConfig", jsConfig.toString())
				.with("jsLangKeys", "{"+String.join(",", jsLang)+"}")
				.with("staticHash", Utils.staticFileHash)
				.with("serverName", Config.getServerDisplayName())
				.with("serverDomain", Config.domain)
				.with("isMobile", req.attribute("mobile")!=null);
	}

	public static PebbleTemplate getTemplate(Request req, String name){
		PebbleEngine engine=desktopEngine;
		if(req.attribute("popup")!=null)
			engine=popupEngine;
		else if(req.attribute("mobile")!=null)
			engine=mobileEngine;
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
