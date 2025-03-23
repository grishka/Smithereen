package smithereen.lang;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.pebbletemplates.pebble.extension.escaper.SafeString;
import smithereen.Utils;
import smithereen.lang.formatting.ICUMessageParser;
import smithereen.lang.formatting.ICUMessageSyntaxException;
import smithereen.lang.formatting.StringTemplate;
import smithereen.model.User;
import spark.utils.StringUtils;

public class Lang{
	private static final Logger LOG=LoggerFactory.getLogger(Lang.class);

	private static HashMap<String, Lang> langsByLocale=new HashMap<>();
	public static List<Lang> list;

	public static Lang get(Locale locale){
		Lang l=langsByLocale.get(locale.toLanguageTag());
		if(l!=null)
			return l;
		l=langsByLocale.get(locale.getLanguage());
		if(l!=null)
			return l;
		return langsByLocale.get("en");
	}

	static{
		list=new ArrayList<>();
		try(InputStream in=Lang.class.getClassLoader().getResourceAsStream("langs/index.json")){
			IndexFile index=Utils.gson.fromJson(new InputStreamReader(in), IndexFile.class);
			for(IndexLanguage lang:index.languages){
				try{
					Lang l=new Lang(lang.locale, lang.name, lang.fallback, index.files);
					list.add(l);
					langsByLocale.put(lang.locale, l);
				}catch(Exception x){
					LOG.error("Error loading language {}", lang, x);
				}
			}
		}catch(IOException|JsonParseException x){
			LOG.error("Error reading langs/index.json", x);
		}
		if(list.isEmpty())
			throw new IllegalArgumentException("No languages loaded; check langs/index.json");

		list.sort(Comparator.comparing(o->o.locale.toString()));

		for(Lang lang:list){
			if(lang.fallbackLocale!=null)
				lang.fallback=langsByLocale.get(lang.fallbackLocale.toLanguageTag());
		}
	}

	private final HashMap<String, Object> data=new HashMap<>();
	private final Locale locale;
	private final PluralRules pluralRules;
	private final Inflector inflector;
	private Lang fallback;
	public String name;
	public final String englishName;
	private final Locale fallbackLocale;
	private final NumberFormat fileSizeFormatter;

	private Lang(String localeID, String englishName, String fallbackLocaleID, List<String> files) throws IOException{
		locale=Locale.forLanguageTag(localeID);
		if(StringUtils.isNotEmpty(fallbackLocaleID)){
			fallbackLocale=Locale.forLanguageTag(fallbackLocaleID);
			if(fallbackLocale.equals(locale))
				throw new IllegalArgumentException("Language can't have itself as its fallback ("+fallbackLocale+" = "+locale+")");
		}else{
			fallbackLocale=null;
		}
		switch(localeID){
			case "ru", "be" -> {
				pluralRules=new SlavicPluralRules();
				inflector=new RussianInflector();
			}
			case "pl" -> {
				pluralRules=new SlavicPluralRules();
				inflector=null;
			}
			case "tr" -> {
				pluralRules=new SingleFormPluralRules();
				inflector=null;
			}
			default -> { // also for "en", "es"
				pluralRules=new EnglishPluralRules();
				inflector=null;
			}
		}
		this.englishName=englishName;
		for(String file:files){
			try(InputStream in=Lang.class.getClassLoader().getResourceAsStream("langs/"+localeID+"/"+file)){
				if(in==null){
					LOG.warn("Lang {} file {} not found in resources", localeID, file);
					continue;
				}
				JsonObject jobj=JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
				for(String key : jobj.keySet()){
					try{
						JsonElement el=jobj.get(key);
						if(el.isJsonPrimitive())
							data.put(key, ICUMessageParser.parse(el.getAsString()));
						else
							LOG.warn("lang {} key '{}' is not a string", locale, key);
					}catch(ICUMessageSyntaxException x){
						throw new RuntimeException("Error while parsing key "+key+" for locale "+localeID, x);
					}
				}
			}
		}
		name=data.get("lang_name").toString();
		fileSizeFormatter=DecimalFormat.getNumberInstance(locale);
		fileSizeFormatter.setMaximumFractionDigits(2);
	}

	public String get(String key){
		if(data.containsKey(key)){
			return data.get(key).toString();
		}
		return fallback!=null ? fallback.get(key) : key.replace('_', ' ');
	}

	public String get(String key, Map<String, Object> formatArgs){
		if(data.containsKey(key)){
			Object v=data.get(key);
			if(v instanceof StringTemplate)
				return ((StringTemplate) v).format(formatArgs, this);
			return v.toString();
		}
		return fallback!=null ? fallback.get(key, formatArgs) : key.replace('_', ' ');
	}

	public void inflected(StringBuilder out, User.Gender gender, String first, String last, Inflector.Case _case){
		if(inflector==null){
			out.append(first);
			if(StringUtils.isNotEmpty(last))
				out.append(' ').append(last);
			return;
		}
		if(gender==null || gender==User.Gender.UNKNOWN){
			gender=inflector.detectGender(first, last, null);
		}
		if(gender==User.Gender.UNKNOWN){
			out.append(first);
			if(StringUtils.isNotEmpty(last))
				out.append(' ').append(last);
		}else{
			out.append(inflector.isInflectable(first) ? inflector.inflectNamePart(first, Inflector.NamePart.FIRST, gender, _case) : first);
			if(StringUtils.isNotEmpty(last))
				out.append(' ').append(inflector.isInflectable(last) ? inflector.inflectNamePart(last, Inflector.NamePart.LAST, gender, _case) : last);
		}
	}

	public User.Gender detectGenderForName(String first, String last, String middle){
		if(inflector==null)
			return User.Gender.UNKNOWN;
		return inflector.detectGender(first, last, middle);
	}

	public String formatDay(LocalDate date){
		return get("date_format_other_year", Map.of("day", date.getDayOfMonth(), "month", get("month_full", Map.of("month", date.getMonthValue())), "year", date.getYear()));
	}

	public String formatDayRelative(LocalDate date, ZoneId timeZone){
		LocalDate today=LocalDate.now(timeZone);
		if(today.isEqual(date))
			return get("date_today");
		if(today.minusDays(1).isEqual(date))
			return get("date_yesterday");
		if(today.plusDays(1).isEqual(date))
			return get("date_tomorrow");
		if(today.getYear()==date.getYear())
			return get("date_format_current_year", Map.of("day", date.getDayOfMonth(), "month", get("month_full", Map.of("month", date.getMonthValue()))));
		return get("date_format_other_year", Map.of("day", date.getDayOfMonth(), "month", get("month_full", Map.of("month", date.getMonthValue())), "year", date.getYear()));
	}

	public String formatDate(Instant date, ZoneId timeZone, boolean forceAbsolute){
		long ts=date.toEpochMilli();
		long tsNow=System.currentTimeMillis();
		long diff=tsNow-ts;
		if(timeZone==null)
			timeZone=ZoneId.systemDefault();

		if(!forceAbsolute){
			if(diff>=-1000 && diff<60_000){
				return get("time_just_now");
			}else if(diff>=60_000 && diff<3600_000){
				return get("time_X_minutes_ago", Map.of("count", (int) (diff/60_000)));
			}else if(diff<-1000 && diff>-3600_000){
				return get("time_in_X_minutes", Map.of("count", -(int) (diff/60_000)));
			}
		}

		ZonedDateTime now=ZonedDateTime.now(timeZone);
		ZonedDateTime dt=date.atZone(timeZone);
		String day=null;
		if(Math.abs(diff)<=2*24*60*60*1000){
			if(now.getYear()==dt.getYear() && now.getDayOfYear()==dt.getDayOfYear()){
				day=get("date_today");
			}else if(diff>0){
				ZonedDateTime yesterday=now.minusDays(1);
				if(yesterday.getYear()==dt.getYear() && yesterday.getDayOfYear()==dt.getDayOfYear())
					day=get("date_yesterday");
			}else{
				ZonedDateTime tomorrow=now.plusDays(1);
				if(tomorrow.getYear()==dt.getYear() && tomorrow.getDayOfYear()==dt.getDayOfYear())
					day=get("date_tomorrow");
			}
		}
		if(day==null){
			if(now.getYear()==dt.getYear()){
				day=get("date_format_current_year", Map.of("day", dt.getDayOfMonth(), "month", get("month_full", Map.of("month", dt.getMonthValue()))));
			}else{
				day=get("date_format_other_year", Map.of("day", dt.getDayOfMonth(), "month", get("month_short", Map.of("month", dt.getMonthValue())), "year", dt.getYear()));
			}
		}

		return get("date_time_format", Map.of("date", new SafeString(day), "time", String.format(locale, "%d:%02d", dt.getHour(), dt.getMinute())));
	}

	public String formatDateFullyAbsolute(Instant date, ZoneId timeZone, boolean includeSeconds){
		ZonedDateTime dt=date.atZone(timeZone);
		return get("date_time_format", Map.of(
				"date", get("date_format_other_year", Map.of("day", dt.getDayOfMonth(), "month", get("month_short", Map.of("month", dt.getMonthValue())), "year", dt.getYear())),
				"time", includeSeconds ? String.format(locale, "%d:%02d:%02d", dt.getHour(), dt.getMinute(), dt.getSecond()) : String.format(locale, "%d:%02d", dt.getHour(), dt.getMinute())
		));
	}

	public String formatTime(Instant time, ZoneId timeZone){
		ZonedDateTime dt=time.atZone(timeZone);
		return String.format(locale, "%d:%02d", dt.getHour(), dt.getMinute());
	}

	public String formatTimeOrDay(Instant time, ZoneId timeZone){
		ZonedDateTime dt=time.atZone(timeZone);
		if(dt.toLocalDate().equals(LocalDate.now(timeZone))){
			return String.format(locale, "%d:%02d:%02d", dt.getHour(), dt.getMinute(), dt.getSecond());
		}else{
			return String.format(locale, "%02d.%02d.%02d", dt.getDayOfMonth(), dt.getMonthValue(), dt.getYear()%100);
		}
	}

	public String formatFileSize(long size){
		String key;
		double amount;
		if(size<1024){
			key="file_size_bytes";
			amount=size;
		}else if(size<1024L*1024){
			key="file_size_kilobytes";
			amount=size/1024.0;
		}else if(size<1024L*1024*1024){
			key="file_size_megabytes";
			amount=size/(1024.0*1024.0);
		}else if(size<1024L*1024*1024*1024){
			key="file_size_gigabytes";
			amount=size/(1024.0*1024.0*1024.0);
		}else{
			key="file_size_terabytes";
			amount=size/(1024.0*1024.0*1024.0*1024.0);
		}
		return get(key, Map.of("amount", fileSizeFormatter.format(amount)));
	}

	public String getAsJS(String key){
		if(!data.containsKey(key)){
			if(fallback!=null)
				return fallback.getAsJS(key);
			return '"'+key.replace('_', ' ')+'"';
		}
		Object o=data.get(key);
		if(o instanceof String)
			return '"'+((String) o).replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\\n")+'"';
		return ((StringTemplate)o).getAsJS();
	}

	public Locale getLocale(){
		return locale;
	}

	public String getPluralRulesName(){
		return pluralRules.getName();
	}

	public PluralCategory getPluralCategory(int quantity){
		return pluralRules.getCategoryForQuantity(quantity);
	}

	private record IndexLanguage(String locale, String name, String fallback){}
	private record IndexFile(List<IndexLanguage> languages, List<String> files){}
}
