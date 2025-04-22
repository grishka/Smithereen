package smithereen.activitypub.objects.activities;

import com.google.gson.JsonObject;

import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.Activity;
import smithereen.jsonld.JLD;

public class QuoteRequest extends Activity{
	@Override
	public String getType(){
		return "QuoteRequest";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		super.asActivityPubObject(obj, serializerContext);

		serializerContext.addAlias("QuoteRequest", JLD.MASTODON_QUOTES_FEP+"QuoteRequest");

		return obj;
	}
}
