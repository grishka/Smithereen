package smithereen.templates;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.loader.ClasspathLoader;
import io.pebbletemplates.pebble.loader.DelegatingLoader;
import io.pebbletemplates.pebble.template.EvaluationContext;
import io.pebbletemplates.pebble.template.EvaluationContextImpl;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import io.pebbletemplates.pebble.template.Scope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.Utils;
import smithereen.model.Account;
import smithereen.model.AdminNotifications;
import smithereen.model.BirthdayReminder;
import smithereen.model.EventReminder;
import smithereen.model.SessionInfo;
import smithereen.model.UserNotifications;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.lang.Lang;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.UserStorage;
import spark.Request;

public class Templates{
	private static final PebbleEngine desktopEngine=makeEngineInstance("desktop", "common");
	private static final PebbleEngine mobileEngine=makeEngineInstance("mobile", "common");
	private static final PebbleEngine popupEngine=makeEngineInstance("popup");
	private static final Map<String, String> staticHashes=new HashMap<>();

	private static final Logger LOG=LoggerFactory.getLogger(Templates.class);

	static{
		try(InputStreamReader reader=new InputStreamReader(Objects.requireNonNull(Templates.class.getClassLoader().getResourceAsStream("static_file_versions.json")))){
			JsonObject obj=JsonParser.parseReader(reader).getAsJsonObject();
			for(Map.Entry<String, JsonElement> e:obj.entrySet()){
				staticHashes.put(e.getKey(), e.getValue().getAsString());
			}
		}catch(IOException x){
			LOG.error("Error reading static_file_versions.json", x);
		}
	}

	public static String getStaticFileVersion(String name){
		return staticHashes.get(name);
	}

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
		ZoneId tz=Utils.timeZoneForRequest(req);
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

					LocalDate today=LocalDate.now(tz);
					BirthdayReminder reminder=UserStorage.getBirthdayReminderForUser(account.user.id, today);
					if(!reminder.userIDs.isEmpty()){
						model.with("birthdayUsers", UserStorage.getByIdAsList(reminder.userIDs));
						model.with("birthdaysAreToday", reminder.day.equals(today));
					}
					EventReminder eventReminder=Utils.context(req).getGroupsController().getUserEventReminder(account.user, tz);
					if(!eventReminder.groupIDs.isEmpty()){
						model.with("eventReminderEvents", Utils.context(req).getGroupsController().getGroupsByIdAsList(eventReminder.groupIDs));
						model.with("eventsAreToday", eventReminder.day.equals(today));
					}
				}catch(SQLException x){
					throw new InternalServerErrorException(x);
				}

				if(info.permissions.role!=null){ // TODO check if this role actually grants permissions that have counters in left menu
					model.with("serverSignupMode", Config.signupMode);
					model.with("adminNotifications", AdminNotifications.getInstance(req));
				}
			}
		}
		jsConfig.addProperty("timeZone", tz!=null ? tz.getId() : null);
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
		for(String key:List.of("error", "ok", "network_error", "close", "cancel", "yes", "no", "show_technical_details", "photo_X_of_Y")){
			jsLang.add("\""+key+"\":"+lang.getAsJS(key));
		}
		if(req.attribute("mobile")!=null){
			for(String key:List.of("search", "qsearch_hint", "more_actions", "photo_open_original", "like", "add_comment",
					"object_X_of_Y", "delete", "delete_photo", "delete_photo_confirm", "set_photo_as_album_cover")){
				if(k!=null && k.contains(key))
					continue;
				jsLang.add("\""+key+"\":"+lang.getAsJS(key));
			}
		}
		model.with("timeZone", tz!=null ? tz : ZoneId.systemDefault()).with("jsConfig", jsConfig.toString())
				.with("jsLangKeys", "{"+String.join(",", jsLang)+"}")
				.with("staticHashes", staticHashes)
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

	public static int asInt(Object o){
		if(o instanceof Integer)
			return (Integer)o;
		if(o instanceof Long)
			return (int)(long)(Long)o;
		throw new IllegalArgumentException("Can't cast "+o+" to int");
	}

	public static <T> T getVariableRegardless(EvaluationContext context, String key){
		Object result=context.getVariable(key);
		if(result!=null)
			return (T)result;
		if(context instanceof EvaluationContextImpl contextImpl){
			List<Scope> scopes=contextImpl.getScopeChain().getGlobalScopes();
			for(Scope scope:scopes){
				result=scope.get(key);
				if(result!=null)
					return (T)result;
			}
		}
		return null;
	}

	public static void addJsLangForNewPostForm(Request req){
		Utils.jsLangKey(req,
				"post_form_cw", "post_form_cw_placeholder", "attach_menu_photo", "attach_menu_cw", "attach_menu_poll", "err_file_upload_too_large", "file_size_kilobytes", "file_size_megabytes", "max_attachment_count_exceeded", "remove_attachment",
				// polls
				"create_poll_question", "create_poll_options", "create_poll_add_option", "create_poll_delete_option", "create_poll_multi_choice", "create_poll_anonymous", "create_poll_time_limit", "X_days", "X_hours",
				// graffiti
				"graffiti_clear", "graffiti_undo", "graffiti_clear_confirm", "graffiti_close_confirm", "confirm_title", "graffiti_color", "graffiti_thickness", "graffiti_opacity", "attach"
			);
	}

	public static void addJsLangForPrivacySettings(Request req){
		Utils.jsLangKey(req,
				"privacy_value_everyone", "privacy_value_friends", "privacy_value_friends_of_friends", "privacy_value_no_one",
				"privacy_value_only_me", "privacy_value_everyone_except", "privacy_value_certain_friends",
				"save", "privacy_settings_title", "privacy_allowed_title", "privacy_denied_title", "privacy_allowed_to_X",
				"privacy_value_to_everyone", "privacy_value_to_friends", "privacy_value_to_friends_of_friends", "privacy_value_to_certain_friends", "delete", "privacy_enter_friend_name",
				"privacy_settings_value_except", "privacy_settings_value_certain_friends_before", "privacy_settings_value_name_separator",
				"select_friends_title", "friends_search_placeholder", "friend_list_your_friends", "friends_in_list", "select_friends_empty_selection");
	}
}
