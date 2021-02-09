package smithereen.lang;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
		return langsByLocale.get("en");
	}

	static{
		ArrayList<String> files=new ArrayList<>();
		list=new ArrayList<>();
		try{
			DataInputStream in=new DataInputStream(Lang.class.getClassLoader().getResourceAsStream("langs/index.json"));
			byte[] buf=new byte[in.available()];
			in.readFully(buf);
			in.close();
			JSONArray arr=new JSONArray(new String(buf, StandardCharsets.UTF_8));
			for(int i=0;i<arr.length();i++)
				files.add(arr.getString(i));
		}catch(IOException x){
			System.err.println("Error reading langs/index.json");
			x.printStackTrace();
		}catch(JSONException x){
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
			}catch(JSONException x){
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

	private final JSONObject data;
	private final Locale locale;
	private final PluralRules pluralRules;
	private final Inflector inflector;
	private final ThreadLocal<DateFormat> dateFormat=new ThreadLocal<>();
	private Lang fallback;
	public final String name;
	private final Locale fallbackLocale;

	private Lang(String localeID) throws IOException, JSONException{
		try(InputStream _in=Lang.class.getClassLoader().getResourceAsStream("langs/"+localeID+".json")){
			DataInputStream in=new DataInputStream(_in);
			byte[] buf=new byte[in.available()];
			in.readFully(buf);
			data=new JSONObject(new String(buf, StandardCharsets.UTF_8));
		}
		locale=Locale.forLanguageTag(localeID);
		String fallbackLocaleID=data.optString("_fallback");
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
			case "en":
			default:
				pluralRules=new EnglishPluralRules();
				inflector=null;
				break;
		}
		name=data.getString("_name");
	}

	public String get(String key){
		try{
			return data.getString(key);
		}catch(JSONException x){
			return fallback!=null ? fallback.get(key) : key;
		}
	}

	public String get(String key, Object... formatArgs){
		try{
			return String.format(locale, data.getString(key), formatArgs);
		}catch(JSONException x){
			return fallback!=null ? fallback.get(key, formatArgs) : key+" "+Arrays.toString(formatArgs);
		}
	}

	public String plural(String key, int quantity, Object... formatArgs){
		try{
			JSONArray v=data.getJSONArray(key);
			if(formatArgs.length>0){
				Object[] args=new Object[formatArgs.length+1];
				args[0]=quantity;
				System.arraycopy(formatArgs, 0, args, 1, formatArgs.length);
				return String.format(locale, v.getString(pluralRules.getIndexForQuantity(quantity)), args);
			}else{
				return String.format(locale, v.getString(pluralRules.getIndexForQuantity(quantity)), quantity);
			}
		}catch(JSONException x){
			return fallback!=null ? fallback.plural(key, quantity, formatArgs) : quantity+" "+key+" "+Arrays.toString(formatArgs);
		}
	}

	public String gendered(String key, User.Gender gender, Object... formatArgs){
		try{
			JSONArray ar=data.optJSONArray(key);
			if(ar==null)
				return get(key, formatArgs);
			String s;
			switch(gender){
				case FEMALE:
					s=ar.getString(1);
					break;
				case MALE:
					s=ar.getString(0);
					break;
				case UNKNOWN:
				default:
					s=ar.getString(ar.length()>2 ? 3 : 0);
					break;
			}
			return formatArgs.length>0 ? String.format(locale, s, formatArgs) : s;
		}catch(JSONException x){
			return fallback!=null ? fallback.gendered(key, gender, formatArgs) : key+" "+Arrays.toString(formatArgs);
		}
	}

	public String inflected(String key, User.Gender gender, String first, String last, String middle, Object... formatArgs){
		try{
			JSONObject o=inflector==null ? null : data.optJSONObject(key);
			if(o==null){
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
			String str=o.getString("str");
			Inflector.Case c=Inflector.Case.valueOf(o.getString("case"));
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
		}catch(JSONException x){
			return fallback!=null ? fallback.inflected(key, gender, first, last, middle, formatArgs) : key+" "+first+" "+last+" "+middle+" "+Arrays.toString(formatArgs);
		}
	}

	public User.Gender detectGenderForName(String first, String last, String middle){
		if(inflector==null)
			return User.Gender.UNKNOWN;
		return inflector.detectGender(first, last, middle);
	}

	public String formatDate(Date date, TimeZone timeZone){
		DateFormat format=dateFormat.get();
		if(format==null)
			dateFormat.set(format=new SimpleDateFormat("dd MMMM yyyy, HH:mm", locale));
		format.setTimeZone(timeZone);
		return format.format(date);
	}

	public Object raw(String key){
		return data.opt(key);
	}

	public Locale getLocale(){
		return locale;
	}
}
