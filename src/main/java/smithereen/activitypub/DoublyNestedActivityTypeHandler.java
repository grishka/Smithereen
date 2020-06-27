package smithereen.activitypub;

import java.sql.SQLException;

import smithereen.BadRequestException;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;

public abstract class DoublyNestedActivityTypeHandler<A extends Actor, T extends Activity, N extends Activity, NN extends Activity, O extends ActivityPubObject> extends NestedActivityTypeHandler<A, T, N, O>{
	public abstract void handle(ActivityHandlerContext context, A actor, T activity, N nested, NN innerNested, O object) throws SQLException;

	@Override
	public final void handle(ActivityHandlerContext context, A actor, T activity, N nested, O object) throws SQLException{
		if(nested.object.object==null)
			throw new BadRequestException("Nested activity must not be a link");
		if(!(nested.object.object instanceof Activity))
			throw new BadRequestException("Nested activity must be an Activity subtype");
		Activity a=(Activity) nested.object.object;
		handle(context, actor, activity, nested, (NN)a, object);
	}
}
