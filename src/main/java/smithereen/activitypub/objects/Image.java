package smithereen.activitypub.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;
import smithereen.jsonld.JLD;

public class Image extends Document{

	public float[] cropRegion;
	public boolean isGraffiti;

	@Override
	public String getType(){
		return "Image";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		if(cropRegion!=null){
			JsonArray _cr=new JsonArray();
			for(float f:cropRegion) _cr.add((double)f);
			obj.add("cropRegion", _cr);
			contextCollector.addAlias("sm", JLD.SMITHEREEN);
			contextCollector.addContainerType("cropRegion", "sm:cropRegion", "@list");
		}
		if(isGraffiti){
			contextCollector.addAlias("sm", JLD.SMITHEREEN);
			contextCollector.addAlias("graffiti", "sm:graffiti");
			obj.addProperty("graffiti", true);
		}
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		JsonArray _cr=optArray(obj, "cropRegion");
		if(_cr!=null && _cr.size()==4){
			cropRegion=new float[4];
			for(int i=0;i<4;i++)
				cropRegion[i]=_cr.get(i).getAsFloat();
		}
		isGraffiti=optBoolean(obj, "graffiti");
		return this;
	}
}
