package smithereen.controllers;

import java.net.URI;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.objects.Image;
import smithereen.activitypub.objects.LocalImage;
import smithereen.activitypub.objects.activities.Like;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.LikeableContentObject;
import smithereen.model.OwnedContentObject;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentableContentObject;
import smithereen.model.comments.CommentableObjectType;
import smithereen.model.notifications.Notification;
import smithereen.model.photos.Photo;
import smithereen.model.photos.PhotoAlbum;
import smithereen.storage.CommentStorage;
import smithereen.storage.LikeStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;

public class UserInteractionsController{
	private final ApplicationContext context;

	public UserInteractionsController(ApplicationContext context){
		this.context=context;
	}

	public PaginatedList<Integer> getLikesForObject(LikeableContentObject object, User self, int offset, int count){
		try{
			long id=object.getObjectID();
			if(object instanceof Post post)
				id=post.getIDForInteractions();
			return LikeStorage.getLikes(id, object.getLikeObjectType(), self!=null ? self.id : 0, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setObjectLiked(LikeableContentObject object, boolean liked, User self){
		setObjectLiked(object, liked, self, null);
	}

	public void setObjectLiked(LikeableContentObject object, boolean liked, User self, URI apID){
		try{
			if(object instanceof Post post && post.isMastodonStyleRepost()){
				object=context.getWallController().getPostOrThrow(post.repostOf);
			}
			context.getPrivacyController().enforceObjectPrivacy(self, object);
			OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(object);
			if(oaa.owner() instanceof User u)
				Utils.ensureUserNotBlocked(self, u);
			else if(oaa.owner() instanceof Group g)
				Utils.ensureUserNotBlocked(self, g);

			LikeableContentObject syncedObject=null;
			if(object instanceof Post post){
				if(post.action==Post.Action.AVATAR_UPDATE && post.attachments!=null &&!post.attachments.isEmpty() && post.attachments.getFirst() instanceof Image img){
					if(img instanceof LocalImage li){
						syncedObject=context.getPhotosController().getPhotoIgnoringPrivacy(li.photoID);
					}else if(img.photoApID!=null){
						try{
							syncedObject=context.getObjectLinkResolver().resolveLocally(img.photoApID, Photo.class);
						}catch(ObjectNotFoundException ignore){}
					}
				}
			}else if(object instanceof Photo photo){
				if(photo.metadata!=null && photo.metadata.correspondingPostID!=0){
					try{
						syncedObject=context.getWallController().getPostOrThrow(photo.metadata.correspondingPostID);
					}catch(ObjectNotFoundException ignore){}
				}
			}

			if(liked){
				int id=LikeStorage.setObjectLiked(self.id, object.getObjectID(), object.getLikeObjectType(), true, apID);
				if(id==0) // Already liked
					return;
				if(!(oaa.author() instanceof ForeignUser) && object.getAuthorID()!=self.id){
					OwnedContentObject related=null;
					if(object instanceof Comment comment){
						related=context.getCommentsController().getCommentParentIgnoringPrivacy(comment);
					}else if(object instanceof Post post && post.getReplyLevel()>0){
						try{
							related=context.getWallController().getPostOrThrow(post.replyKey.getFirst());
						}catch(ObjectNotFoundException ignore){}
					}
					context.getNotificationsController().createNotification(oaa.author(), Notification.Type.LIKE, object, related, self);
				}
				if(!(self instanceof ForeignUser))
					context.getActivityPubWorker().sendLikeActivity(object, self, id);
				if(syncedObject!=null)
					LikeStorage.setObjectLiked(self.id, syncedObject.getObjectID(), syncedObject.getLikeObjectType(), true, null);
				context.getFriendsController().incrementHintsRank(self, oaa.author(), 2);
				if(oaa.owner() instanceof User u && u.id!=oaa.author().id)
					context.getFriendsController().incrementHintsRank(self, u, 1);
				else if(oaa.owner() instanceof Group g)
					context.getGroupsController().incrementHintsRank(self, g, 2);
			}else{
				int id=LikeStorage.setObjectLiked(self.id, object.getObjectID(), object.getLikeObjectType(), false, apID);
				if(id==0)
					return;
				if(syncedObject!=null)
					LikeStorage.setObjectLiked(self.id, syncedObject.getObjectID(), syncedObject.getLikeObjectType(), false, null);
				if(!(oaa.author() instanceof ForeignUser) && object.getAuthorID()!=self.id){
					NotificationsStorage.deleteNotification(object.getObjectTypeForLikeNotifications(), object.getObjectID(), Notification.Type.LIKE, self.id);
					if(syncedObject!=null)
						NotificationsStorage.deleteNotification(syncedObject.getObjectTypeForLikeNotifications(), syncedObject.getObjectID(), Notification.Type.LIKE, self.id);
				}
				if(!(self instanceof ForeignUser))
					context.getActivityPubWorker().sendUndoLikeActivity(object, self, id);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<Integer> getRepostedUsers(Post object, int count){
		try{
			if(object.isMastodonStyleRepost()){
				object=context.getWallController().getPostOrThrow(object.repostOf);
			}
			return PostStorage.getRepostedUsers(object.id, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Post> getLikedPosts(User self, boolean topLevelOnly, int offset, int count){
		try{
			PaginatedList<Integer> ids;
			if(topLevelOnly)
				ids=LikeStorage.getLikedPostsTopLevelOnly(self.id, offset, count);
			else{
				PaginatedList<Long> longIDs=LikeStorage.getLikedObjectIDs(self.id, Like.ObjectType.POST, offset, count);
				ids=new PaginatedList<>(longIDs, longIDs.list.stream().map(Long::intValue).toList());
			}
			Map<Integer, Post> posts=PostStorage.getPostsByID(ids.list);
			return new PaginatedList<>(ids, ids.list.stream().map(posts::get).filter(Objects::nonNull).toList());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public <T extends LikeableContentObject> Map<Long, UserInteractions> getUserInteractions(Collection<T> objects, User self){
		if(objects.isEmpty())
			return Map.of();

		HashMap<Long, UserInteractions> interactions=new HashMap<>();
		LikeableContentObject first=objects.iterator().next();
		if(first instanceof Post)
			throw new UnsupportedOperationException();

		for(T obj:objects){
			interactions.put(obj.getObjectID(), new UserInteractions());
		}

		try{
			LikeStorage.fillLikesInInteractions(interactions, first.getLikeObjectType(), self!=null ? self.id : 0);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}

		if(first.getLikeObjectType()==Like.ObjectType.PHOTO){
			Set<Long> needAlbums=objects.stream().map(o->((Photo)o).albumID).collect(Collectors.toSet());
			Collection<PhotoAlbum> albums=context.getPhotosController().getAlbumsIgnoringPrivacy(needAlbums).values();
			Map<Integer, User> users=context.getUsersController().getUsers(albums.stream().map(a->a.ownerID).filter(id->id>0).collect(Collectors.toSet()));
			Set<Long> commentableAlbums=albums.stream().filter(a->{
				if(a.ownerID<0)
					return !a.flags.contains(PhotoAlbum.Flag.GROUP_DISABLE_COMMENTING);
				return context.getPrivacyController().checkUserPrivacy(self, users.get(a.ownerID), a.commentPrivacy);
			}).map(a->a.id).collect(Collectors.toSet());
			for(T obj:objects){
				if(!(obj instanceof Photo photo))
					throw new IllegalStateException();
				interactions.get(obj.getObjectID()).canComment=commentableAlbums.contains(photo.albumID);
			}
		}

		if(first instanceof CommentableContentObject commentable){
			try{
				Map<Long, Integer> counts=CommentStorage.getCommentCounts(commentable.getCommentParentID().type(), objects.stream().map(LikeableContentObject::getObjectID).collect(Collectors.toSet()));
				for(Map.Entry<Long, UserInteractions> e:interactions.entrySet()){
					e.getValue().commentCount=counts.getOrDefault(e.getKey(), 0);
				}
			}catch(SQLException x){
				throw new InternalServerErrorException(x);
			}
		}

		return interactions;
	}

	public PaginatedList<Long> getLikedObjects(User self, Like.ObjectType type, int offset, int count){
		try{
			return LikeStorage.getLikedObjectIDs(self.id, type, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
