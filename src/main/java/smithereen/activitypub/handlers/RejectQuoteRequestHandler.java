package smithereen.activitypub.handlers;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.QuoteRequest;
import smithereen.activitypub.objects.activities.Reject;
import smithereen.model.ForeignUser;

public class RejectQuoteRequestHandler extends ActivityTypeHandler<ForeignUser, Reject, QuoteRequest>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Reject activity, QuoteRequest object){
		// no-op because Smithereen does not restrict reposts and only sends quote requests so reposts display on Mastodon
	}
}
