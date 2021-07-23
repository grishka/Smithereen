package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

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
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		if(totalItems>=0)
			obj.addProperty("totalItems", totalItems);
		if(current!=null)
			obj.addProperty("current", current.toString());
		if(first!=null)
			obj.add("first", first.serialize(contextCollector));
		if(last!=null)
			obj.addProperty("last", last.toString());
		if(items!=null)
			obj.add(ordered ? "orderedItems" : "items", serializeLinkOrObjectArray(items, contextCollector));
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext) throws Exception{
		super.parseActivityPubObject(obj, parserContext);
		totalItems=optInt(obj, "totalItems");
		current=tryParseURL(optString(obj, "current"));
		first=tryParseLinkOrObject(obj.get("first"), parserContext);
		last=tryParseURL(optString(obj, "last"));
		items=tryParseArrayOfLinksOrObjects(obj.get(ordered ? "orderedItems" : "items"), parserContext);
		return this;
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder("ActivityPubCollection{");
		sb.append(super.toString());
		sb.append("totalItems=");
		sb.append(totalItems);
		if(current!=null){
			sb.append(", current=");
			sb.append(current);
		}
		if(first!=null){
			sb.append(", first=");
			sb.append(first);
		}
		if(last!=null){
			sb.append(", last=");
			sb.append(last);
		}
		if(items!=null){
			sb.append(", items=");
			sb.append(items);
		}
		sb.append(", ordered=");
		sb.append(ordered);
		sb.append('}');
		return sb.toString();
	}
}
