package smithereen.controllers;

import java.sql.SQLException;

import smithereen.ApplicationContext;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.storage.BookmarksStorage;
import smithereen.storage.GroupStorage;
import smithereen.storage.SearchStorage;
import smithereen.storage.UserStorage;

public class BookmarksController{
	private final ApplicationContext context;

	public BookmarksController(ApplicationContext context){
		this.context=context;
	}

	public PaginatedList<Integer> getBookmarkedUsers(User self, int offset, int count){
		try{
			return BookmarksStorage.getUsers(self.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Integer> searchBookmarkedUsers(User self, String query, int offset, int count){
		try{
			return SearchStorage.searchBookmarkedUsers(query, self.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public boolean isUserBookmarked(User self, User user){
		try{
			return BookmarksStorage.isUserBookmarked(self.id, user.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void addUserBookmark(User self, User user){
		if(self.id==user.id)
			throw new BadRequestException("Can't bookmark self");
		try{
			BookmarksStorage.addUserBookmark(self.id, user.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void removeUserBookmark(User self, User user){
		try{
			BookmarksStorage.removeUserBookmark(self.id, user.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}



	public PaginatedList<Integer> getBookmarkedGroups(User self, int offset, int count){
		try{
			return BookmarksStorage.getGroups(self.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public PaginatedList<Integer> searchBookmarkedGroups(User self, String query, int offset, int count){
		try{
			return SearchStorage.searchBookmarkedGroups(query, self.id, offset, count);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public boolean isGroupBookmarked(User self, Group group){
		try{
			return BookmarksStorage.isGroupBookmarked(self.id, group.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void addGroupBookmark(User self, Group group){
		try{
			context.getPrivacyController().enforceUserAccessToGroupProfile(self, group);
			BookmarksStorage.addGroupBookmark(self.id, group.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}

	public void removeGroupBookmark(User self, Group group){
		try{
			BookmarksStorage.removeGroupBookmark(self.id, group.id);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
