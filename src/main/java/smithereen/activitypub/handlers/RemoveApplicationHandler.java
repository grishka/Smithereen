package smithereen.activitypub.handlers;

import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubApplication;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.exceptions.BadRequestException;
import smithereen.model.ForeignUser;
import smithereen.model.apps.ClientApp;

public class RemoveApplicationHandler extends ActivityTypeHandler<ForeignUser, Remove, ActivityPubApplication>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Remove activity, ActivityPubApplication object){
		if(activity.target==null)
			throw new BadRequestException("target is required");
		if(Objects.equals(activity.target.getObjectID(), actor.getAppsURL())){
			ClientApp app=object.asNativeApp(context.appContext);
			if(app.isLocal()){
				context.appContext.getAppsController().removeAppUser(actor, app);
			}
		}else{
			throw new BadRequestException("Unknown target");
		}
	}
}
