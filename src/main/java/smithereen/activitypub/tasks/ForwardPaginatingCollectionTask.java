package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.exceptions.FederationException;
import smithereen.util.NoResultCallable;

/**
 * Base class for tasks that deal with collections. Handles paginating through a collection.
 */
public abstract class ForwardPaginatingCollectionTask extends NoResultCallable{
	protected static final Logger LOG=LoggerFactory.getLogger(ForwardPaginatingCollectionTask.class);

	protected final URI collectionID;
	protected final ApplicationContext context;
	protected ActivityPubCollection collection;
	protected long totalItems, processedItems;
	protected int maxItems=Integer.MAX_VALUE;

	public ForwardPaginatingCollectionTask(ApplicationContext context, URI collectionID){
		this.collectionID=Objects.requireNonNull(collectionID);
		this.context=context;
	}

	public ForwardPaginatingCollectionTask(ApplicationContext context, ActivityPubCollection collection){
		this.collection=collection;
		collectionID=collection.activityPubID;
		this.context=context;
	}

	@Override
	protected void compute(){
		if(collection==null){
			LOG.trace("Fetching collection {}", collectionID);
			collection=context.getObjectLinkResolver().resolve(collectionID, ActivityPubCollection.class, true, false, false);
		}
		totalItems=Math.min(collection.totalItems, maxItems);
		onCollectionLoaded();
		if(collection.items!=null){
			LOG.trace("Collection {} has items in it directly, processing as single page", collectionID);
			doOneCollectionPage(collection);
		}else if(collection.totalItems==0){
			LOG.trace("Collection {} has 0 total items", collectionID);
			collection.items=List.of();
			doOneCollectionPage(collection);
		}else{
			if(collection.first==null)
				throw new FederationException("neither collection.first nor collection.items are not present");
			if(collection.first.object!=null)
				processCollectionPage(collection.first.requireObject());
			else
				loadNextCollectionPage(collection.first.link);
		}
	}

	private void loadNextCollectionPage(URI id){
		LOG.trace("Fetching page {} for collection {}", id, collectionID);
		CollectionPage page=context.getObjectLinkResolver().resolve(id, CollectionPage.class, true, false, false);
		processCollectionPage(page);
	}

	private void processCollectionPage(CollectionPage page){
		if(page.items==null || page.items.isEmpty()){
			LOG.trace("Finished processing collection {} because items array was null or empty", collectionID);
			return;
		}
		doOneCollectionPage(page);
		processedItems+=page.items.size();
		if(totalItems>=0 && processedItems>=totalItems){
			LOG.trace("Finished processing collection {} because item count limit {} was reached", collectionID, totalItems);
			return;
		}
		if(page.next!=null){
			loadNextCollectionPage(page.next);
		}else{
			LOG.trace("Finished processing collection {} because there are no next pages", collectionID);
		}
	}

	protected abstract void doOneCollectionPage(ActivityPubCollection page);

	protected void onCollectionLoaded(){
	}
}
