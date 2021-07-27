package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;
import smithereen.jsonld.JLD;
import spark.utils.StringUtils;

public class Document extends ActivityPubObject{

	public String localID;
	public String blurHash;
	public int width;
	public int height;

	@Override
	public String getType(){
		return "Document";
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		blurHash=optString(obj, "blurhash");
		width=optInt(obj, "width");
		height=optInt(obj, "height");
		return this;
	}

	@Override
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		if(StringUtils.isNotEmpty(blurHash)){
			obj.addProperty("blurhash", blurHash);
			contextCollector.addAlias("toot", JLD.MASTODON);
			contextCollector.addAlias("blurhash", "toot:blurhash");
		}
		if(width>0)
			obj.addProperty("width", width);
		if(height>0)
			obj.addProperty("height", height);
		return obj;
	}
}
