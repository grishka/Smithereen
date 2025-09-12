package smithereen.activitypub.tasks;

import java.net.URI;
import java.util.Set;

import smithereen.ApplicationContext;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.LinkOrObject;

public class FilterCollectionAgainstSetTask extends ForwardPaginatingCollectionTask{
	private final Set<URI> filter;
	private final Set<URI> result;

	public FilterCollectionAgainstSetTask(ApplicationContext context, ActivityPubCollection collection, Set<URI> filter, Set<URI> result){
		super(context, collection);
		this.filter=filter;
		this.result=result;
	}

	@Override
	protected void doOneCollectionPage(ActivityPubCollection page){
		for(LinkOrObject lo: page.items){
			if(lo.link!=null && filter.contains(lo.link))
				result.add(lo.link);
		}
	}
}
