package smithereen.activitypub.objects;

import java.net.URI;
import java.net.URL;
import java.util.Objects;

import smithereen.activitypub.ContextCollector;

public class LinkOrObject{
	public final URI link;
	public final ActivityPubObject object;

	public LinkOrObject(URI link){
		this.link=link;
		object=null;
	}

	public LinkOrObject(ActivityPubObject object){
		this.object=object;
		link=null;
	}

	public Object serialize(ContextCollector contextCollector){
		if(link==null && object==null)
			throw new NullPointerException("Both link and object are null");
		if(link!=null)
			return link.toString();
		return object.asActivityPubObject(null, contextCollector);
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
}
