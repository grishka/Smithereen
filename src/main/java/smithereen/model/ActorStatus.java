package smithereen.model;

import java.net.URI;
import java.time.Instant;

public record ActorStatus(String text, Instant updatedAt, Instant expiry, URI apId){
	public boolean isExpired(){
		return expiry!=null && expiry.isBefore(Instant.now());
	}

	public ActorStatus withText(String text){
		return new ActorStatus(text, updatedAt, expiry, apId);
	}
}
