package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.net.URI;

import smithereen.activitypub.ParserContext;
import smithereen.activitypub.SerializerContext;
import smithereen.jsonld.JLD;

public class QuoteAuthorization extends ActivityPubObject{
	public URI interactingObject;
	public URI interactionTarget;

	@Override
	public String getType(){
		return "QuoteAuthorization";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		super.asActivityPubObject(obj, serializerContext);

		serializerContext.addAlias("QuoteAuthorization", JLD.MASTODON_QUOTES_FEP+"QuoteAuthorization");
		serializerContext.addAlias("gts", JLD.GOTOSOCIAL);
		serializerContext.addType("interactingObject", "gts:interactingObject", "@id");
		serializerContext.addType("interactionTarget", "gts:interactionTarget", "@id");

		obj.addProperty("interactingObject", interactingObject.toString());
		obj.addProperty("interactionTarget", interactionTarget.toString());

		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);

		interactingObject=tryParseURL(optString(obj, "interactingObject"));
		interactionTarget=tryParseURL(optString(obj, "interactionTarget"));

		return this;
	}
}
