package smithereen.activitypub.objects;

import org.json.JSONObject;

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
	protected ActivityPubObject parseActivityPubObject(JSONObject obj, ParserContext parserContext) throws Exception{
		super.parseActivityPubObject(obj, parserContext);
		value=Utils.sanitizeHTML(obj.getString("value"));
		return this;
	}

	@Override
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		obj.put("value", value);
		contextCollector.addAlias("value", JLD.SCHEMA_ORG+"value");
		contextCollector.addAlias("PropertyValue", JLD.SCHEMA_ORG+"PropertyValue");
		return obj;
	}
}
