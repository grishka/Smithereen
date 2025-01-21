package smithereen.activitypub.objects.activities;

import com.google.gson.JsonObject;

import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.jsonld.JLD;

public class Add extends Activity{
	public boolean tentative;

	@Override
	public String getType(){
		return "Add";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);
		if(tentative){
			obj.addProperty("tentative", true);
			serializerContext.addAlias("sm", JLD.SMITHEREEN);
			serializerContext.addAlias("tentative", "sm:tentative");
		}
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		tentative=optBoolean(obj, "tentative");
		if(object.object!=null){
			// If the object is inlined, its ID must be on the same host as the actor,
			// to prevent impersonation of actors from other servers.
			ensureHostMatchesID(object.object.activityPubID, actor.link, "object.id");
		}
		return this;
	}
}
