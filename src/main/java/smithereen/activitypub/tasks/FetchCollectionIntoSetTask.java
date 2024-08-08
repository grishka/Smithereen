package smithereen.activitypub.tasks;

import java.net.URI;
import java.util.Set;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.LinkOrObject;

public class FetchCollectionIntoSetTask extends ForwardPaginatingCollectionTask{
	private final Set<URI> set;

	public FetchCollectionIntoSetTask(ApplicationContext context, ActivityPubCollection collection, Set<URI> set, int maxItems){
		super(context, collection);
		this.set=set;
		this.maxItems=maxItems;
	}

	@Override
	protected void doOneCollectionPage(CollectionPage page){
		for(LinkOrObject lo: page.items){
			if(lo.link!=null)
				set.add(lo.link);
		}
	}
}
