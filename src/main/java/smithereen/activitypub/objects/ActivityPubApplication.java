package smithereen.activitypub.objects;

import java.net.URI;

import smithereen.ApplicationContext;
import smithereen.model.apps.ClientApp;
import smithereen.model.apps.ClientAppType;
import spark.utils.StringUtils;

public class ActivityPubApplication extends Actor{
	@Override
	public int getLocalID(){
		return 0;
	}

	@Override
	public URI getWallURL(){
		return null;
	}

	@Override
	public URI getWallCommentsURL(){
		return null;
	}

	@Override
	public URI getPhotoAlbumsURL(){
		return null;
	}

	@Override
	public String getTypeAndIdForURL(){
		return "";
	}

	@Override
	public String getName(){
		return name;
	}

	@Override
	public String serializeProfileFields(){
		return null;
	}

	@Override
	public String getType(){
		return "Application";
	}

	public ClientApp asNativeApp(ApplicationContext ctx){
		ClientApp app=new ClientApp();
		app.id=ctx.getAppsController().getAppIdByActivityPubID(activityPubID);
		app.apID=activityPubID;
		app.username=username;
		app.domain=domain;
		app.publicKey=publicKey;
		app.name=StringUtils.isEmpty(name) ? activityPubID.toString() : name;
		app.type=ClientAppType.STANDALONE; // TODO
		app.description=summary;
		app.logo=getAvatarImage();
		app.developerID=0; // TODO
		app.apInbox=inbox;
		app.apSharedInbox=sharedInbox;
		return app;
	}
}
