package smithereen.storage;

import java.sql.SQLException;
import java.util.List;

import smithereen.model.PaginatedList;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;

public class BookmarksStorage{
	public static PaginatedList<Integer> getUsers(int ownerID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("bookmarks_user")
					.count()
					.where("owner_id=?", ownerID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Integer> ids=new SQLQueryBuilder(conn)
					.selectFrom("bookmarks_user")
					.columns("user_id")
					.where("owner_id=?", ownerID)
					.orderBy("id DESC")
					.limit(count, offset)
					.executeAndGetIntList();
			return new PaginatedList<>(ids, total, offset, count);
		}
	}

	public static boolean isUserBookmarked(int ownerID, int userID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("bookmarks_user")
				.count()
				.where("owner_id=? AND user_id=?", ownerID, userID)
				.executeAndGetInt()>0;
	}

	public static void addUserBookmark(int ownerID, int userID) throws SQLException{
		new SQLQueryBuilder()
				.insertIgnoreInto("bookmarks_user")
				.value("owner_id", ownerID)
				.value("user_id", userID)
				.executeNoResult();
	}

	public static void removeUserBookmark(int ownerID, int userID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("bookmarks_user")
				.where("owner_id=? AND user_id=?", ownerID, userID)
				.executeNoResult();
	}



	public static PaginatedList<Integer> getGroups(int ownerID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("bookmarks_group")
					.count()
					.where("owner_id=?", ownerID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Integer> ids=new SQLQueryBuilder(conn)
					.selectFrom("bookmarks_group")
					.columns("group_id")
					.where("owner_id=?", ownerID)
					.orderBy("id DESC")
					.limit(count, offset)
					.executeAndGetIntList();
			return new PaginatedList<>(ids, total, offset, count);
		}
	}

	public static boolean isGroupBookmarked(int ownerID, int groupID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("bookmarks_group")
				.count()
				.where("owner_id=? AND group_id=?", ownerID, groupID)
				.executeAndGetInt()>0;
	}

	public static void addGroupBookmark(int ownerID, int groupID) throws SQLException{
		new SQLQueryBuilder()
				.insertIgnoreInto("bookmarks_group")
				.value("owner_id", ownerID)
				.value("group_id", groupID)
				.executeNoResult();
	}

	public static void removeGroupBookmark(int ownerID, int groupID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("bookmarks_group")
				.where("owner_id=? AND group_id=?", ownerID, groupID)
				.executeNoResult();
	}
}
