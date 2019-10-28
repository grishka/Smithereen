package smithereen.activitypub.objects;

import java.net.URI;
import java.net.URL;

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
		if(link!=null)
			return link.toString();
		return object.asActivityPubObject(null, contextCollector);
	}

	@Override
	public String toString(){
		return link==null ? object.toString() : link.toString();
	}
}
