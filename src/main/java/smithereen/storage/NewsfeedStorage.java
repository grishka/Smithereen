package smithereen.storage;

import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;

import smithereen.model.PaginatedList;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;

public class NewsfeedStorage{

	public static PaginatedList<NewsfeedEntry> getFriendsFeed(int userID, long startFromID, int offset, int count, EnumSet<NewsfeedEntry.Type> types) throws SQLException{
		if(types.isEmpty())
			return PaginatedList.emptyList(count);
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("newsfeed")
					.count()
					.whereIn("type", types)
					.andWhere("((`author_id` IN (SELECT followee_id FROM followings WHERE follower_id=? AND accepted=1 AND muted=0) OR (type=0 AND author_id=?)) AND `id`<=? AND `time`>DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL 10 DAY))", userID, userID, startFromID==0 ? Integer.MAX_VALUE : startFromID)
					.executeAndGetInt();
			List<NewsfeedEntry> feed=new SQLQueryBuilder(conn)
					.selectFrom("newsfeed")
					.columns("type", "object_id", "author_id", "id", "time")
					.whereIn("type", types)
					.andWhere("((`author_id` IN (SELECT followee_id FROM followings WHERE follower_id=? AND accepted=1 AND muted=0) OR (type=0 AND author_id=?)) AND `id`<=?)", userID, userID, startFromID==0 ? Integer.MAX_VALUE : startFromID)
					.orderBy("time DESC")
					.limit(count, offset)
					.executeAsStream(NewsfeedEntry::fromResultSet)
					.toList();
			return new PaginatedList<>(feed, total, offset, count);
		}
	}

	public static void putFriendsEntry(int userID, long objectID, NewsfeedEntry.Type type, Instant time) throws SQLException{
		SQLQueryBuilder b=new SQLQueryBuilder()
				.insertIgnoreInto("newsfeed")
				.value("type", type)
				.value("author_id", userID)
				.value("object_id", objectID);
		if(time!=null)
			b.value("time", time);
		b.executeNoResult();
	}

	public static void deleteFriendsEntry(int userID, long objectID, NewsfeedEntry.Type type) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("newsfeed")
				.where("type=? AND author_id=? AND object_id=?", type.ordinal(), userID, objectID)
				.executeNoResult();
	}

	public static void deleteAllFriendsEntriesForObject(long objectID, NewsfeedEntry.Type type) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("newsfeed")
				.where("type=? AND object_id=?", type.ordinal(), objectID)
				.executeNoResult();
	}

	public static PaginatedList<NewsfeedEntry> getGroupsFeed(int userID, long startFromID, int offset, int count, EnumSet<NewsfeedEntry.Type> types) throws SQLException{
		if(types.isEmpty())
			return PaginatedList.emptyList(count);
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("newsfeed_groups")
					.count()
					.whereIn("type", types)
					.andWhere("(`group_id` IN (SELECT group_id FROM group_memberships WHERE user_id=? AND accepted=1) AND `id`<=? AND `time`>DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL 10 DAY))", userID, startFromID==0 ? Integer.MAX_VALUE : startFromID)
					.executeAndGetInt();
			List<NewsfeedEntry> feed=new SQLQueryBuilder(conn)
					.selectFrom("newsfeed_groups")
					.columns("type", "object_id", "group_id", "id", "time")
					.whereIn("type", types)
					.andWhere("(`group_id` IN (SELECT group_id FROM group_memberships WHERE user_id=? AND accepted=1) AND `id`<=?)", userID, startFromID==0 ? Integer.MAX_VALUE : startFromID)
					.orderBy("time DESC")
					.limit(count, offset)
					.executeAsStream(NewsfeedEntry::fromGroupsResultSet)
					.toList();
			return new PaginatedList<>(feed, total, offset, count);
		}
	}

	public static void putGroupsEntry(int groupID, long objectID, NewsfeedEntry.Type type, Instant time) throws SQLException{
		SQLQueryBuilder b=new SQLQueryBuilder()
				.insertIgnoreInto("newsfeed_groups")
				.value("type", type)
				.value("group_id", groupID)
				.value("object_id", objectID);
		if(time!=null)
			b.value("time", time);
		b.executeNoResult();
	}

	public static void deleteGroupsEntry(int groupID, long objectID, NewsfeedEntry.Type type) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("newsfeed_groups")
				.where("type=? AND group_id=? AND object_id=?", type.ordinal(), groupID, objectID)
				.executeNoResult();
	}

	public static void deleteAllGroupsEntriesForObject(long objectID, NewsfeedEntry.Type type) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("newsfeed_groups")
				.where("type=? AND object_id=?", type.ordinal(), objectID)
				.executeNoResult();
	}
}
