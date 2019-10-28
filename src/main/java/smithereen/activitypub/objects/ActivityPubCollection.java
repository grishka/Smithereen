package smithereen.activitypub.objects;

import org.json.JSONObject;

import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import smithereen.activitypub.ContextCollector;

public class ActivityPubCollection extends ActivityPubObject{

	public int totalItems;
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
	protected ActivityPubObject parseActivityPubObject(JSONObject obj) throws Exception{
		super.parseActivityPubObject(obj);
		totalItems=obj.optInt("totalItems");
		current=tryParseURL(obj.optString("current"));
		first=tryParseLinkOrObject(obj.optString("first"));
		last=tryParseURL(obj.optString("last"));
		items=tryParseArrayOfLinksOrObjects(obj.opt(ordered ? "orderedItems" : "items"));
		return this;
	}
}
