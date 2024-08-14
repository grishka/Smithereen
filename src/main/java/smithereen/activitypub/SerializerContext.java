package smithereen.activitypub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.function.Supplier;

import smithereen.ApplicationContext;
import smithereen.jsonld.JLD;

public class SerializerContext{

	private JsonObject additionalContext;
	private JsonArray context=new JsonArray();
	public final ApplicationContext appContext;
	private String requesterDomain;
	private Supplier<String> requesterDomainSupplier;
	private boolean requesterDomainDetermined;

	public SerializerContext(ApplicationContext appContext, String requesterDomain){
		this.appContext=appContext;
		this.requesterDomain=requesterDomain;
		requesterDomainDetermined=true;
		context.add(JLD.ACTIVITY_STREAMS);
	}

	public SerializerContext(ApplicationContext appContext, Supplier<String> requesterDomainSupplier){
		this.appContext=appContext;
		this.requesterDomainSupplier=requesterDomainSupplier;
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

	/**
	 * Convenience for addAlias(name, "sm:"+name)
	 * @param name
	 */
	public void addSmAlias(String name){
		addAlias("sm", JLD.SMITHEREEN);
		addAlias(name, "sm:"+name);
	}

	public void addSmIdType(String name){
		addAlias("sm", JLD.SMITHEREEN);
		addType(name, "sm:"+name, "@id");
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

	public JsonElement getJLDContext(){
		if(context.size()==1)
			return context.get(0);
		return context;
	}

	public String getRequesterDomain(){
		if(requesterDomainDetermined){
			return requesterDomain;
		}
		requesterDomain=requesterDomainSupplier.get();
		requesterDomainDetermined=true;
		return requesterDomain;
	}
}
