package smithereen.activitypub;

import java.sql.SQLException;

import smithereen.exceptions.BadRequestException;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;

public abstract class NestedActivityTypeHandler<A extends Actor, T extends Activity, N extends Activity, O extends ActivityPubObject> extends ActivityTypeHandler<A, T, O>{
	public abstract void handle(ActivityHandlerContext context, A actor, T activity, N nested, O object) throws SQLException;

	@Override
	public final void handle(ActivityHandlerContext context, A actor, T activity, O object) throws SQLException{
		if(activity.object.object==null)
			throw new BadRequestException("Nested activity must not be a link");
		if(!(activity.object.object instanceof Activity))
			throw new BadRequestException("Nested activity must be an Activity subtype");
		Activity a=(Activity) activity.object.object;
		handle(context, actor, activity, (N)a, object);
	}
}
