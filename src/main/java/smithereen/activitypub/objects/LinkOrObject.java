package smithereen.activitypub.objects;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.net.URI;
import java.util.Objects;

import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.SerializerContext;

public class LinkOrObject{
	public static final LinkOrObject PUBLIC=new LinkOrObject(ActivityPub.AS_PUBLIC);

	public final URI link;
	public final ActivityPubObject object;

	public LinkOrObject(URI link){
		if(link==null)
			throw new IllegalArgumentException("link can't be null");
		this.link=link;
		object=null;
	}

	public LinkOrObject(ActivityPubObject object){
		if(object==null)
			throw new IllegalArgumentException("object can't be null");
		this.object=object;
		link=null;
	}

	public JsonElement serialize(SerializerContext serializerContext){
		if(link==null && object==null)
			throw new NullPointerException("Both link and object are null");
		if(link!=null)
			return new JsonPrimitive(link.toString());
		return object.asActivityPubObject(new JsonObject(), serializerContext);
	}

	@Override
	public String toString(){
		return link==null ? object.toString() : link.toString();
	}

	@Override
	public boolean equals(Object o){
		if(this==o) return true;
		if(o instanceof URI)
			return Objects.equals(link, o);
		if(o instanceof ActivityPubObject)
			return Objects.equals(object, o);
		if(o==null || getClass()!=o.getClass()) return false;
		LinkOrObject that=(LinkOrObject) o;
		return Objects.equals(link, that.link) &&
				Objects.equals(object, that.object);
	}

	@Override
	public int hashCode(){
		return Objects.hash(link, object);
	}

	public ActivityPubObject resolve(){
		return null;
	}

	public <T extends ActivityPubObject> T requireObject(){
		return (T)Objects.requireNonNull(object);
	}

	public URI getObjectID(){
		if(link!=null)
			return link;
		return Objects.requireNonNull(object).activityPubID;
	}
}
