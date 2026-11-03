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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
	private static final Pattern TEXT_SEARCH_WORD_PATTERN=Pattern.compile("\\w[\\w-]*", Pattern.UNICODE_CHARACTER_CLASS);

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

	public static String prepareNameQuery(String query){
		return Arrays.stream(TextProcessor.transliterate(query).replaceAll("[()\\[\\]*+~<>\\\"@-]", " ").split("\\s+")).filter(Predicate.not(String::isBlank)).map(s->"+(>"+s+" <("+s+"*))").collect(Collectors.joining(" "));
	}

	public static String prepareTextQuery(String query){
		query=query.strip();
		if(query.isEmpty())
			return "";
		if(query.length()>3 && query.startsWith("\"") && query.endsWith("\"")){
			// User wants exact matches only. Make sure there are no "s in the middle of the string.
			query=query.replace("\"", "");
			if(query.isEmpty()) // Something like """"" is not valid
				return "";
			return '"'+query+'"';
		}
		// Put a + before every word in the search query to make sure all words are included in every search result, and remove non-word characters.
		Matcher matcher=TEXT_SEARCH_WORD_PATTERN.matcher(query);
		ArrayList<String> words=new ArrayList<>();
		String prevShortWord=null;
		while(matcher.find()){
			String word=matcher.group();
			if(word.contains("-")){
				words.add('"'+word+'"');
			}else if(word.length()<3){
				if(!words.isEmpty()){
					String prev=words.getLast();
					if(prev.startsWith("\""))
						prev=prev.substring(1, prev.length()-1);
					words.set(words.size()-1, '"'+prev+' '+word+'"');
				}else{
					prevShortWord=word;
				}
			}else{
				if(prevShortWord!=null){
					word=prevShortWord+' '+word;
					prevShortWord=null;
				}
				words.add(word);
			}
		}
		if(words.isEmpty()) // Query too short
			return "";
		return '+'+String.join(" +", words);
	}

	private static PaginatedList<Integer> searchUsersWithShortQuery(String query, int selfID, int count) throws SQLException{
		query=query.replace("%", "").strip();
		if(query.isEmpty())
			return PaginatedList.emptyList(count);
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("users")
					.count()
					.where("username LIKE ?", query+"%")
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			ArrayList<Integer> results=new ArrayList<>();
			new SQLQueryBuilder(conn)
					.selectFrom("followings")
					.columns("followee_id")
					.join("LEFT JOIN users ON followings.followee_id=users.id")
					.where("username LIKE ? AND follower_id=?", query+"%", selfID)
					.orderBy("hints_rank DESC")
					.limit(count, 0)
					.executeAndGetIntStream()
					.boxed()
					.forEach(results::add);

			if(results.size()<count){
				new SQLQueryBuilder(conn)
						.selectFrom("users")
						.columns("id")
						.whereNotIn("id", results)
						.andWhere("username LIKE ?", query+"%")
						.orderBy("LENGTH(username) ASC, username ASC")
						.limit(count-results.size(), 0)
						.executeAndGetIntStream()
						.boxed()
						.forEach(results::add);
			}

			return new PaginatedList<>(results, total, 0, count);
		}
	}

	public static List<SearchResult> search(String query, int selfID, int maxCount) throws SQLException{
		HashSet<Integer> needUsers=new HashSet<>(), needGroups=new HashSet<>();
		ArrayList<SearchResult> results=new ArrayList<>();

		query=query.strip();
		if(query.isEmpty())
			return List.of();
		if(query.length()<3){
			for(int uid:searchUsersWithShortQuery(query, selfID, maxCount).list){
				results.add(new SearchResult(SearchResult.Type.USER, uid));
				needUsers.add(uid);
			}
		}else{
			query=prepareNameQuery(query);
			try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){

				PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT qsearch_index.user_id, qsearch_index.group_id FROM qsearch_index "+
						"LEFT JOIN followings ON followings.followee_id=qsearch_index.user_id "+
						"LEFT JOIN group_memberships ON group_memberships.group_id=qsearch_index.group_id "+
						"WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND (followings.follower_id=? OR group_memberships.user_id=?) "+
						"ORDER BY IFNULL(followings.hints_rank, group_memberships.hints_rank) DESC LIMIT ?", query, selfID, selfID, maxCount);
				try(ResultSet res=stmt.executeQuery()){
					addResults(res, results, needUsers, needGroups);
				}
				if(results.size()<maxCount){
					stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT user_id, group_id FROM qsearch_index WHERE MATCH(string) AGAINST (? IN BOOLEAN MODE) LIMIT ?", query, maxCount-results.size());
					try(ResultSet res=stmt.executeQuery()){
						addResults(res, results, needUsers, needGroups);
					}
				}
			}
		}

		Map<Integer, User> users=UserStorage.getById(needUsers, false);
		Map<Integer, Group> groups=GroupStorage.getById(needGroups);
		for(SearchResult sr: results){
			switch(sr.type){
				case USER -> sr.user=users.get(sr.id);
				case GROUP -> sr.group=groups.get(sr.id);
			}
		}

		return results;
	}

	public static PaginatedList<Integer> searchUsers(String query, int selfID, int count) throws SQLException{
		query=query.strip();
		if(query.isEmpty())
			return PaginatedList.emptyList(count);
		if(query.length()<3){
			return searchUsersWithShortQuery(query, selfID, count);
		}
		query=prepareNameQuery(query);
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=DatabaseUtils.oneFieldToInt(SQLQueryBuilder.prepareStatement(conn,
					"SELECT COUNT(*) FROM qsearch_index WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND user_id IS NOT NULL", query).executeQuery());
			if(total==0)
				return PaginatedList.emptyList(count);
			ArrayList<Integer> results=new ArrayList<>();
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT user_id FROM qsearch_index " +
					"LEFT JOIN followings ON followings.followee_id=qsearch_index.user_id " +
					"WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND followings.follower_id=? ORDER BY followings.hints_rank DESC LIMIT ?", query, selfID, count);
			DatabaseUtils.intResultSetToStream(stmt.executeQuery(), null).forEach(results::add);
			if(results.size()<count){
				String notIn="";
				if(!results.isEmpty()){
					notIn=" AND user_id NOT IN ("+results.stream().map(Object::toString).collect(Collectors.joining(","))+")";
				}
				stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT user_id FROM qsearch_index WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND user_id IS NOT NULL"+notIn+" LIMIT ?", query, count);
				DatabaseUtils.intResultSetToStream(stmt.executeQuery(), null).forEach(id->{
					if(!results.contains(id) && results.size()<count)
						results.add(id);
				});
			}
			return new PaginatedList<>(results, total, 0, count);
		}
	}

	public static PaginatedList<Integer> searchFriends(String query, int selfID, int offset, int count, boolean useHints) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			query=prepareNameQuery(query);
			int total=DatabaseUtils.oneFieldToInt(SQLQueryBuilder.prepareStatement(conn,
					"SELECT COUNT(*) FROM qsearch_index WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND user_id IN (SELECT followee_id FROM followings WHERE follower_id=? AND mutual=1 AND accepted=1)",
					query, selfID).executeQuery());
			if(total==0)
				return PaginatedList.emptyList(count);
			String orderBy=useHints ? " ORDER BY hints_rank DESC" : "";
			List<Integer> list=DatabaseUtils.intResultSetToList(SQLQueryBuilder.prepareStatement(conn,
					"SELECT user_id FROM qsearch_index " +
							"RIGHT JOIN followings ON followee_id=user_id " +
							"WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND mutual=1 AND accepted=1 AND follower_id=?"+orderBy+" LIMIT ? OFFSET ?",
					query, selfID, count, offset).executeQuery());
			return new PaginatedList<>(list, total, offset, count);
		}
	}

	public static PaginatedList<Integer> searchGroups(String query, boolean events, int selfID, int offset, int count, boolean includePrivate) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			query=prepareNameQuery(query);
			String privateWhere=includePrivate ? "" : " AND groups.access_type<>2";
			int total=DatabaseUtils.oneFieldToInt(SQLQueryBuilder.prepareStatement(conn,
					"SELECT COUNT(*) FROM qsearch_index JOIN `groups` ON group_id=`groups`.id WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND `groups`.`type`=?"+privateWhere+" AND group_id IN (SELECT group_id FROM group_memberships WHERE user_id=? AND accepted=1)",
					query, events ? Group.Type.EVENT : Group.Type.GROUP, selfID).executeQuery());
			if(total==0)
				return PaginatedList.emptyList(count);
			String orderBy=includePrivate ? " ORDER BY hints_rank DESC" : "";
			List<Integer> list=DatabaseUtils.intResultSetToList(SQLQueryBuilder.prepareStatement(conn,
					"SELECT qsearch_index.group_id FROM qsearch_index " +
							"JOIN `groups` ON qsearch_index.group_id=`groups`.id " +
							"RIGHT JOIN group_memberships ON group_memberships.group_id=qsearch_index.group_id " +
							"WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND `groups`.`type`=?"+privateWhere+" AND group_memberships.user_id=? AND accepted=1"+orderBy+" LIMIT ? OFFSET ?",
					query, events ? Group.Type.EVENT : Group.Type.GROUP, selfID, count, offset).executeQuery());
			return new PaginatedList<>(list, total, offset, count);
		}
	}

	public static PaginatedList<Integer> searchAllGroups(String query, boolean events, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			query=prepareNameQuery(query);
			int total=DatabaseUtils.oneFieldToInt(SQLQueryBuilder.prepareStatement(conn,
					"SELECT COUNT(*) FROM qsearch_index JOIN `groups` ON group_id=`groups`.id WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND `groups`.`type`=? AND groups.access_type<>2",
					query, events ? Group.Type.EVENT : Group.Type.GROUP).executeQuery());
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Integer> list=DatabaseUtils.intResultSetToList(SQLQueryBuilder.prepareStatement(conn,
					"SELECT qsearch_index.group_id FROM qsearch_index " +
							"JOIN `groups` ON qsearch_index.group_id=`groups`.id " +
							"WHERE (MATCH(string) AGAINST (? IN BOOLEAN MODE)) AND `groups`.`type`=? AND groups.access_type<>2 LIMIT ? OFFSET ?",
					query, events ? Group.Type.EVENT : Group.Type.GROUP, count, offset).executeQuery());
			return new PaginatedList<>(list, total, offset, count);
		}
	}

	public static PaginatedList<Integer> searchBookmarkedUsers(String query, int selfID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			query=prepareNameQuery(query);
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
			query=prepareNameQuery(query);
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
