package smithereen.activitypub.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;

import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;
import smithereen.jsonld.JLD;

public class Image extends Document{

	public float[] cropRegion;
	public boolean isGraffiti;
	public URI photoApID;

	@Override
	public String getType(){
		return "Image";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);
		if(cropRegion!=null){
			JsonArray _cr=new JsonArray();
			for(float f:cropRegion) _cr.add((double)f);
			obj.add("cropRegion", _cr);
			serializerContext.addAlias("sm", JLD.SMITHEREEN);
			serializerContext.addContainerType("cropRegion", "sm:cropRegion", "@list");
		}
		if(isGraffiti){
			serializerContext.addAlias("sm", JLD.SMITHEREEN);
			serializerContext.addAlias("graffiti", "sm:graffiti");
			obj.addProperty("graffiti", true);
		}
		if(photoApID!=null){
			serializerContext.addSmIdType("photo");
			obj.addProperty("photo", photoApID.toString());
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
		photoApID=tryParseURL(optString(obj, "photo"));
		return this;
	}
}
