package smithereen.data;

import org.json.JSONObject;

import java.util.Locale;
import java.util.TimeZone;

public class UserPreferences{
	public Locale locale;
	public TimeZone timeZone;
	public int lastSeenNotificationID;

	public static UserPreferences fromJSON(JSONObject o){
		UserPreferences prefs=new UserPreferences();

		String locale=o.optString("lang", null);
		if(locale!=null){
			prefs.locale=Locale.forLanguageTag(locale);
		}
		String timezone=o.optString("tz", null);
		if(timezone!=null){
			prefs.timeZone=TimeZone.getTimeZone(timezone);
		}
		prefs.lastSeenNotificationID=o.optInt("lsntf", 0);

		return prefs;
	}

	public JSONObject toJSON(){
		JSONObject o=new JSONObject();
		if(locale!=null)
			o.put("lang", locale.toLanguageTag());
		if(timeZone!=null)
			o.put("tz", timeZone.getID());
		if(lastSeenNotificationID!=0)
			o.put("lsntf", lastSeenNotificationID);
		return o;
	}
}
