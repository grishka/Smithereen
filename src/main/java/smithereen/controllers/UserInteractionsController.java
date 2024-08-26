package smithereen.controllers;

import java.net.URI;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.objects.activities.Like;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.LikeableContentObject;
import smithereen.model.OwnedContentObject;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.notifications.Notification;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.storage.LikeStorage;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;

public class UserInteractionsController{
	private final ApplicationContext context;

	public UserInteractionsController(ApplicationContext context){
		this.context=context;
	}

	public PaginatedList<User> getLikesForObject(LikeableContentObject object, User self, int offset, int count){
		try{
			if(!(object instanceof OwnedContentObject owned))
				throw new IllegalArgumentException();

			PaginatedList<Integer> likeIDs=LikeStorage.getLikes(owned.getObjectID(), object.getLikeObjectType(), self!=null ? self.id : 0, offset, count);
			List<User> users=UserStorage.getByIdAsList(likeIDs.list);
			return new PaginatedList<>(likeIDs, users);
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
			if(!(object instanceof OwnedContentObject contentObject))
				throw new IllegalArgumentException();

			context.getPrivacyController().enforceObjectPrivacy(self, contentObject);
			OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(contentObject);
			if(oaa.owner() instanceof User u)
				Utils.ensureUserNotBlocked(self, u);
			else if(oaa.owner() instanceof Group g)
				Utils.ensureUserNotBlocked(self, g);

			if(liked){
				int id=LikeStorage.setObjectLiked(self.id, contentObject.getObjectID(), object.getLikeObjectType(), true, apID);
				if(id==0) // Already liked
					return;
				if(!(oaa.author() instanceof ForeignUser) && contentObject.getAuthorID()!=self.id){
					Notification n=new Notification();
					n.type=Notification.Type.LIKE;
					n.actorID=self.id;
					n.objectID=contentObject.getObjectID();
					n.objectType=object.getObjectTypeForLikeNotifications();
					NotificationsStorage.putNotification(contentObject.getAuthorID(), n);
				}
				if(!(self instanceof ForeignUser))
					context.getActivityPubWorker().sendLikeActivity(object, self, id);
			}else{
				int id=LikeStorage.setObjectLiked(self.id, contentObject.getObjectID(), object.getLikeObjectType(), false, apID);
				if(id==0)
					return;
				if(!(oaa.author() instanceof ForeignUser) && contentObject.getAuthorID()!=self.id){
					NotificationsStorage.deleteNotification(object.getObjectTypeForLikeNotifications(), contentObject.getObjectID(), Notification.Type.LIKE, self.id);
				}
				if(!(self instanceof ForeignUser))
					context.getActivityPubWorker().sendUndoLikeActivity(object, self, id);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public List<User> getRepostedUsers(Post object, int count){
		try{
			if(object.isMastodonStyleRepost()){
				object=context.getWallController().getPostOrThrow(object.repostOf);
			}
			return UserStorage.getByIdAsList(PostStorage.getRepostedUsers(object.id, count));
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

	public <T extends LikeableContentObject> Map<Long, UserInteractions> getUserInteractions(List<T> objects, User self){
		if(objects.isEmpty())
			return Map.of();

		HashMap<Long, UserInteractions> interactions=new HashMap<>();
		LikeableContentObject first=objects.getFirst();
		if(first instanceof Post)
			throw new UnsupportedOperationException();

		for(T obj:objects){
			OwnedContentObject owned=(OwnedContentObject) obj;
			interactions.put(owned.getObjectID(), new UserInteractions());
		}

		try{
			LikeStorage.fillLikesInInteractions(interactions, first.getLikeObjectType(), self!=null ? self.id : 0);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}

		// TODO comments

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
