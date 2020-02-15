package smithereen.lang;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Lang{
	private static HashMap<String, Lang> langsByLocale=new HashMap<>();
	public static List<Lang> list;

	public static Lang get(Locale locale){
		Lang l=langsByLocale.get(locale.getLanguage());
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
	}

	private final JSONObject data;
	private final Locale locale;
	private final PluralRules pluralRules;
	private final ThreadLocal<DateFormat> dateFormat=new ThreadLocal<>();
	public final String name;

	private Lang(String localeID) throws IOException, JSONException{
		DataInputStream in=new DataInputStream(Lang.class.getClassLoader().getResourceAsStream("langs/"+localeID+".json"));
		byte[] buf=new byte[in.available()];
		in.readFully(buf);
		in.close();
		data=new JSONObject(new String(buf, StandardCharsets.UTF_8));
		locale=Locale.forLanguageTag(localeID);
		switch(localeID){
			case "ru":
				pluralRules=new RussianPluralRules();
				break;
			case "en":
			default:
				pluralRules=new EnglishPluralRules();
				break;
		}
		name=data.getString("_name");
	}

	public String get(String key){
		try{
			return data.getString(key);
		}catch(JSONException x){
			return key;
		}
	}

	public String get(String key, Object... formatArgs){
		try{
			return String.format(locale, data.getString(key), formatArgs);
		}catch(JSONException x){
			return key+" "+Arrays.toString(formatArgs);
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
			return quantity+" "+key+" "+Arrays.toString(formatArgs);
		}
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
}
