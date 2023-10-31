package smithereen.activitypub.objects.activities;

import com.google.gson.JsonObject;

import smithereen.activitypub.SerializerContext;
import smithereen.jsonld.JLD;

public class Join extends Follow{
	public boolean tentative;

	public Join(boolean tentative){
		this.tentative=tentative;
	}

	@Override
	public String getType(){
		return tentative ? "TentativeJoin" : "Join";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		serializerContext.addAlias("TentativeJoin", "sm:TentativeJoin");
		serializerContext.addAlias("sm", JLD.SMITHEREEN);
		return super.asActivityPubObject(obj, serializerContext);
	}
}
