package smithereen.activitypub.objects.activities;

import com.google.gson.JsonObject;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.util.JsonArrayBuilder;

public class Flag extends Activity{
	public List<URI> object;

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		obj.add("object", object.stream().map(URI::toString).collect(JsonArrayBuilder.COLLECTOR));
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		object=StreamSupport.stream(Objects.requireNonNull(obj.getAsJsonArray("object")).spliterator(), false).map(el->tryParseURL(el.getAsString())).toList();
		return super.parseActivityPubObject(obj, parserContext);
	}

	@Override
	public String getType(){
		return "Flag";
	}
}
