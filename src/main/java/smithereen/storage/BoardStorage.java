package smithereen.storage;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import smithereen.model.PaginatedList;
import smithereen.model.board.BoardTopic;
import smithereen.model.board.BoardTopicsSortOrder;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.storage.utils.Pair;

public class BoardStorage{
	public static PaginatedList<BoardTopic> getGroupTopics(int groupID, int offset, int count, BoardTopicsSortOrder order) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("board_topics")
					.count()
					.where("group_id=?", groupID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			String orderBy=switch(order){
				case CREATED_ASC -> "created_at ASC";
				case CREATED_DESC -> "created_at DESC";
				case UPDATED_ASC -> "updated_at ASC";
				case UPDATED_DESC -> "updated_at DESC";
			};
			List<BoardTopic> topics=new SQLQueryBuilder(conn)
					.selectFrom("board_topics")
					.allColumns()
					.where("group_id=?",groupID)
					.orderBy("ISNULL(pinned_at) ASC, pinned_at DESC, "+orderBy)
					.limit(count, offset)
					.executeAsStream(BoardTopic::fromResultSet)
					.toList();
			return new PaginatedList<>(topics, total, offset, count);
		}
	}

	public static PaginatedList<BoardTopic> getGroupPinnedTopics(int groupID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("board_topics")
					.count()
					.where("group_id=? AND pinned_at IS NOT NULL",groupID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<BoardTopic> topics=new SQLQueryBuilder(conn)
					.selectFrom("board_topics")
					.allColumns()
					.where("group_id=? AND pinned_at IS NOT NULL",groupID)
					.orderBy("pinned_at DESC")
					.limit(count, offset)
					.executeAsStream(BoardTopic::fromResultSet)
					.toList();
			return new PaginatedList<>(topics, total, offset, count);
		}
	}

	public static BoardTopic getTopic(long id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("board_topics")
				.where("id=?", id)
				.executeAndGetSingleObject(BoardTopic::fromResultSet);
	}

	public static Map<Long, BoardTopic> getTopics(Collection<Long> ids) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("board_topics")
				.whereIn("id", ids)
				.executeAsStream(BoardTopic::fromResultSet)
				.collect(Collectors.toMap(bt->bt.id, Function.identity()));
	}

	public static long createTopic(int groupID, String title, int authorID, String activityPubID, String activityPubURL) throws SQLException{
		return new SQLQueryBuilder()
				.insertInto("board_topics")
				.value("title", title)
				.value("author_id", authorID)
				.value("group_id", groupID)
				.value("last_comment_author_id", authorID)
				.value("ap_id", activityPubID)
				.value("ap_url", activityPubURL)
				.executeAndGetIDLong();
	}

	public static long getTopicIDByActivityPubID(URI apID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("board_topics")
				.columns("id")
				.where("ap_id=?", apID.toString())
				.executeAndGetLong();
	}

	public static long putForeignTopic(BoardTopic topic) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			long id=new SQLQueryBuilder(conn)
					.selectFrom("board_topics")
					.columns("id")
					.where("ap_id=?", topic.apID.toString())
					.executeAndGetLong();
			if(id!=-1)
				return id;
			id=new SQLQueryBuilder(conn)
					.insertInto("board_topics")
					.value("title", topic.title)
					.value("author_id", topic.authorID)
					.value("group_id", topic.groupID)
					.value("last_comment_author_id", topic.authorID)
					.value("ap_id", topic.getActivityPubID())
					.value("ap_url", topic.getActivityPubURL())
					.value("created_at", topic.createdAt)
					.value("updated_at", topic.updatedAt)
					.executeAndGetIDLong();
			return id;
		}
	}

	public static void setTopicActivityPubID(long id, URI apID, URI apURL) throws SQLException{
		new SQLQueryBuilder()
				.update("board_topics")
				.where("id=? AND ap_id IS NULL", id)
				.value("ap_id", apID.toString())
				.value("ap_url", apURL==null ? apID.toString() : apURL.toString())
				.executeNoResult();
	}

	public static void setTopicFirstCommentID(long topicID, long commentID) throws SQLException{
		new SQLQueryBuilder()
				.update("board_topics")
				.where("id=?", topicID)
				.value("first_comment_id", commentID)
				.executeNoResult();
	}

	public static void deleteTopic(long topicID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("board_topics")
				.where("id=?", topicID)
				.executeNoResult();
	}

	public static void renameTopic(long topicID, String title) throws SQLException{
		new SQLQueryBuilder()
				.update("board_topics")
				.where("id=?", topicID)
				.value("title", title)
				.executeNoResult();
	}

	public static Map<Long, Integer> getGroupIDsForTopics(Set<Long> topicIDs) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("board_topics")
				.columns("id", "group_id")
				.whereIn("id", topicIDs)
				.executeAsStream(r->new Pair<>(r.getLong(1), r.getInt(2)))
				.collect(Collectors.toMap(Pair::first, Pair::second));
	}

	public static void setTopicClosed(long topicID, boolean closed) throws SQLException{
		new SQLQueryBuilder()
				.update("board_topics")
				.value("is_closed", closed)
				.where("id=?", topicID)
				.executeNoResult();
	}

	public static void setTopicPinned(long topicID, boolean pinned) throws SQLException{
		new SQLQueryBuilder()
				.update("board_topics")
				.valueExpr("pinned_at", pinned ? "CURRENT_TIMESTAMP()" : "NULL")
				.where("id=?", topicID)
				.executeNoResult();
	}

	public static void setTopicPinned(long topicID, Instant pinnedAt) throws SQLException{
		new SQLQueryBuilder()
				.update("board_topics")
				.value("pinned_at", pinnedAt)
				.where("id=?", topicID)
				.executeNoResult();
	}
}
