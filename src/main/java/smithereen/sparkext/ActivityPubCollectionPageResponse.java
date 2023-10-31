package smithereen.sparkext;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.model.PaginatedList;

public class ActivityPubCollectionPageResponse{
	public int totalItems;
	public List<LinkOrObject> items;
	public boolean ordered;

	public static ActivityPubCollectionPageResponse forObjects(List<? extends ActivityPubObject> objects, int total){
		ActivityPubCollectionPageResponse r=new ActivityPubCollectionPageResponse();
		r.items=objects.stream().map(LinkOrObject::new).collect(Collectors.toList());
		r.totalItems=total;
		return r;
	}

	public static ActivityPubCollectionPageResponse forLinks(List<URI> objects, int total){
		ActivityPubCollectionPageResponse r=new ActivityPubCollectionPageResponse();
		r.items=objects.stream().map(LinkOrObject::new).collect(Collectors.toList());
		r.totalItems=total;
		return r;
	}

	public static ActivityPubCollectionPageResponse forLinks(PaginatedList<URI> lt){
		return forLinks(lt.list, lt.total);
	}

	public static ActivityPubCollectionPageResponse forObjects(PaginatedList<? extends ActivityPubObject> lt){
		return forObjects(lt.list, lt.total);
	}

	public ActivityPubCollectionPageResponse ordered(){
		ordered=true;
		return this;
	}
}
