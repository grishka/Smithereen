package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;
import smithereen.jsonld.JLD;
import spark.utils.StringUtils;

public class Document extends ActivityPubObject{

	public String localID;
	public String blurHash;

	@Override
	public String getType(){
		return "Document";
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		blurHash=optString(obj, "blurhash");
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
		return obj;
	}
}
