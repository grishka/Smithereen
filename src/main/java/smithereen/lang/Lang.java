package smithereen.lang;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import smithereen.data.User;
import spark.utils.StringUtils;

public class Lang{
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
		ArrayList<String> files=new ArrayList<>();
		list=new ArrayList<>();
		try(InputStream in=Lang.class.getClassLoader().getResourceAsStream("langs/index.json")){
			JsonArray arr=JsonParser.parseReader(new InputStreamReader(in)).getAsJsonArray();
			for(JsonElement el:arr)
				files.add(el.getAsString());
		}catch(IOException x){
			System.err.println("Error reading langs/index.json");
			x.printStackTrace();
		}catch(JsonParseException x){
			System.err.println("Error parsing langs/index.json");
			x.printStackTrace();
		}
		if(files.isEmpty())
			throw new IllegalArgumentException("No language files to load; check langs/index.json");
		for(String langFileName:files){
			try{
				Lang l=new Lang(langFileName);
				langsByLocale.put(langFileName, l);
				list.add(l);
			}catch(IOException x){
				System.err.println("Error reading langs/"+langFileName+".json");
				x.printStackTrace();
			}catch(JsonParseException x){
				System.err.println("Error parsing langs/"+langFileName+".json");
				x.printStackTrace();
			}
		}

		list.sort(new Comparator<Lang>(){
			@Override
			public int compare(Lang o1, Lang o2){
				return o1.locale.toString().compareTo(o2.locale.toString());
			}
		});

		for(Lang lang:list){
			if(lang.fallbackLocale!=null)
				lang.fallback=langsByLocale.get(lang.fallbackLocale.toLanguageTag());
		}
	}

	private final JsonObject data;
	private final Locale locale;
	private final PluralRules pluralRules;
	private final Inflector inflector;
	private Lang fallback;
	public final String name;
	private final Locale fallbackLocale;

	private Lang(String localeID) throws IOException{
		try(InputStream in=Lang.class.getClassLoader().getResourceAsStream("langs/"+localeID+".json")){
			data=JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject();
		}
		locale=Locale.forLanguageTag(localeID);
		String fallbackLocaleID=data.has("_fallback") && data.get("_fallback").isJsonPrimitive() ? data.get("_fallback").getAsString() : null;
		if(StringUtils.isNotEmpty(fallbackLocaleID)){
			fallbackLocale=Locale.forLanguageTag(fallbackLocaleID);
			if(fallbackLocale.equals(locale))
				throw new IllegalArgumentException("Language can't have itself as its fallback ("+fallbackLocale+" = "+locale+")");
		}else{
			fallbackLocale=null;
		}
		switch(localeID){
			case "ru":
				pluralRules=new SlavicPluralRules();
				inflector=new RussianInflector();
				break;
			case "pl":
				pluralRules=new SlavicPluralRules();
				inflector=null;
				break;
			case "tr":
				pluralRules=new SingleFormPluralRules();
				inflector=null;
				break;
			case "es": //Spanish plural rules are the same as english
			case "en":
			default:
				pluralRules=new EnglishPluralRules();
				inflector=null;
				break;
		}
		name=data.get("_name").getAsString();
	}

	public String get(String key){
		JsonElement el=data.get(key);
		if(el!=null && el.isJsonPrimitive())
			return el.getAsString();
		return fallback!=null ? fallback.get(key) : key;
	}

	public String get(String key, Object... formatArgs){
		JsonElement el=data.get(key);
		if(el!=null && el.isJsonPrimitive())
			return String.format(locale, el.getAsString(), formatArgs);
		return fallback!=null ? fallback.get(key, formatArgs) : key+" "+Arrays.toString(formatArgs);
	}

	public String plural(String key, int quantity, Object... formatArgs){
		JsonElement el=data.get(key);
		if(el!=null && el.isJsonArray()){
			if(formatArgs.length>0){
				Object[] args=new Object[formatArgs.length+1];
				args[0]=quantity;
				System.arraycopy(formatArgs, 0, args, 1, formatArgs.length);
				return String.format(locale, el.getAsJsonArray().get(pluralRules.getIndexForQuantity(quantity)).getAsString(), args);
			}else{
				return String.format(locale, el.getAsJsonArray().get(pluralRules.getIndexForQuantity(quantity)).getAsString(), quantity);
			}
		}
		return fallback!=null ? fallback.plural(key, quantity, formatArgs) : quantity+" "+key+" "+Arrays.toString(formatArgs);
	}

	public String gendered(String key, User.Gender gender, Object... formatArgs){
		JsonElement el=data.get(key);
		if(el!=null){
			if(el.isJsonArray()){
				JsonArray ar=el.getAsJsonArray();
				String s=switch(gender){
					case MALE -> ar.get(1).getAsString();
					case FEMALE -> ar.get(0).getAsString();
					case UNKNOWN -> ar.get(ar.size()>2 ? 3 : 0).getAsString();
				};
				return formatArgs.length>0 ? String.format(locale, s, formatArgs) : s;
			}else if(el.isJsonPrimitive()){
				return get(key, formatArgs);
			}
		}
		return fallback!=null ? fallback.gendered(key, gender, formatArgs) : key+" "+Arrays.toString(formatArgs);
	}

	public String inflected(String key, User.Gender gender, String first, String last, String middle, Object... formatArgs){
		JsonElement el=data.get(key);
		if(el==null){
			return fallback!=null ? fallback.inflected(key, gender, first, last, middle, formatArgs) : key+" "+first+" "+last+" "+middle+" "+Arrays.toString(formatArgs);
		}else if(!el.isJsonObject()){
			String name="";
			if(first!=null)
				name+=first;
			if(middle!=null)
				name+=" "+middle;
			if(last!=null)
				name+=" "+last;
			Object[] args=new Object[formatArgs.length+1];
			args[0]=name;
			System.arraycopy(formatArgs, 0, args, 1, formatArgs.length);
			return get(key, args);
		}
		JsonObject o=el.getAsJsonObject();
		String str=o.get("str").getAsString();
		Inflector.Case c=Inflector.Case.valueOf(o.get("case").getAsString());
		String name="";
		if(gender==null || gender==User.Gender.UNKNOWN){
			gender=inflector.detectGender(first, last, middle);
		}
		if(gender==User.Gender.UNKNOWN){
			if(first!=null)
				name+=first;
			if(middle!=null)
				name+=" "+middle;
			if(last!=null)
				name+=" "+last;
		}else{
			if(first!=null)
				name+=inflector.isInflectable(first) ? inflector.inflectNamePart(first, Inflector.NamePart.FIRST, gender, c) : first;
			if(middle!=null)
				name+=" "+(inflector.isInflectable(middle) ? inflector.inflectNamePart(middle, Inflector.NamePart.MIDDLE, gender, c) : middle);
			if(last!=null)
				name+=" "+(inflector.isInflectable(last) ? inflector.inflectNamePart(last, Inflector.NamePart.LAST, gender, c) : last);
		}

		Object[] args=new Object[formatArgs.length+1];
		args[0]=name;
		System.arraycopy(formatArgs, 0, args, 1, formatArgs.length);
		return String.format(locale, str, args);
	}

	public User.Gender detectGenderForName(String first, String last, String middle){
		if(inflector==null)
			return User.Gender.UNKNOWN;
		return inflector.detectGender(first, last, middle);
	}

	public String formatDay(LocalDate date){
		JsonArray months=data.getAsJsonArray("months_full");
		return get("date_format_other_year", date.getDayOfMonth(), months.get(date.getMonthValue()-1).getAsString(), date.getYear());
	}

	public String formatDate(Date date, TimeZone timeZone, boolean forceAbsolute){
		long ts=date.getTime();
		long tsNow=System.currentTimeMillis();
		long diff=tsNow-ts;

		if(!forceAbsolute){
			if(diff>=0 && diff<60_000){
				return get("time_just_now");
			}else if(diff>=60_000 && diff<3600_000){
				return plural("time_X_minutes_ago", (int) (diff/60_000));
			}else if(diff<0 && diff>-3600_000){
				return plural("time_in_X_minutes", -(int) (diff/60_000));
			}
		}

		ZonedDateTime now=ZonedDateTime.now(timeZone.toZoneId());
		ZonedDateTime dt=date.toInstant().atZone(timeZone.toZoneId());
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
				JsonArray months=data.getAsJsonArray("months_full");
				day=get("date_format_current_year", dt.getDayOfMonth(), months.get(dt.getMonthValue()-1).getAsString());
			}else{
				JsonArray months=data.getAsJsonArray("months_short");
				day=get("date_format_other_year", dt.getDayOfMonth(), months.get(dt.getMonthValue()-1).getAsString(), dt.getYear());
			}
		}

		return get("date_time_format", day, String.format(locale, "%d:%02d", dt.getHour(), dt.getMinute()));
	}

	public JsonElement raw(String key){
		return data.get(key);
	}

	public Locale getLocale(){
		return locale;
	}
}
