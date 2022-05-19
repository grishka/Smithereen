package smithereen.controllers;

import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import smithereen.ApplicationContext;
import smithereen.data.FriendRequest;
import smithereen.data.FriendshipStatus;
import smithereen.data.PaginatedList;
import smithereen.data.User;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.storage.UserStorage;

public class FriendsController{
	private final ApplicationContext context;

	public FriendsController(ApplicationContext context){
		this.context=context;
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

	public PaginatedList<User> getFriends(User user, int offset, int count){
		try{
			return UserStorage.getFriendListForUser(user.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<User> getMutualFriends(User user, User otherUser, int offset, int count){
		try{
			if(user.id==otherUser.id)
				throw new IllegalArgumentException("must be different users");
			return UserStorage.getMutualFriendListForUser(user.id, otherUser.id, offset, count);
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
}
