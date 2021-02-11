package smithereen.sparkext;

import smithereen.activitypub.objects.ActivityPubObject;
import spark.Request;
import spark.Response;

@FunctionalInterface
public interface ActivityPubCollectionRoute{
	ActivityPubCollectionPageResponse handle(Request req, Response resp, int offset, int count) throws Exception;
}
