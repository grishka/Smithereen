package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.net.URI;

import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;

public class Relationship extends ActivityPubObject{

	public static final URI FRIEND_OF=URI.create("http://purl.org/vocab/relationship/friendOf");

	public LinkOrObject subject;
	public LinkOrObject object;
	public URI relationship;

	@Override
	public String getType(){
		return "Relationship";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		obj.addProperty("relationship", relationship.toString());
		obj.add("object", object.serialize(contextCollector));
		obj.add("subject", subject.serialize(contextCollector));
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		relationship=tryParseURL(obj.get("relationship").getAsString());
		object=tryParseLinkOrObject(obj.get("object"), parserContext);
		subject=tryParseLinkOrObject(obj.get("subject"), parserContext);
		return this;
	}
}
