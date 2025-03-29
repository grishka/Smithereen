package smithereen.controllers;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.model.ForeignUser;
import smithereen.model.FriendRequest;
import smithereen.model.FriendshipStatus;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.notifications.Notification;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.UserErrorException;
import smithereen.model.notifications.RealtimeNotification;
import smithereen.storage.UserStorage;
import smithereen.storage.utils.IntPair;
import smithereen.util.MaintenanceScheduler;

public class FriendsController{
	private static final Logger LOG=LoggerFactory.getLogger(FriendsController.class);

	private final ApplicationContext ctx;
	private ArrayList<PendingHintsRankIncrement> pendingHintsRankIncrements=new ArrayList<>();

	public FriendsController(ApplicationContext ctx){
		this.ctx=ctx;
		MaintenanceScheduler.runPeriodically(this::doPendingHintsUpdates, 10, TimeUnit.MINUTES);
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
			return UserStorage.getNonMutualFollowers(user.id, true, true, offset, count, false);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> getFollows(User user, int offset, int count){
		try{
			return UserStorage.getNonMutualFollowers(user.id, false, true, offset, count, true);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> getFriends(User user, int offset, int count, SortOrder order){
		return getFriends(user, offset, count, order, false);
	}

	public PaginatedList<User> getOnlineFriends(User user, int offset, int count, SortOrder order){
		return getFriends(user, offset, count, order, true);
	}

	public PaginatedList<User> getMutualFriends(User user, User otherUser, int offset, int count, SortOrder order){
		try{
			if(user.id==otherUser.id)
				throw new IllegalArgumentException("must be different users");
			return UserStorage.getMutualFriendListForUser(user.id, otherUser.id, offset, count, order==SortOrder.HINTS);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	private PaginatedList<User> getFriends(User user, int offset, int count, SortOrder order, boolean onlineOnly){
		try{
			return switch(order){
				case ID_ASCENDING -> UserStorage.getFriendListForUser(user.id, offset, count, onlineOnly, false);
				case RANDOM -> UserStorage.getRandomFriendsForProfile(user.id, count, onlineOnly);
				case HINTS -> UserStorage.getFriendListForUser(user.id, offset, count, onlineOnly, true);
			};
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

	/**
	 * Same as getFriendshipStatus but doesn't check friend requests
	 * @param self
	 * @param other
	 * @return
	 */
	public FriendshipStatus getSimpleFriendshipStatus(User self, User other){
		try{
			return UserStorage.getSimpleFriendshipStatus(self.id, other.id);
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

	public void storeFriendship(@NotNull User user1, @NotNull User user2, boolean updateNumbers){
		try{
			if(UserStorage.getFriendshipStatus(user1.id, user2.id)==FriendshipStatus.FRIENDS)
				return;
			UserStorage.followUser(user1.id, user2.id, true, true, updateNumbers);
			UserStorage.followUser(user2.id, user1.id, true, true, updateNumbers);
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
						}else{
							ctx.getNotificationsController().sendRealtimeNotifications(user, "friendReq"+self.id+"_"+Utils.randomAlphanumericString(5), RealtimeNotification.Type.FRIEND_REQUEST, null, null, self);
						}
					}else{
						UserStorage.followUser(self.id, user.id, !(user instanceof ForeignUser), false, true);
						if(user instanceof ForeignUser fu){
							ctx.getActivityPubWorker().sendFollowUserActivity(self, fu);
						}else{
							ctx.getNotificationsController().createNotification(user, Notification.Type.FOLLOW, null, null, self);
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
					UserStorage.followUser(self.id, user.id, !(user instanceof ForeignUser), false, true);
					if(user instanceof ForeignUser fu){
						ctx.getActivityPubWorker().sendFollowUserActivity(self, fu);
					}else{
						ctx.getNotificationsController().createNotification(user, Notification.Type.FOLLOW, null, null, self);
						ctx.getNewsfeedController().putFriendsFeedEntry(self, user.id, NewsfeedEntry.Type.ADD_FRIEND);
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
				ctx.getNotificationsController().createNotification(user, Notification.Type.FRIEND_REQ_ACCEPT, null, null, self);
				ctx.getActivityPubWorker().sendAddToFriendsCollectionActivity(self, user);
				ctx.getNewsfeedController().putFriendsFeedEntry(user, self.id, NewsfeedEntry.Type.ADD_FRIEND);
			}
			ctx.getNewsfeedController().putFriendsFeedEntry(self, user.id, NewsfeedEntry.Type.ADD_FRIEND);
			ctx.getNotificationsController().sendRealtimeCountersUpdates(self);
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
			ctx.getNotificationsController().sendRealtimeCountersUpdates(self);
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

	public boolean isUserMuted(User self, User user){
		try{
			return UserStorage.isUserMuted(self.id, user.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setUserMuted(User self, User user, boolean muted){
		try{
			UserStorage.setUserMuted(self.id, user.id, muted);
			ctx.getNewsfeedController().clearFriendsFeedCache(self.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void incrementHintsRank(User self, User friend, int amount){
		if(self.id==friend.id)
			return;
		pendingHintsRankIncrements.add(new PendingHintsRankIncrement(self.id, friend.id, amount));
	}

	public void doPendingHintsUpdates(){
		if(pendingHintsRankIncrements.isEmpty())
			return;
		ArrayList<PendingHintsRankIncrement> increments=pendingHintsRankIncrements;
		pendingHintsRankIncrements=new ArrayList<>();
		try{
			HashMap<IntPair, Integer> totals=new HashMap<>();
			for(PendingHintsRankIncrement i:increments){
				IntPair key=new IntPair(i.followerID, i.followeeID);
				totals.put(key, totals.getOrDefault(key, 0)+i.amount);
			}
			HashSet<Integer> usersToNormalize=new HashSet<>();
			for(Map.Entry<IntPair, Integer> i:totals.entrySet()){
				IntPair key=i.getKey();
				if(UserStorage.incrementFriendHintsRank(key.first(), key.second(), i.getValue()))
					usersToNormalize.add(key.first());
			}
			if(!usersToNormalize.isEmpty())
				UserStorage.normalizeFriendHintsRanksIfNeeded(usersToNormalize);
		}catch(SQLException x){
			LOG.error("Failed to update hint ranks", x);
		}
	}

	public enum SortOrder{
		ID_ASCENDING,
		RANDOM,
		HINTS
	}

	private record PendingHintsRankIncrement(int followerID, int followeeID, int amount){}
}
