package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import java.time.Instant;

import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.ParserContext;

public class Tombstone extends ActivityPubObject{

	public String formerType;
	public Instant deleted;

	@Override
	public String getType(){
		return "Tombstone";
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);
		if(formerType!=null)
			obj.addProperty("formerType", formerType);
		if(deleted!=null)
			obj.addProperty("deleted", Utils.formatDateAsISO(deleted));
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		formerType=optString(obj, "formerType");
		if(obj.has("deleted"))
			deleted=tryParseDate(obj.get("deleted").getAsString());
		return this;
	}
}
