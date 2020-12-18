package smithereen.activitypub.objects;

import org.json.JSONObject;

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
	protected ActivityPubObject parseActivityPubObject(JSONObject obj, ParserContext parserContext) throws Exception{
		super.parseActivityPubObject(obj, parserContext);
		blurHash=obj.optString("blurhash");
		return this;
	}

	@Override
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		if(StringUtils.isNotEmpty(blurHash)){
			obj.put("blurhash", blurHash);
			contextCollector.addAlias("toot", JLD.MASTODON);
			contextCollector.addAlias("blurhash", "toot:blurhash");
		}
		return obj;
	}
}
