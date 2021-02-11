package smithereen.activitypub.objects;

import org.json.JSONObject;

import java.net.URI;
import java.util.List;

import smithereen.activitypub.ContextCollector;
import smithereen.activitypub.ParserContext;

public class ActivityPubCollection extends ActivityPubObject{

	public int totalItems=-1;
	public URI current;
	public LinkOrObject first;
	public URI last;
	public List<LinkOrObject> items;

	public boolean ordered;

	public ActivityPubCollection(boolean ordered){
		this.ordered=ordered;
	}

	@Override
	public String getType(){
		return ordered ? "OrderedCollection" : "Collection";
	}

	@Override
	public JSONObject asActivityPubObject(JSONObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		if(totalItems>=0)
			obj.put("totalItems", totalItems);
		if(current!=null)
			obj.put("current", current.toString());
		if(first!=null)
			obj.put("first", first.serialize(contextCollector));
		if(last!=null)
			obj.put("last", last.toString());
		if(items!=null)
			obj.put(ordered ? "orderedItems" : "items", serializeLinkOrObjectArray(items, contextCollector));
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JSONObject obj, ParserContext parserContext) throws Exception{
		super.parseActivityPubObject(obj, parserContext);
		totalItems=obj.optInt("totalItems", -1);
		current=tryParseURL(obj.optString("current"));
		first=tryParseLinkOrObject(obj.optString("first"), parserContext);
		last=tryParseURL(obj.optString("last"));
		items=tryParseArrayOfLinksOrObjects(obj.opt(ordered ? "orderedItems" : "items"), parserContext);
		return this;
	}
}
