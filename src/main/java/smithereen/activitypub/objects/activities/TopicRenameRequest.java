package smithereen.activitypub.objects.activities;

import com.google.gson.JsonObject;

import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.Activity;

public class TopicRenameRequest extends Activity{
	@Override
	public String getType(){
		return "TopicRenameRequest";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		serializerContext.addSmAlias("TopicRenameRequest");
		return super.asActivityPubObject(obj, serializerContext);
	}
}
