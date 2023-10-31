package smithereen.model;

import com.google.gson.annotations.SerializedName;

import java.time.ZoneId;
import java.util.Locale;

public class UserPreferences{
	@SerializedName("lang")
	public Locale locale;
	@SerializedName("tz")
	public ZoneId timeZone;
	@SerializedName("lsntf")
	public int lastSeenNotificationID;
}
