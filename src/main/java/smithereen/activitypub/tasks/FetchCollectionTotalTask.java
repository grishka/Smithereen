package smithereen.activitypub.tasks;

import java.net.URI;
import java.util.concurrent.Callable;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.ActivityPubCollection;

public class FetchCollectionTotalTask implements Callable<Long>{
	private final ApplicationContext ctx;
	private final URI collectionID;

	public FetchCollectionTotalTask(ApplicationContext ctx, URI collectionID){
		this.ctx=ctx;
		this.collectionID=collectionID;
	}

	@Override
	public Long call() throws Exception{
		return ctx.getObjectLinkResolver().resolve(collectionID, ActivityPubCollection.class, true, false, false).totalItems;
	}
}
