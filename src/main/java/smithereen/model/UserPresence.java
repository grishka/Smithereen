package smithereen.model;

import java.time.Instant;

public record UserPresence(boolean isOnline, Instant lastUpdated, PresenceType type){

	public boolean isMobile(){
		return type==PresenceType.MOBILE_WEB;
	}

	public enum PresenceType{
		WEB,
		MOBILE_WEB,
	}
}
