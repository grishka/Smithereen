package smithereen.activitypub;

import java.sql.SQLException;

import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;

public abstract class ActivityTypeHandler<A extends Actor, T extends Activity, O extends ActivityPubObject>{
	public abstract void handle(ActivityHandlerContext context, A actor, T activity, O object) throws SQLException;
}
