package smithereen.activitypub.objects;

import java.net.URI;
import java.util.List;

import smithereen.activitypub.ActivityPub;

public class WebfingerResponse{
	// TODO add @Required here when https://github.com/google/gson/pull/1900 gets merged
	public String subject;
	public List<Link> links;

	public URI getActivityPubActorID(){
		for(WebfingerResponse.Link link:links){
			if("self".equals(link.rel) && ("application/activity+json".equals(link.type) || ActivityPub.CONTENT_TYPE.equals(link.type)) && link.href!=null){
				return link.href;
			}
		}
		return null;
	}

	public static class Link{
		public String rel;
		public URI href;
		public String type;
		public String template;
	}
}
