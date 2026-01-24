package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import smithereen.activitypub.ParserContext;
import smithereen.activitypub.SerializerContext;
import spark.utils.StringUtils;

public class Audio extends Document{

	@Nullable
	public String artist;

	@Nullable
	public String title;

	@Override
	public String getType(){
		return "Audio";
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		artist=optString(obj, "_artist");
		title=optString(obj, "_title");
		return this;
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, SerializerContext serializerContext){
		obj=super.asActivityPubObject(obj, serializerContext);
		if(StringUtils.isNotBlank(artist)){
			obj.addProperty("_artist", artist);
		}
		if(StringUtils.isNotBlank(title)){
			obj.addProperty("_title", title);
		}
		return obj;
	}
}
