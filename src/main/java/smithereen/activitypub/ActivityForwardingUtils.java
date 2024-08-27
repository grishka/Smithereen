package smithereen.activitypub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import smithereen.activitypub.objects.Actor;
import smithereen.model.Server;
import smithereen.model.User;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.storage.GroupStorage;

public class ActivityForwardingUtils{
	private static final Logger LOG=LoggerFactory.getLogger(ActivityForwardingUtils.class);

	public static void forwardPhotoRelatedActivity(ActivityHandlerContext context, Photo photo){
		try{
			if(photo.apID==null && context.ldSignatureOwner!=null){
				PhotoAlbum album=context.appContext.getPhotosController().getAlbumIgnoringPrivacy(photo.albumID);
				Set<URI> inboxes;
				Actor owner;
				if(album.ownerID>0){
					inboxes=new HashSet<>();
					User ownerUser=context.appContext.getUsersController().getUserOrThrow(album.ownerID);
					owner=ownerUser;
					context.appContext.getActivityPubWorker().getInboxesWithPrivacy(inboxes, ownerUser, album.viewPrivacy);
				}else{
					inboxes=new HashSet<>(GroupStorage.getGroupMemberInboxes(-album.ownerID));
					owner=context.appContext.getGroupsController().getGroupOrThrow(-album.ownerID);
				}
				context.forwardActivity(inboxes, owner, Server.Feature.PHOTO_ALBUMS);
			}
		}catch(SQLException x){
			LOG.error("Failed to forward a photo-related activity", x);
		}
	}
}
