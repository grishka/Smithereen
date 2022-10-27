package smithereen.controllers;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import javax.swing.SortOrder;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.data.ForeignUser;
import smithereen.data.FriendRequest;
import smithereen.data.FriendshipStatus;
import smithereen.data.PaginatedList;
import smithereen.data.User;
import smithereen.data.WebDeltaResponse;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.data.notifications.Notification;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.UserErrorException;
import smithereen.storage.NotificationsStorage;
import smithereen.storage.UserStorage;

public class FriendsController{
	private final ApplicationContext ctx;

	public FriendsController(ApplicationContext ctx){
		this.ctx=ctx;
	}

	public PaginatedList<FriendRequest> getIncomingFriendRequests(User self, int offset, int count){
		try{
			return UserStorage.getIncomingFriendRequestsForUser(self.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> getFollowers(User user, int offset, int count){
		try{
			return UserStorage.getNonMutualFollowers(user.id, true, true, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> getFollows(User user, int offset, int count){
		try{
			return UserStorage.getNonMutualFollowers(user.id, false, true, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> getFriends(User user, int offset, int count, SortOrder order){
		try{
			return switch(order){
				case ID_ASCENDING -> UserStorage.getFriendListForUser(user.id, offset, count);
				case RANDOM -> UserStorage.getRandomFriendsForProfile(user.id, count);
			};
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> getMutualFriends(User user, User otherUser, int offset, int count, SortOrder order){
		try{
			if(user.id==otherUser.id)
				throw new IllegalArgumentException("must be different users");
			return UserStorage.getMutualFriendListForUser(user.id, otherUser.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public int getMutualFriendsCount(User user, User otherUser){
		try{
			if(user.id==otherUser.id)
				throw new IllegalArgumentException("must be different users");
			return UserStorage.getMutualFriendsCount(user.id, otherUser.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public FriendshipStatus getFriendshipStatus(User self, User other){
		try{
			return UserStorage.getFriendshipStatus(self.id, other.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Map<URI, Integer> getFriendsByActivityPubIDs(@NotNull User user, Collection<URI> query){
		try{
			return UserStorage.getFriendsByActivityPubIDs(query, user.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void storeFriendship(@NotNull User user1, @NotNull User user2){
		try{
			if(UserStorage.getFriendshipStatus(user1.id, user2.id)==FriendshipStatus.FRIENDS)
				return;
			UserStorage.followUser(user1.id, user2.id, true, true);
			UserStorage.followUser(user2.id, user1.id, true, true);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void sendFriendRequest(User self, User user, String message){
		try{
			if(user.id==self.id){
				throw new UserErrorException("err_cant_friend_self");
			}
			Utils.ensureUserNotBlocked(self, user);
			FriendshipStatus status=getFriendshipStatus(self, user);
			switch(status){
				case NONE, FOLLOWED_BY -> {
					if(status==FriendshipStatus.NONE && user.supportsFriendRequests()){
						UserStorage.putFriendRequest(self.id, user.id, message, !(user instanceof ForeignUser));
						if(user instanceof ForeignUser fu){
							ctx.getActivityPubWorker().sendFriendRequestActivity(self, fu, message);
						}
					}else{
						UserStorage.followUser(self.id, user.id, !(user instanceof ForeignUser), false);
						if(user instanceof ForeignUser fu){
							ctx.getActivityPubWorker().sendFollowUserActivity(self, fu);
						}else{
							ctx.getActivityPubWorker().sendAddToFriendsCollectionActivity(self, user);
						}
					}
				}
				case FRIENDS -> throw new UserErrorException("err_already_friends");
				case REQUEST_RECVD -> throw new UserErrorException("err_have_incoming_friend_req");
				case FOLLOWING, REQUEST_SENT, FOLLOW_REQUESTED -> throw new UserErrorException("err_friend_req_already_sent");
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void followUser(User self, User user){
		try{
			if(user.id==self.id){
				throw new UserErrorException("err_cant_friend_self");
			}
			Utils.ensureUserNotBlocked(self, user);
			FriendshipStatus status=getFriendshipStatus(self, user);
			switch(status){
				case NONE, FOLLOWED_BY, REQUEST_RECVD -> {
					UserStorage.followUser(self.id, user.id, !(user instanceof ForeignUser), false);
					if(user instanceof ForeignUser fu){
						ctx.getActivityPubWorker().sendFollowUserActivity(self, fu);
					}else{
						ctx.getActivityPubWorker().sendAddToFriendsCollectionActivity(self, user);
					}
				}
				case FRIENDS -> throw new UserErrorException("err_already_friends");
				case FOLLOWING, REQUEST_SENT, FOLLOW_REQUESTED -> throw new UserErrorException("err_friend_req_already_sent");
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void acceptFriendRequest(User self, User user){
		try{
			if(user instanceof ForeignUser fu){
				if(!UserStorage.acceptFriendRequest(self.id, user.id, false))
					return;
				ctx.getActivityPubWorker().sendFollowUserActivity(self, fu);
			}else{
				if(!UserStorage.acceptFriendRequest(self.id, user.id, true))
					return;
				Notification n=new Notification();
				n.type=Notification.Type.FRIEND_REQ_ACCEPT;
				n.actorID=self.id;
				NotificationsStorage.putNotification(user.id, n);
				ctx.getActivityPubWorker().sendAddToFriendsCollectionActivity(self, user);
				ctx.getNewsfeedController().putFriendsFeedEntry(user, self.id, NewsfeedEntry.Type.ADD_FRIEND);
			}
			ctx.getNewsfeedController().putFriendsFeedEntry(self, user.id, NewsfeedEntry.Type.ADD_FRIEND);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void rejectFriendRequest(User self, User user){
		try{
			UserStorage.deleteFriendRequest(self.id, user.id);
			if(user instanceof ForeignUser fu){
				ctx.getActivityPubWorker().sendRejectFriendRequestActivity(self, fu);
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void blockUser(User self, User user){
		try{
			FriendshipStatus status=UserStorage.getFriendshipStatus(self.id, user.id);
			UserStorage.blockUser(self.id, user.id);
			if(user instanceof ForeignUser fu)
				ctx.getActivityPubWorker().sendBlockActivity(self, fu);
			if(status==FriendshipStatus.FRIENDS){
				ctx.getActivityPubWorker().sendRemoveFromFriendsCollectionActivity(self, user);
				ctx.getNewsfeedController().deleteFriendsFeedEntry(self, user.id, NewsfeedEntry.Type.ADD_FRIEND);
				if(!(user instanceof ForeignUser)){
					ctx.getNewsfeedController().deleteFriendsFeedEntry(user, self.id, NewsfeedEntry.Type.ADD_FRIEND);
				}
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void unblockUser(User self, User user){
		try{
			UserStorage.unblockUser(self.id, user.id);
			if(user instanceof ForeignUser fu)
				ctx.getActivityPubWorker().sendUndoBlockActivity(self, fu);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void removeFriend(User self, User user){
		try{
			FriendshipStatus status=UserStorage.getFriendshipStatus(self.id, user.id);
			if(status!=FriendshipStatus.FRIENDS && status!=FriendshipStatus.REQUEST_SENT && status!=FriendshipStatus.FOLLOWING && status!=FriendshipStatus.FOLLOW_REQUESTED){
				throw new UserErrorException("err_not_friends");
			}
			UserStorage.unfriendUser(self.id, user.id);
			if(user instanceof ForeignUser){
				ctx.getActivityPubWorker().sendUnfriendActivity(self, user);
			}
			if(status==FriendshipStatus.FRIENDS){
				ctx.getActivityPubWorker().sendRemoveFromFriendsCollectionActivity(self, user);
				ctx.getNewsfeedController().deleteFriendsFeedEntry(self, user.id, NewsfeedEntry.Type.ADD_FRIEND);
				if(!(user instanceof ForeignUser)){
					ctx.getNewsfeedController().deleteFriendsFeedEntry(user, self.id, NewsfeedEntry.Type.ADD_FRIEND);
				}
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public enum SortOrder{
		ID_ASCENDING,
		RANDOM
		// TODO hints
	}
}
