package smithereen.activitypub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import smithereen.jsonld.JLD;

public class SerializerContext{

	private JsonObject additionalContext;
	private JsonArray context=new JsonArray();

	public SerializerContext(){
		context.add(JLD.ACTIVITY_STREAMS);
	}

	public void addSchema(String schema){
		context.add(schema);
	}

	public void addAlias(String key, String value){
		if(additionalContext==null){
			additionalContext=new JsonObject();
			context.add(additionalContext);
		}
		additionalContext.addProperty(key, value);
	}

	public void addType(String key, String id, String type){
		if(additionalContext==null){
			additionalContext=new JsonObject();
			context.add(additionalContext);
		}
		JsonObject o=new JsonObject();
		o.addProperty("@id", id);
		o.addProperty("@type", type);
		additionalContext.add(key, o);
	}

	public void addContainerType(String key, String id, String type){
		if(additionalContext==null){
			additionalContext=new JsonObject();
			context.add(additionalContext);
		}
		JsonObject o=new JsonObject();
		o.addProperty("@id", id);
		o.addProperty("@container", type);
		additionalContext.add(key, o);
	}

	public JsonElement toContext(){
		if(context.size()==1)
			return context.get(0);
		return context;
	}
}
