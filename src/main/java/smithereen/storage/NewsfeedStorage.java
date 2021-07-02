package smithereen.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import smithereen.data.feed.NewsfeedEntry;

public class NewsfeedStorage{

	public static void putEntry(int userID, int objectID, NewsfeedEntry.Type type, Timestamp time) throws SQLException{
		SQLQueryBuilder b=new SQLQueryBuilder()
				.insertIgnoreInto("newsfeed")
				.value("type", type)
				.value("author_id", userID)
				.value("object_id", objectID);
		if(time!=null)
			b.value("time", time);
		b.createStatement().execute();
	}

	public static void deleteEntry(int userID, int objectID, NewsfeedEntry.Type type) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("newsfeed")
				.where("type=? AND author_id=? AND object_id=?", type.ordinal(), userID, objectID)
				.createStatement()
				.execute();
	}
}
