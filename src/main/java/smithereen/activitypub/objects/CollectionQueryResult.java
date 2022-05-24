package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.net.URI;
import java.util.List;

import smithereen.activitypub.ContextCollector;
import smithereen.jsonld.JLD;

public class CollectionQueryResult extends CollectionPage{
	public CollectionQueryResult(){
		super(false);
	}

	@Override
	public String getType(){
		return "CollectionQueryResult";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		contextCollector.addAlias("sm", JLD.SMITHEREEN);
		contextCollector.addAlias("CollectionQueryResult", "sm:CollectionQueryResult");
		return super.asActivityPubObject(obj, contextCollector);
	}

	public static CollectionQueryResult empty(URI parent){
		CollectionQueryResult r=new CollectionQueryResult();
		r.items=List.of();
		r.partOf=parent;
		return r;
	}
}
