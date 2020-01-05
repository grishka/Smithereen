package smithereen.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import smithereen.data.feed.NewsfeedEntry;

public class NewsfeedStorage{

	public static void putRetoot(int authorID, int postID, Timestamp published) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("INSERT INTO `newsfeed` (`type`, `author_id`, `object_id`, `time`) VALUES (?, ?, ?, ?)");
		stmt.setInt(1, NewsfeedEntry.TYPE_RETOOT);
		stmt.setInt(2, authorID);
		stmt.setInt(3, postID);
		stmt.setTimestamp(4, published);
		stmt.execute();
	}

	public static void deleteRetoot(int authorID, int postID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("DELETE FROM `newsfeed` WHERE `type`=? AND `author_id`=? AND `object_id`=?");
		stmt.setInt(1, NewsfeedEntry.TYPE_RETOOT);
		stmt.setInt(2, authorID);
		stmt.setInt(3, postID);
		stmt.execute();
	}
}
