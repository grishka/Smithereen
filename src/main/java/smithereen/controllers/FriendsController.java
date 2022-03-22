package smithereen.controllers;

import java.sql.SQLException;

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
			throw new IllegalStateException(x);
		}
	}
}
