package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.controllers.PhotosController;
import smithereen.model.photos.PhotoAlbum;

public class FetchActorPhotoAlbumsTask extends ForwardPaginatingCollectionTask{
	private static final Logger LOG=LoggerFactory.getLogger(FetchActorPhotoAlbumsTask.class);

	private final ActivityPubWorker apw;
	private final ApplicationContext context;
	private final Actor actor;
	private final HashMap<URI, Future<Void>> fetchingPhotoAlbums;
	private final Set<Long> seenAlbums=new HashSet<>();

	public FetchActorPhotoAlbumsTask(ActivityPubWorker apw, ApplicationContext context, Actor actor, HashMap<URI, Future<Void>> fetchingPhotoAlbums){
		super(context, actor.getPhotoAlbumsURL());
		this.apw=apw;
		this.context=context;
		this.actor=actor;
		this.fetchingPhotoAlbums=fetchingPhotoAlbums;
		maxItems=PhotosController.MAX_ALBUMS_PER_OWNER;
	}

	@Override
	protected void compute(){
		super.compute();
		context.getPhotosController().deleteRemoteAlbumsNotInSet(actor, seenAlbums);
	}

	@Override
	protected void doOneCollectionPage(CollectionPage page){
		try{
			int i=0;
			for(LinkOrObject lo:page.items){
				if(lo.object instanceof ActivityPubPhotoAlbum album){
					PhotoAlbum nativeAlbum=album.asNativePhotoAlbum(context);
					context.getObjectLinkResolver().storeOrUpdateRemoteObject(nativeAlbum, album);
					seenAlbums.add(nativeAlbum.id);
					apw.fetchPhotoAlbumContents(album, nativeAlbum).get();
					i++;
					if(i==maxItems)
						break;
				}
			}
		}catch(Exception x){
			LOG.warn("Error fetching actor photo albums", x);
		}
	}
}
