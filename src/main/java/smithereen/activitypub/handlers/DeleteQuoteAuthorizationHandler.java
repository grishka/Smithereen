package smithereen.activitypub.handlers;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.QuoteAuthorization;
import smithereen.activitypub.objects.activities.Delete;
import smithereen.model.ForeignUser;

public class DeleteQuoteAuthorizationHandler extends ActivityTypeHandler<ForeignUser, Delete, QuoteAuthorization>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Delete activity, QuoteAuthorization object){
		// no-op because Smithereen does not allow detaching quotes
	}
}
