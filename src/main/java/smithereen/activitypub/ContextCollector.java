package smithereen.activitypub;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;

import smithereen.jsonld.JLD;

public class ContextCollector{

	private JSONObject additionalContext;
	private JSONArray context=new JSONArray(Collections.singletonList(JLD.ACTIVITY_STREAMS));

	public void addSchema(String schema){
		context.put(schema);
	}

	public void addAlias(String key, String value){
		if(additionalContext==null){
			additionalContext=new JSONObject();
			context.put(additionalContext);
		}
		additionalContext.put(key, value);
	}

	public void addType(String key, String id, String type){
		if(additionalContext==null){
			additionalContext=new JSONObject();
			context.put(additionalContext);
		}
		JSONObject o=new JSONObject();
		o.put("@id", id);
		o.put("@type", type);
		additionalContext.put(key, o);
	}

	public void addContainerType(String key, String id, String type){
		if(additionalContext==null){
			additionalContext=new JSONObject();
			context.put(additionalContext);
		}
		JSONObject o=new JSONObject();
		o.put("@id", id);
		o.put("@container", type);
		additionalContext.put(key, o);
	}

	public Object toContext(){
		if(context.length()==1)
			return context.get(0);
		return context;
	}
}
