package smithereen.data;

import com.google.gson.annotations.SerializedName;

import java.util.Locale;
import java.util.TimeZone;

public class UserPreferences{
	@SerializedName("lang")
	public Locale locale;
	@SerializedName("tz")
	public TimeZone timeZone;
	@SerializedName("lsntf")
	public int lastSeenNotificationID;
}
