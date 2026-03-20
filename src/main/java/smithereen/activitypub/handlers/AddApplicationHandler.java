package smithereen.activitypub.handlers;

import java.util.Objects;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubApplication;
import smithereen.activitypub.objects.activities.Add;
import smithereen.exceptions.BadRequestException;
import smithereen.model.ForeignUser;
import smithereen.model.apps.ClientApp;

public class AddApplicationHandler extends ActivityTypeHandler<ForeignUser, Add, ActivityPubApplication>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Add activity, ActivityPubApplication object){
		if(activity.target==null)
			throw new BadRequestException("target is required");
		if(Objects.equals(activity.target.getObjectID(), actor.getAppsURL())){
			ClientApp app=object.asNativeApp(context.appContext);
			if(app.isLocal()){
				context.appContext.getAppsController().addAppUser(actor, app);
			}
		}else{
			throw new BadRequestException("Unknown target");
		}
	}
}
