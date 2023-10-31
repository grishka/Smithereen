package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.net.URI;

import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;

public abstract class ActivityPubLink extends ActivityPubObject{
	public URI href;

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);
		obj.addProperty("href", href.toString());
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		href=tryParseURL(obj.get("href").getAsString());
		return this;
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder(getType()+"{");
		sb.append(super.toString());
		if(href!=null){
			sb.append("href=");
			sb.append(href);
		}
		sb.append('}');
		return sb.toString();
	}
}
