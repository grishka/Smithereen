package smithereen.activitypub.objects.activities;

import com.google.gson.JsonObject;

import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.jsonld.JLD;

public class Add extends Activity{
	public boolean tentative;

	@Override
	public String getType(){
		return "Add";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		if(tentative){
			obj.addProperty("tentative", true);
			contextCollector.addAlias("sm", JLD.SMITHEREEN);
			contextCollector.addAlias("tentative", "sm:tentative");
		}
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		tentative=optBoolean(obj, "tentative");
		return this;
	}
}
