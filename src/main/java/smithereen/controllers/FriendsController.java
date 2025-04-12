package smithereen.controllers;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.swing.SortOrder;

import smithereen.ApplicationContext;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserActionNotAllowedException;
import smithereen.model.ForeignUser;
import smithereen.model.FriendRequest;
import smithereen.model.FriendshipStatus;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.friends.FriendList;
import smithereen.model.friends.PublicFriendList;
import smithereen.model.notifications.Notification;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.exceptions.UserErrorException;
import smithereen.model.notifications.RealtimeNotification;
import smithereen.storage.UserStorage;
import smithereen.storage.utils.IntPair;
import smithereen.util.MaintenanceScheduler;
import smithereen.util.NamedMutexCollection;

public class FriendsController{
	private static final Logger LOG=LoggerFactory.getLogger(FriendsController.class);

	private final ApplicationContext ctx;
	private ArrayList<PendingHintsRankIncrement> pendingHintsRankIncrements=new ArrayList<>();
	private final NamedMutexCollection friendListsUpdateMutex=new NamedMutexCollection();
	private final LruCache<Integer, List<FriendList>> friendListsCache=new LruCache<>(1000);

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
		return getFriends(user, offset, count, order, false, 0);
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

	public PaginatedList<User> getFriends(User user, int offset, int count, SortOrder order, boolean onlineOnly, int listID){
		try{
			return switch(order){
				case ID_ASCENDING, HINTS, RECENTLY_ADDED -> UserStorage.getFriendListForUser(user.id, offset, count, onlineOnly, order, listID);
				case RANDOM -> UserStorage.getRandomFriendsForProfile(user.id, count, onlineOnly);
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
			Set<Integer> user1Lists=Set.of(), user2Lists=Set.of();
			if(status==FriendshipStatus.FRIENDS){
				if(!(self instanceof ForeignUser)){
					user1Lists=getFriendListIDsForUser(self, user);
				}
				if(!(user instanceof ForeignUser)){
					user2Lists=getFriendListIDsForUser(user, self);
				}
			}
			UserStorage.blockUser(self.id, user.id);
			if(user instanceof ForeignUser fu)
				ctx.getActivityPubWorker().sendBlockActivity(self, fu);
			if(status==FriendshipStatus.FRIENDS){
				ctx.getActivityPubWorker().sendRemoveFromFriendsCollectionActivity(self, user);
				ctx.getNewsfeedController().deleteFriendsFeedEntry(self, user.id, NewsfeedEntry.Type.ADD_FRIEND);
				if(!(user instanceof ForeignUser)){
					ctx.getNewsfeedController().deleteFriendsFeedEntry(user, self.id, NewsfeedEntry.Type.ADD_FRIEND);
				}
				if(!user1Lists.isEmpty())
					ctx.getPrivacyController().updatePrivacySettingsAffectedByFriendListChanges(self, user1Lists);
				if(!user2Lists.isEmpty())
					ctx.getPrivacyController().updatePrivacySettingsAffectedByFriendListChanges(user, user2Lists);
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
			Set<Integer> user1Lists=Set.of(), user2Lists=Set.of();
			if(status==FriendshipStatus.FRIENDS){
				if(!(self instanceof ForeignUser)){
					user1Lists=getFriendListIDsForUser(self, user);
				}
				if(!(user instanceof ForeignUser)){
					user2Lists=getFriendListIDsForUser(user, self);
				}
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
				if(!user1Lists.isEmpty())
					ctx.getPrivacyController().updatePrivacySettingsAffectedByFriendListChanges(self, user1Lists);
				if(!user2Lists.isEmpty())
					ctx.getPrivacyController().updatePrivacySettingsAffectedByFriendListChanges(user, user2Lists);
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

	public List<FriendList> getFriendLists(User owner){
		List<FriendList> lists=friendListsCache.get(owner.id);
		if(lists!=null)
			return lists;
		try{
			lists=UserStorage.getFriendLists(owner.id);
			friendListsCache.put(owner.id, lists);
			return lists;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public int createFriendList(User owner, String name, Collection<Integer> memberIDs){
		String mutexName=String.valueOf(owner.id);
		friendListsUpdateMutex.acquire(mutexName);
		try{
			int id=UserStorage.createFriendList(owner.id, name, memberIDs);
			friendListsCache.remove(owner.id);
			return id;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}finally{
			friendListsUpdateMutex.release(mutexName);
		}
	}

	public Map<Integer, BitSet> getFriendListsForUsers(User self, User owner, Collection<Integer> friendIDs){
		if(friendIDs.isEmpty())
			return Map.of();
		try{
			Map<Integer, BitSet> lists=UserStorage.getFriendListsForUsers(owner.id, friendIDs);
			if(self==null || self.id!=owner.id){
				BitSet publicMask=BitSet.valueOf(new long[]{0xff00000000000000L});
				for(BitSet userLists:lists.values())
					userLists.and(publicMask);
			}
			return lists;
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public Set<Integer> getFriendListIDsForUser(User self, User friend){
		try{
			return UserStorage.getFriendListsForUsers(self.id, Set.of(friend.id)).getOrDefault(friend.id, new BitSet())
					.stream()
					.map(i->i+1)
					.boxed()
					.collect(Collectors.toSet());
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void setUserFriendLists(User self, User friend, BitSet lists){
		try{
			Set<Integer> prevLists=getFriendListIDsForUser(self, friend);
			if(!lists.isEmpty()){
				Set<Integer> validListIDs=getFriendLists(self).stream().map(FriendList::id).collect(Collectors.toSet());
				lists.stream().forEach(id->{
					id+=1;
					if(!validListIDs.contains(id) && (id<FriendList.FIRST_PUBLIC_LIST_ID || id>=FriendList.FIRST_PUBLIC_LIST_ID+PublicFriendList.values().length)){
						lists.clear(id-1);
					}
				});
			}
			UserStorage.setFriendListsForUser(self.id, friend.id, lists);
			Set<Integer> newLists=lists.stream()
					.map(i->i+1)
					.boxed()
					.collect(Collectors.toSet());

			HashSet<Integer> allAffectedLists=new HashSet<>();
			for(int id:prevLists){
				if(!newLists.contains(id))
					allAffectedLists.add(id);
			}
			for(int id:newLists){
				if(!prevLists.contains(id))
					allAffectedLists.add(id);
			}
			ctx.getPrivacyController().updatePrivacySettingsAffectedByFriendListChanges(self, allAffectedLists);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void deleteFriendList(User owner, int id){
		if(id<1 || id>=FriendList.FIRST_PUBLIC_LIST_ID)
			throw new UserActionNotAllowedException();
		String mutexName=String.valueOf(owner.id);
		friendListsUpdateMutex.acquire(mutexName);
		try{
			UserStorage.deleteFriendList(owner.id, id);
			friendListsCache.remove(owner.id);
			ctx.getPrivacyController().updatePrivacySettingsAffectedByFriendListChanges(owner, Set.of(id));
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}finally{
			friendListsUpdateMutex.release(mutexName);
		}
	}

	public Set<Integer> getFriendListMemberIDs(User owner, int id){
		if(id<1 || id>64)
			throw new ObjectNotFoundException();
		try{
			return UserStorage.getFriendListMemberIDs(owner.id, id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void updateFriendList(User owner, int id, String name, Set<Integer> memberIDs){
		FriendList existing=null;
		if(id<FriendList.FIRST_PUBLIC_LIST_ID){
			for(FriendList fl:getFriendLists(owner)){
				if(fl.id()==id){
					existing=fl;
					break;
				}
			}
			if(existing==null)
				throw new ObjectNotFoundException();
		}

		boolean anythingChanged=false;
		try{
			if(existing!=null && !Objects.equals(existing.name(), name)){
				anythingChanged=true;
				UserStorage.renameFriendList(owner.id, id, name);
			}

			Set<Integer> existingMemberIDs=getFriendListMemberIDs(owner, id);
			if(!existingMemberIDs.equals(memberIDs)){
				anythingChanged=true;
				Set<Integer> toAdd=memberIDs.stream().filter(mid->!existingMemberIDs.contains(mid)).collect(Collectors.toSet());
				Set<Integer> toRemove=existingMemberIDs.stream().filter(mid->!memberIDs.contains(mid)).collect(Collectors.toSet());

				if(!toAdd.isEmpty())
					UserStorage.addToFriendList(owner.id, id, toAdd);
				if(!toRemove.isEmpty())
					UserStorage.removeFromFriendList(owner.id, id, toRemove);

				ctx.getPrivacyController().updatePrivacySettingsAffectedByFriendListChanges(owner, Set.of(id));
			}
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}

		if(anythingChanged){
			friendListsCache.remove(owner.id);
		}
	}

	public enum SortOrder{
		ID_ASCENDING,
		RANDOM,
		HINTS,
		RECENTLY_ADDED
	}

	private record PendingHintsRankIncrement(int followerID, int followeeID, int amount){}
}
