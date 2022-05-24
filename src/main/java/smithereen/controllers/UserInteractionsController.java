package smithereen.controllers;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import smithereen.ApplicationContext;
import smithereen.data.PaginatedList;
import smithereen.data.Post;
import smithereen.data.User;
import smithereen.data.UserInteractions;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.storage.LikeStorage;
import smithereen.storage.PostStorage;
import smithereen.storage.UserStorage;

public class UserInteractionsController{
	private final ApplicationContext context;

	public UserInteractionsController(ApplicationContext context){
		this.context=context;
	}

	public PaginatedList<User> getLikesForObject(Post object, User self, int offset, int count){
		try{
			UserInteractions interactions=PostStorage.getPostInteractions(Collections.singletonList(object.id), 0).get(object.id);
			List<User> users=UserStorage.getByIdAsList(LikeStorage.getPostLikes(object.id, 0, offset, count));
			return new PaginatedList<>(users, interactions.likeCount, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
