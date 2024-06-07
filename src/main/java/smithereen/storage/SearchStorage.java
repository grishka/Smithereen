package smithereen.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.SearchResult;
import smithereen.model.User;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.text.TextProcessor;

public class SearchStorage{

	private static void addResults(ResultSet res, ArrayList<SearchResult> results, Set<Integer> users, Set<Integer> groups) throws SQLException{
		while(res.next()){
			SearchResult sr;
			int userID=res.getInt(1);
			if(res.wasNull()){
				int groupID=res.getInt(2);
				if(res.wasNull())
					continue;
				if(groups.contains(groupID))
					continue;
				sr=new SearchResult(SearchResult.Type.GROUP, groupID);
				groups.add(groupID);
			}else{
				if(users.contains(userID))
					continue;
				sr=new SearchResult(SearchResult.Type.USER, userID);
				users.add(userID);
			}
			results.add(sr);
		}
	}

	private static String prepareQuery(String query){
		return Arrays.stream(TextProcessor.transliterate(query).replaceAll("[()\\[\\]*+~<>\\\"@-]", " ").split("\\s+")).filter(Predicate.not(String::isBlank)).map(s->'+'+s+'*').collect(Collectors.joining(" "));
	}

	public static List<SearchResult> search(String query, int selfID, int maxCount) throws SQLException{
		query=prepareQuery(query);
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			ArrayList<SearchResult> results=new ArrayList<>();
			HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();

			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT user_id, group_id FROM qsearch_index WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND "+
					"((user_id IN (SELECT followee_id FROM followings WHERE follower_id=?)) OR (group_id IN (SELECT group_id FROM group_memberships WHERE user_id=?))) LIMIT ?", query, selfID, selfID, maxCount);
			try(ResultSet res=stmt.executeQuery()){
				addResults(res, results, needUsers, needGroups);
			}
			if(results.size()<maxCount){
				stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT user_id, group_id FROM qsearch_index WHERE MATCH(string) AGAINST (? IN BOOLEAN MODE) LIMIT ?", query, maxCount-results.size());
				try(ResultSet res=stmt.executeQuery()){
					addResults(res, results, needUsers, needGroups);
				}
			}

			Map<Integer, User> users=UserStorage.getById(needUsers);
			Map<Integer, Group> groups=GroupStorage.getById(needGroups);
			for(SearchResult sr: results){
				switch(sr.type){
					case USER -> sr.user=users.get(sr.id);
					case GROUP -> sr.group=groups.get(sr.id);
				}
			}

			return results;
		}
	}

	public static List<Integer> searchUsers(String query, int selfID, int count) throws SQLException{
		query=prepareQuery(query);
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			ArrayList<Integer> results=new ArrayList<>();
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT user_id FROM qsearch_index WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND "+
					"((user_id IN (SELECT followee_id FROM followings WHERE follower_id=?))) LIMIT ?", query, selfID, count);
			DatabaseUtils.intResultSetToStream(stmt.executeQuery(), null).forEach(results::add);
			if(results.size()<count){
				stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT user_id FROM qsearch_index WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND user_id IS NOT NULL LIMIT ?", query, count);
				DatabaseUtils.intResultSetToStream(stmt.executeQuery(), null).forEach(id->{
					if(!results.contains(id) && results.size()<count)
						results.add(id);
				});
			}
			return results;
		}
	}

	public static PaginatedList<Integer> searchFriends(String query, int selfID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			query=prepareQuery(query);
			int total=DatabaseUtils.oneFieldToInt(SQLQueryBuilder.prepareStatement(conn,
					"SELECT COUNT(*) FROM qsearch_index WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND user_id IN (SELECT followee_id FROM followings WHERE follower_id=? AND mutual=1 AND accepted=1)",
					query, selfID).executeQuery());
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Integer> list=DatabaseUtils.intResultSetToList(SQLQueryBuilder.prepareStatement(conn,
					"SELECT user_id FROM qsearch_index WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND user_id IN (SELECT followee_id FROM followings WHERE follower_id=? AND mutual=1 AND accepted=1) LIMIT ? OFFSET ?",
					query, selfID, count, offset).executeQuery());
			return new PaginatedList<>(list, total, offset, count);
		}
	}

	public static PaginatedList<Integer> searchGroups(String query, boolean events, int selfID, int offset, int count, boolean includePrivate) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			query=prepareQuery(query);
			String privateWhere=includePrivate ? "" : " AND groups.access_type<>2";
			int total=DatabaseUtils.oneFieldToInt(SQLQueryBuilder.prepareStatement(conn,
					"SELECT COUNT(*) FROM qsearch_index JOIN `groups` ON group_id=`groups`.id WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND `groups`.`type`=?"+privateWhere+" AND group_id IN (SELECT group_id FROM group_memberships WHERE user_id=? AND accepted=1)",
					query, events ? Group.Type.EVENT : Group.Type.GROUP, selfID).executeQuery());
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Integer> list=DatabaseUtils.intResultSetToList(SQLQueryBuilder.prepareStatement(conn,
					"SELECT group_id FROM qsearch_index JOIN `groups` ON group_id=`groups`.id WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND `groups`.`type`=?"+privateWhere+" AND group_id IN (SELECT group_id FROM group_memberships WHERE user_id=? AND accepted=1) LIMIT ? OFFSET ?",
					query, events ? Group.Type.EVENT : Group.Type.GROUP, selfID, count, offset).executeQuery());
			return new PaginatedList<>(list, total, offset, count);
		}
	}

	public static PaginatedList<Integer> searchBookmarkedUsers(String query, int selfID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			query=prepareQuery(query);
			int total=DatabaseUtils.oneFieldToInt(SQLQueryBuilder.prepareStatement(conn,
					"SELECT COUNT(*) FROM qsearch_index WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND user_id IN (SELECT user_id FROM bookmarks_user WHERE owner_id=?)",
					query, selfID).executeQuery());
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Integer> list=DatabaseUtils.intResultSetToList(SQLQueryBuilder.prepareStatement(conn,
					"SELECT user_id FROM qsearch_index WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND user_id IN (SELECT user_id FROM bookmarks_user WHERE owner_id=?) LIMIT ? OFFSET ?",
					query, selfID, count, offset).executeQuery());
			return new PaginatedList<>(list, total, offset, count);
		}
	}

	public static PaginatedList<Integer> searchBookmarkedGroups(String query, int selfID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			query=prepareQuery(query);
			int total=DatabaseUtils.oneFieldToInt(SQLQueryBuilder.prepareStatement(conn,
					"SELECT COUNT(*) FROM qsearch_index WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND group_id IN (SELECT group_id FROM bookmarks_group WHERE owner_id=?)",
					query, selfID).executeQuery());
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Integer> list=DatabaseUtils.intResultSetToList(SQLQueryBuilder.prepareStatement(conn,
					"SELECT group_id FROM qsearch_index WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND group_id IN (SELECT group_id FROM bookmarks_group WHERE owner_id=?) LIMIT ? OFFSET ?",
					query, selfID, count, offset).executeQuery());
			return new PaginatedList<>(list, total, offset, count);
		}
	}
}
