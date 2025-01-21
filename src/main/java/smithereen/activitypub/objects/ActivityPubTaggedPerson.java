package smithereen.activitypub.objects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import smithereen.activitypub.ParserContext;
import smithereen.activitypub.SerializerContext;
import smithereen.model.photos.ImageRect;
import smithereen.util.JsonArrayBuilder;

public class ActivityPubTaggedPerson extends ActivityPubLink{
	public ImageRect rect;
	public boolean approved;

	@Override
	public String getType(){
		return "TaggedPerson";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		super.asActivityPubObject(obj, serializerContext);
		serializerContext.addSmAlias("TaggedPerson");
		serializerContext.addContainerType("rect", "sm:rect", "@list");
		obj.add("rect", new JsonArrayBuilder()
				.add(rect.x1())
				.add(rect.y1())
				.add(rect.x2())
				.add(rect.y2())
				.build());
		if(href!=null){
			serializerContext.addSmAlias("approved");
			obj.addProperty("approved", approved);
		}
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		if(obj.get("rect") instanceof JsonArray jr){
			if(jr.size()==4){
				float x1=getFloat(jr.get(0));
				float y1=getFloat(jr.get(1));
				float x2=getFloat(jr.get(2));
				float y2=getFloat(jr.get(3));
				if(x1>=0 && y1>=0 && x2>=0 && x2<=1 && y2>=0 && y2<=1 && x1<x2 && y1<y2){
					rect=new ImageRect(x1, y1, x2, y2);
				}
			}
		}
		approved=optBoolean(obj, "approved");
		return this;
	}

	private float getFloat(JsonElement el){
		if(el instanceof JsonPrimitive jp && jp.isNumber()){
			return jp.getAsFloat();
		}
		return Float.NaN;
	}
}
