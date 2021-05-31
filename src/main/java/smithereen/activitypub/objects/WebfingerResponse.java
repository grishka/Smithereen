package smithereen.activitypub.objects;

import java.net.URI;
import java.util.List;

public class WebfingerResponse{
	// TODO add @Required here when https://github.com/google/gson/pull/1900 gets merged
	public String subject;
	public List<Link> links;

	public static class Link{
		public String rel;
		public URI href;
		public String type;
		public String template;
	}
}
