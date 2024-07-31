package smithereen.controllers;

import java.sql.SQLException;
import java.util.Collections;
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

	public PaginatedList<User> getLikesForObject(Post object, User self, int offset, int count){
		try{
			if(object.isMastodonStyleRepost()){
				object=context.getWallController().getPostOrThrow(object.repostOf);
			}
			UserInteractions interactions=PostStorage.getPostInteractions(Collections.singletonList(object.id), 0).get(object.id);
			List<User> users=UserStorage.getByIdAsList(LikeStorage.getPostLikes(object.id, self!=null ? self.id : 0, offset, count));
			return new PaginatedList<>(users, interactions.likeCount, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setObjectLiked(LikeableContentObject object, boolean liked, User self){
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
				int id=LikeStorage.setObjectLiked(self.id, contentObject.getObjectID(), object.getLikeObjectType(), true);
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
				if(object instanceof Post post) // TODO
					context.getActivityPubWorker().sendLikeActivity(post, self, id);
			}else{
				int id=LikeStorage.setObjectLiked(self.id, contentObject.getObjectID(), object.getLikeObjectType(), false);
				if(id==0)
					return;
				if(!(oaa.author() instanceof ForeignUser) && contentObject.getAuthorID()!=self.id){
					NotificationsStorage.deleteNotification(object.getObjectTypeForLikeNotifications(), contentObject.getObjectID(), Notification.Type.LIKE, self.id);
				}
				if(object instanceof Post post) // TODO
					context.getActivityPubWorker().sendUndoLikeActivity(post, self, id);
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
}
