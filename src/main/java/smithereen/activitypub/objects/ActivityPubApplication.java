package smithereen.activitypub.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.activitypub.ParserContext;
import smithereen.model.apps.ClientApp;
import smithereen.model.apps.ClientAppType;
import spark.utils.StringUtils;

public class ActivityPubApplication extends Actor{
	public List<String> redirectUri;

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);

		if(obj.has("redirectUri")){
			redirectUri=switch(obj.get("redirectUri")){
				case JsonPrimitive jp when jp.isString() -> List.of(jp.getAsString());
				case JsonArray ja -> ja.asList().stream().filter(JsonElement::isJsonPrimitive).map(JsonElement::getAsString).toList();
				default -> null;
			};
		}

		return this;
	}

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
		app.allowedRedirectURIs=redirectUri==null ? Set.of() : new HashSet<>(redirectUri);
		return app;
	}

	public static ActivityPubApplication fromNativeApp(ClientApp app, ApplicationContext ctx){
		ActivityPubApplication aa=new LocalActivityPubApplication(app);
		aa.activityPubID=app.getActivityPubID();
		if(StringUtils.isNotEmpty(app.username))
			aa.username=app.username;
		aa.domain=app.domain;
		aa.publicKey=app.publicKey;
		aa.name=app.name;
		aa.summary=app.description;
		if(app.logo!=null)
			aa.icon=List.of(app.logo);
		if(app.developerID>0)
			aa.attributedTo=ctx.getUsersController().getUserOrThrow(app.developerID).activityPubID;
		aa.inbox=Config.localURI("/apps/"+app.id+"/inbox");
		aa.sharedInbox=Config.localURI("/activitypub/sharedInbox");
		if(app.allowedRedirectURIs!=null)
			aa.redirectUri=new ArrayList<>(app.allowedRedirectURIs);
		return aa;
	}

	@Override
	protected boolean canBeFollowed(){
		return false;
	}

	@Override
	protected boolean canFollowOtherActors(){
		return false;
	}
}
