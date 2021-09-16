package smithereen.activitypub.objects;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

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
	public JsonObject asActivityPubObject(JsonObject obj, ContextCollector contextCollector){
		obj=super.asActivityPubObject(obj, contextCollector);
		if(partOf!=null)
			obj.addProperty("partOf", partOf.toString());
		if(prev!=null)
			obj.addProperty("prev", prev.toString());
		if(next!=null)
			obj.addProperty("next", next.toString());
		return obj;
	}

	@Override
	protected ActivityPubObject parseActivityPubObject(JsonObject obj, ParserContext parserContext){
		super.parseActivityPubObject(obj, parserContext);
		partOf=tryParseURL(optString(obj, "partOf"));
		prev=tryParseURL(optString(obj, "prev"));
		next=tryParseURL(optString(obj, "next"));
		return this;
	}

	@Override
	public void validate(@Nullable URI parentID, String propertyName){
		super.validate(parentID, propertyName);
		ensureHostMatchesID(partOf, parentID, propertyName+".partOf");
		ensureHostMatchesID(prev, parentID, propertyName+".prev");
		ensureHostMatchesID(next, parentID, propertyName+".next");
	}

	@Override
	public String toString(){
		StringBuilder sb=new StringBuilder("CollectionPage{");
		sb.append(super.toString());
		if(partOf!=null){
			sb.append("partOf=");
			sb.append(partOf);
		}
		if(prev!=null){
			sb.append(", prev=");
			sb.append(prev);
		}
		if(next!=null){
			sb.append(", next=");
			sb.append(next);
		}
		sb.append('}');
		return sb.toString();
	}
}
