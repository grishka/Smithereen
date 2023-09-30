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

import smithereen.Utils;
import smithereen.model.Group;
import smithereen.model.SearchResult;
import smithereen.model.User;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;

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

	public static List<SearchResult> search(String query, int selfID, int maxCount) throws SQLException{
		query=Arrays.stream(Utils.transliterate(query).replaceAll("[()\\[\\]*+~<>\\\"@-]", " ").split("[ \t]+")).filter(Predicate.not(String::isBlank)).map(s->'+'+s+'*').collect(Collectors.joining(" "));
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
}
