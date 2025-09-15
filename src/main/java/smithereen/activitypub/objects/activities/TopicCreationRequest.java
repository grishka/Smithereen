package smithereen.activitypub.objects.activities;

import com.google.gson.JsonObject;

import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.Activity;

public class TopicCreationRequest extends Activity{
	@Override
	public String getType(){
		return "TopicCreationRequest";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		super.asActivityPubObject(obj, serializerContext);

		serializerContext.addSmAlias("TopicCreationRequest");

		return obj;
	}
}
