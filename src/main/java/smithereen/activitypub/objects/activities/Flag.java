package smithereen.activitypub.objects.activities;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.exceptions.FederationException;
import smithereen.util.JsonArrayBuilder;

public class Flag extends Activity{
	public List<URI> object;

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);
		obj.add("object", object.stream().map(URI::toString).collect(JsonArrayBuilder.COLLECTOR));
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		JsonElement reportObject=obj.get("object");
		if(reportObject.isJsonArray())
			object=StreamSupport.stream(Objects.requireNonNull(reportObject.getAsJsonArray()).spliterator(), false).map(el->tryParseURL(el.getAsString())).toList();
		else if(reportObject.isJsonPrimitive())
			object=List.of(tryParseURL(reportObject.getAsString()));
		return super.parseActivityPubObject(obj, parserContext);
	}

	@Override
	public String getType(){
		return "Flag";
	}
}
