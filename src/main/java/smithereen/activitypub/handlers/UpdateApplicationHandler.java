package smithereen.activitypub.handlers;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.ActivityPubApplication;
import smithereen.activitypub.objects.activities.Update;
import smithereen.model.apps.ClientApp;

public class UpdateApplicationHandler extends ActivityTypeHandler<ActivityPubApplication, Update, ActivityPubApplication>{
	@Override
	public void handle(ActivityHandlerContext context, ActivityPubApplication actor, Update activity, ActivityPubApplication object){
		ClientApp app=object.asNativeApp(context.appContext);
		if(app.id==0){
			LOG.debug("Ignoring Update{Application} of an unknown app");
			return;
		}
		context.appContext.getAppsController().putOrUpdateForeignApp(app);
	}
}
