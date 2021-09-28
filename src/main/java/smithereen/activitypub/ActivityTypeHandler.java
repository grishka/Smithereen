package smithereen.activitypub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;

public abstract class ActivityTypeHandler<A extends Actor, T extends Activity, O extends ActivityPubObject>{
	protected static final Logger LOG=LoggerFactory.getLogger(ActivityTypeHandler.class);

	public abstract void handle(ActivityHandlerContext context, A actor, T activity, O object) throws SQLException;
}
