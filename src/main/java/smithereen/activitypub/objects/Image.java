package smithereen.activitypub.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;
import smithereen.jsonld.JLD;

public class Image extends Document{

	public int width;
	public int height;
	public float[] cropRegion;

	@Override
	public String getType(){
		return "Image";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		if(width>0)
			obj.addProperty("width", width);
		if(height>0)
			obj.addProperty("height", height);
		if(cropRegion!=null){
			JsonArray _cr=new JsonArray();
			for(float f:cropRegion) _cr.add((double)f);
			obj.add("cropRegion", _cr);
			contextCollector.addAlias("sm", JLD.SMITHEREEN);
			contextCollector.addContainerType("cropRegion", "sm:cropRegion", "@list");
		}
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		width=optInt(obj, "width");
		height=optInt(obj, "height");
		JsonArray _cr=optArray(obj, "cropRegion");
		if(_cr!=null && _cr.size()==4){
			cropRegion=new float[4];
			for(int i=0;i<4;i++)
				cropRegion[i]=_cr.get(i).getAsFloat();
		}
		return this;
	}
}
