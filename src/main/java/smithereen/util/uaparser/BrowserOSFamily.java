package smithereen.util.uaparser;

public enum BrowserOSFamily{
	WINDOWS_PHONE("Windows Phone"),
	WINDOWS("Windows"),
	MAC_OS("macOS"),
	IOS("iOS"),
	ANDROID("Android"),
	WEBOS("WebOS"),
	BLACKBERRY("BlackBerry"),
	BADA("Bada"),
	TIZEN("Tizen"),
	LINUX("Linux"),
	CHROME_OS("Chrome OS"),
	PLAYSTATION_4("PlayStation 4"),
	ROKU("Roku"),
	UNKNOWN("Unknown OS");

	public final String displayName;

	BrowserOSFamily(String displayName){
		this.displayName=displayName;
	}
}
