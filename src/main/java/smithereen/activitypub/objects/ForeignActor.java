package smithereen.activitypub.objects;

import java.util.Set;

public interface ForeignActor{
	Set<String> WEBSITE_FIELD_KEYS=Set.of("website", "web", "web site", "blog", "homepage", "www", "site", "personal page", "personal website", "personal blog");

	boolean needUpdate();
}
