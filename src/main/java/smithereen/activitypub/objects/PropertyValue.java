package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;
import smithereen.jsonld.JLD;

public class PropertyValue extends ActivityPubObject{

	public String value;

	public PropertyValue(){}

	public PropertyValue(String name, String value){
		this.name=name;
		this.value=value;
	}

	@Override
	public String getType(){
		return "PropertyValue";
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		value=Utils.sanitizeHTML(obj.get("value").getAsString());
		return this;
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);
		obj.addProperty("value", value);
		serializerContext.addAlias("value", JLD.SCHEMA_ORG+"value");
		serializerContext.addAlias("PropertyValue", JLD.SCHEMA_ORG+"PropertyValue");
		return obj;
	}
}
