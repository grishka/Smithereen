package smithereen.activitypub;

import java.net.URI;

import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.Actor;

public record ActivityDeliveryRetry(Activity activity, URI inbox, Actor actor, int attemptNumber){
	public long getDelayForThisAttempt(){
		return switch(attemptNumber){
			case 1 -> 30_000; // 30 seconds
			case 2 -> 60_000; // 1 minute
			case 3 -> 5*60_000; // 5 minutes
			case 4 -> 600_000; // 10 minutes
			case 5 -> 3*600_000; // 30 minutes
			case 6 -> 3600_000; // 1 hour
			case 7 -> 3*3600_000; // 3 hours
			case 8 -> 6*3600_000; // 6 hours
			case 9 -> 12*3600_000; // 12 hours
			default -> throw new IllegalStateException();
		};
	}

	public boolean needMoreAttempts(){
		return attemptNumber<10;
	}
}
