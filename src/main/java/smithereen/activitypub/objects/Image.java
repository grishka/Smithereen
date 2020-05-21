package smithereen.activitypub.objects;

import org.json.JSONArray;
import org.json.JSONObject;

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
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		if(width>0)
			obj.put("width", width);
		if(height>0)
			obj.put("height", height);
		if(cropRegion!=null){
			JSONArray _cr=new JSONArray();
			for(float f:cropRegion) _cr.put((double)f);
			obj.put("cropRegion", _cr);
			contextCollector.addAlias("sm", JLD.SMITHEREEN);
			contextCollector.addContainerType("cropRegion", "sm:cropRegion", "@list");
		}
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JSONObject obj, ParserContext parserContext) throws Exception{
		super.parseActivityPubObject(obj, parserContext);
		width=obj.optInt("width");
		height=obj.optInt("height");
		JSONArray _cr=obj.optJSONArray("cropRegion");
		if(_cr!=null && _cr.length()==4){
			cropRegion=new float[4];
			for(int i=0;i<4;i++)
				cropRegion[i]=_cr.optFloat(i);
		}
		return this;
	}
}
