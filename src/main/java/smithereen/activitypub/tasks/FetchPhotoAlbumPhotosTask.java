package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubPhoto;
import smithereen.activitypub.objects.ActivityPubPhotoAlbum;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.controllers.PhotosController;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.storage.utils.Pair;

public class FetchPhotoAlbumPhotosTask extends ForwardPaginatingCollectionTask{
	private static final Logger LOG=LoggerFactory.getLogger(FetchPhotoAlbumPhotosTask.class);
	private final ActivityPubWorker apw;
	private final HashMap<URI, Future<Void>> fetchingPhotoAlbums;
	private final PhotoAlbum nativeAlbum;
	private final ActivityPubPhotoAlbum album;
	private final Set<Long> seenPhotos=new HashSet<>();

	public FetchPhotoAlbumPhotosTask(ApplicationContext context, ActivityPubPhotoAlbum collection, PhotoAlbum nativeAlbum, ActivityPubWorker apw, HashMap<URI, Future<Void>> fetchingPhotoAlbums){
		super(context, collection);
		album=collection;
		this.apw=apw;
		this.fetchingPhotoAlbums=fetchingPhotoAlbums;
		this.nativeAlbum=nativeAlbum;
		maxItems=PhotosController.MAX_PHOTOS_PER_ALBUM;
	}

	@Override
	protected void compute(){
		try{
			super.compute();
			if(album.preview!=null && album.preview.link!=null){
				long newCoverID=context.getPhotosController().getPhotoIdByActivityPubId(album.preview.link);
				if(newCoverID!=0 && newCoverID!=nativeAlbum.coverID){
					nativeAlbum.coverID=newCoverID;
					context.getObjectLinkResolver().storeOrUpdateRemoteObject(nativeAlbum, album);
				}
			}
			PhotoAlbum finalAlbum=context.getPhotosController().getAlbumIgnoringPrivacy(nativeAlbum.id);
			if(finalAlbum.numPhotos>seenPhotos.size()){
				context.getPhotosController().deleteRemotePhotosNotInSet(finalAlbum, seenPhotos);
			}
		}finally{
			synchronized(apw){
				fetchingPhotoAlbums.remove(collectionID);
			}
		}
	}

	@Override
	protected void doOneCollectionPage(CollectionPage page){
		try{
			ArrayList<Pair<Photo, ActivityPubPhoto>> photos=new ArrayList<>();
			for(LinkOrObject lo:page.items){
				ActivityPubPhoto photo;
				if(lo.link!=null){
					photo=context.getObjectLinkResolver().resolve(lo.link, ActivityPubPhoto.class, true, false, false);
				}else if(lo.object instanceof ActivityPubPhoto p){
					photo=p;
				}else{
					LOG.debug("Photo album collection item is of unexpected type {}, skipping", lo.object.getType());
					continue;
				}
				Photo nPhoto=photo.asNativePhoto(context);
				photos.add(new Pair<>(nPhoto, photo));
				seenPhotos.add(nPhoto.id);
			}
			if(!photos.isEmpty()){
				context.getPhotosController().putOrUpdateForeignPhotos(photos);
			}
		}catch(Exception x){
			LOG.warn("Error processing photo album page", x);
		}
	}
}
