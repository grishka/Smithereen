package smithereen.data;

import org.json.JSONObject;

import java.util.Locale;

public class UserPreferences{
	public Locale locale;

	public static UserPreferences fromJSON(JSONObject o){
		UserPreferences prefs=new UserPreferences();

		String locale=o.optString("lang", null);
		if(locale!=null){
			prefs.locale=Locale.forLanguageTag(locale);
		}

		return prefs;
	}

	public JSONObject toJSON(){
		JSONObject o=new JSONObject();
		if(locale!=null)
			o.put("lang", locale.toLanguageTag());
		return o;
	}
}
