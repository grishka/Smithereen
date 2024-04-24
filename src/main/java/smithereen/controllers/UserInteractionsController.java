package smithereen.controllers;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
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

	public void setObjectLiked(Post object, boolean liked, User self){
		try{
			if(object.isMastodonStyleRepost()){
				object=context.getWallController().getPostOrThrow(object.repostOf);
			}
			context.getPrivacyController().enforceObjectPrivacy(self, object);
			OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(object);
			if(oaa.owner() instanceof User u)
				Utils.ensureUserNotBlocked(self, u);
			else if(oaa.owner() instanceof Group g)
				Utils.ensureUserNotBlocked(self, g);

			if(liked){
				int id=LikeStorage.setPostLiked(self.id, object.id, true);
				if(id==0) // Already liked
					return;
				if(!(oaa.author() instanceof ForeignUser) && object.authorID!=self.id){
					Notification n=new Notification();
					n.type=Notification.Type.LIKE;
					n.actorID=self.id;
					n.objectID=object.id;
					n.objectType=Notification.ObjectType.POST;
					NotificationsStorage.putNotification(object.authorID, n);
				}
				context.getActivityPubWorker().sendLikeActivity(object, self, id);
			}else{
				context.getPrivacyController().enforceObjectPrivacy(self, object);
				int id=LikeStorage.setPostLiked(self.id, object.id, false);
				if(id==0)
					return;
				if(!(oaa.author() instanceof ForeignUser) && object.authorID!=self.id){
					NotificationsStorage.deleteNotification(Notification.ObjectType.POST, object.id, Notification.Type.LIKE, self.id);
				}
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
}
