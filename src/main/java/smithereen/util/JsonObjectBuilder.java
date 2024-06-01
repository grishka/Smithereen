package smithereen.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;

public class JsonObjectBuilder{
	private JsonObject obj=new JsonObject();

	public JsonObjectBuilder add(String key, JsonElement el){
		obj.add(key, el);
		return this;
	}

	public JsonObjectBuilder add(String key, String el){
		obj.addProperty(key, el);
		return this;
	}

	public JsonObjectBuilder add(String key, Number el){
		obj.addProperty(key, el);
		return this;
	}

	public JsonObjectBuilder add(String key, boolean el){
		obj.addProperty(key, el);
		return this;
	}

	public JsonObjectBuilder add(String key, JsonObjectBuilder el){
		obj.add(key, el.build());
		return this;
	}

	public JsonObjectBuilder add(String key, JsonArrayBuilder el){
		obj.add(key, el.build());
		return this;
	}

	public <K, V> JsonObjectBuilder addAll(Map<K, V> map){
		for(Map.Entry<K, V> e:map.entrySet()){
			String key=e.getKey().toString();
			switch(e.getValue()){
				case JsonElement je -> obj.add(key, je);
				case String s -> obj.addProperty(key, s);
				case Number n -> obj.addProperty(key, n);
				case Boolean b -> obj.addProperty(key, b);
				case null -> obj.add(key, null);
				default -> {}
			}
		}
		return this;
	}

	public JsonObject build(){
		return obj;
	}
}
