package smithereen.activitypub.objects;

import org.json.JSONObject;

import java.net.URI;

import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;

public class CollectionPage extends ActivityPubCollection{

	public URI partOf;
	public URI prev;
	public URI next;

	public CollectionPage(boolean ordered){
		super(ordered);
	}

	@Override
	public String getType(){
		return ordered ? "OrderedCollectionPage" : "CollectionPage";
	}

	@Override
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		if(partOf!=null)
			obj.put("partOf", partOf.toString());
		if(prev!=null)
			obj.put("prev", prev.toString());
		if(next!=null)
			obj.put("next", next.toString());
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JSONObject obj, ParserContext parserContext) throws Exception{
		super.parseActivityPubObject(obj, parserContext);
		partOf=tryParseURL(obj.optString("partOf"));
		prev=tryParseURL(obj.optString("prev"));
		next=tryParseURL(obj.optString("next"));
		return this;
	}
}
