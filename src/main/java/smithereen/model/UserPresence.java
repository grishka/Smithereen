package smithereen.model;

import java.time.Instant;

public record UserPresence(boolean isOnline, Instant lastUpdated, PresenceType type, long appID){

	public boolean isMobile(){
		return type==PresenceType.MOBILE_WEB || type==PresenceType.MOBILE_API;
	}

	public enum PresenceType{
		WEB,
		MOBILE_WEB,
		API,
		MOBILE_API,
	}
}
