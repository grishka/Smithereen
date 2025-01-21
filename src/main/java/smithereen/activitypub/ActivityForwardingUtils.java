package smithereen.activitypub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.ForeignActor;
import smithereen.model.ForeignUser;
import smithereen.model.LikeableContentObject;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.Post;
import smithereen.model.Server;
import smithereen.model.User;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.storage.GroupStorage;
import smithereen.storage.PostStorage;

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

	public static void forwardPostInteraction(ActivityHandlerContext context, Post post){
		OwnerAndAuthor oaa=context.appContext.getWallController().getContentAuthorAndOwner(post);
		try{
			if(!(oaa.owner() instanceof ForeignUser)){
				if(context.ldSignatureOwner!=null)
					context.forwardActivity(PostStorage.getInboxesForPostInteractionForwarding(post), oaa.author());
			}
		}catch(SQLException x){
			LOG.error("Failed to forward a post-related activity", x);
		}
	}

	public static void forwardCommentInteraction(ActivityHandlerContext context, Comment comment){
		OwnerAndAuthor oaa=context.appContext.getWallController().getContentAuthorAndOwner(comment);
		if(!(oaa.owner() instanceof ForeignActor) && context.ldSignatureOwner!=null){
			CommentableContentObject parent=context.appContext.getCommentsController().getCommentParentIgnoringPrivacy(comment);
			context.forwardActivity(context.appContext.getActivityPubWorker().getInboxesForComment(comment, parent), oaa.owner(), comment.parentObjectID.getRqeuiredServerFeature());
		}
	}

	public static void forwardContentInteraction(ActivityHandlerContext context, LikeableContentObject obj){
		switch(obj){
			case Post post -> forwardPostInteraction(context, post);
			case Comment comment -> forwardCommentInteraction(context, comment);
			case Photo photo -> forwardPhotoRelatedActivity(context, photo);
		}
	}
}
