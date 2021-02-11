package smithereen.sparkext;

import java.net.URI;
import java.util.ArrayList;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.routes.ActivityPubRoutes;
import spark.Request;
import spark.Response;
import spark.Route;

public class ActivityPubCollectionRouteAdapter implements Route{

	private final ActivityPubCollectionRoute route;
	private final int perPage;

	public ActivityPubCollectionRouteAdapter(ActivityPubCollectionRoute route, int perPage){
		this.route=route;
		this.perPage=perPage;
	}

	@Override
	public Object handle(Request req, Response resp) throws Exception{
		int pageIndex=Math.max(1, Utils.parseIntOrDefault(req.queryParams("page"), 1));
		int offset=(pageIndex-1)*perPage;

		ActivityPubCollectionPageResponse r=route.handle(req, resp, offset, perPage);

		int total=r.totalItems;
		int lastPage=(total+perPage-1)/perPage;
		CollectionPage page=new CollectionPage(r.ordered);
		page.items=r.items;
		page.totalItems=total;
		URI baseURI=Config.localURI(req.pathInfo());
		page.activityPubID=URI.create(baseURI+"?page="+pageIndex);
		page.partOf=baseURI;
		if(pageIndex>1){
			page.first=new LinkOrObject(URI.create(baseURI+"?page=1"));
			page.prev=URI.create(baseURI+"?page="+(pageIndex-1));
		}
		if(pageIndex<lastPage){
			page.last=URI.create(baseURI+"?page="+lastPage);
			page.next=URI.create(baseURI+"?page="+(pageIndex+1));
		}
		if(pageIndex==1 && req.queryParams("page")==null){
			ActivityPubCollection collection=new ActivityPubCollection(r.ordered);
			collection.totalItems=total;
			collection.first=new LinkOrObject(page);
			collection.activityPubID=page.partOf;
			return collection.asRootActivityPubObject();
		}
		resp.type(ActivityPub.CONTENT_TYPE);
		return page.asRootActivityPubObject();
	}
}
