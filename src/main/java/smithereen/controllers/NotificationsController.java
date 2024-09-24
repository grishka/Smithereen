package smithereen.controllers;

import java.sql.SQLException;

import smithereen.ApplicationContext;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.Account;
import smithereen.model.ForeignUser;
import smithereen.model.OwnedContentObject;
import smithereen.model.OwnerAndAuthor;
import smithereen.model.PaginatedList;
import smithereen.model.Post;
import smithereen.model.User;
import smithereen.model.comments.Comment;
import smithereen.model.notifications.Notification;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.model.photos.Photo;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.SessionStorage;
import smithereen.storage.UserStorage;

public class NotificationsController{
	private final ApplicationContext context;

	public NotificationsController(ApplicationContext context){
		this.context=context;
	}

	public PaginatedList<Notification> getNotifications(User user, int offset, int count){
		try{
			return NotificationsStorage.getNotifications(user.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setNotificationsSeen(Account self, int lastSeenID){
		try{
			if(lastSeenID>self.prefs.lastSeenNotificationID){
				self.prefs.lastSeenNotificationID=lastSeenID;
				SessionStorage.updatePreferences(self.id, self.prefs);
			}
			NotificationsStorage.getNotificationsForUser(self.user.id, self.prefs.lastSeenNotificationID).setNotificationsViewed();
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void createNotificationsForObject(Object obj){
		switch(obj){
			case Post post -> {
				Post parent;
				if(post.getReplyLevel()>0){
					try{
						parent=context.getWallController().getPostOrThrow(post.replyKey.getLast());
					}catch(ObjectNotFoundException x){
						return;
					}
				}else{
					parent=null;
				}
				OwnerAndAuthor oaa=context.getWallController().getContentAuthorAndOwner(post);
				// For a reply to a local post, notify the parent post author about the reply
				if(parent!=null && parent.isLocal() && parent.authorID!=post.authorID){
					User parentAuthor=context.getUsersController().getUserOrThrow(parent.authorID);
					createNotification(parentAuthor, Notification.Type.REPLY, post, parent, oaa.author());
				}

				// Notify every mentioned local user, except the parent post author, if any
				for(User user:context.getUsersController().getUsers(post.mentionedUserIDs).values()){
					if(user instanceof ForeignUser)
						continue;
					if(parent!=null && user.id==parent.authorID)
						continue;
					if(user.id==post.authorID)
						continue;
					createNotification(user, Notification.Type.MENTION, post, null, oaa.author());
				}

				// Finally, if it's a wall post on a local user's wall, notify them
				if(post.getReplyLevel()==0 && post.ownerID!=post.authorID && post.ownerID>0 && !(oaa.owner() instanceof ForeignUser)){
					createNotification((User)oaa.owner(), Notification.Type.POST_OWN_WALL, post, null, oaa.author());
				}

				// If this is a quote-repost of a local post, notify its author
				if(post.repostOf!=0){
					Post firstRepost=context.getWallController().getPostOrThrow(post.repostOf);
					if(firstRepost.isLocal() && post.authorID!=firstRepost.authorID){
						User repostedPostAuthor=context.getUsersController().getUserOrThrow(firstRepost.authorID);
						createNotification(repostedPostAuthor, Notification.Type.REPOST, firstRepost, post, oaa.author());
					}
				}
			}
			case Comment comment -> {

			}
			default -> throw new IllegalStateException("Unexpected value: " + obj);
		}
	}

	public void createNotification(User owner, Notification.Type type, OwnedContentObject object, OwnedContentObject relatedObject, User actor){
		if(owner instanceof ForeignUser)
			return;
		if(context.getPrivacyController().isUserBlocked(actor, owner))
			return;
		try{
			NotificationsStorage.putNotification(owner.id, type, getObjectTypeForObject(object), object==null ? 0 : object.getObjectID(),
					getObjectTypeForObject(relatedObject), relatedObject==null ? 0 : relatedObject.getObjectID(), actor.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private Notification.ObjectType getObjectTypeForObject(OwnedContentObject obj){
		return switch(obj){
			case null -> null;
			case Post post -> Notification.ObjectType.POST;
			case Photo photo -> Notification.ObjectType.PHOTO;
			default -> throw new IllegalStateException("Unexpected value: " + obj);
		};
	}
}
