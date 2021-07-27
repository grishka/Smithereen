package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import smithereen.Utils;
import smithereen.activitypub.ContextCollector;
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
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		obj.addProperty("value", value);
		contextCollector.addAlias("value", JLD.SCHEMA_ORG+"value");
		contextCollector.addAlias("PropertyValue", JLD.SCHEMA_ORG+"PropertyValue");
		return obj;
	}
}
