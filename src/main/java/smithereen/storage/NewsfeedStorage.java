package smithereen.storage;

import java.sql.SQLException;
import java.time.Instant;

import smithereen.model.feed.NewsfeedEntry;
import smithereen.storage.sql.SQLQueryBuilder;

public class NewsfeedStorage{

	public static void putEntry(int userID, int objectID, NewsfeedEntry.Type type, Instant time) throws SQLException{
		SQLQueryBuilder b=new SQLQueryBuilder()
				.insertIgnoreInto("newsfeed")
				.value("type", type)
				.value("author_id", userID)
				.value("object_id", objectID);
		if(time!=null)
			b.value("time", time);
		b.executeNoResult();
	}

	public static void deleteEntry(int userID, int objectID, NewsfeedEntry.Type type) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("newsfeed")
				.where("type=? AND author_id=? AND object_id=?", type.ordinal(), userID, objectID)
				.executeNoResult();
	}
}
