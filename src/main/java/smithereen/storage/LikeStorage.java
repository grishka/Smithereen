package smithereen.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LikeStorage{

	private static final int TYPE_POST=1;

	public static void setPostLiked(int userID, int objectID, boolean liked) throws SQLException{
		if(liked)
			putLike(userID, objectID, TYPE_POST);
		else
			deleteLike(userID, objectID, TYPE_POST);
	}

	private static void putLike(int userID, int objectID, int type) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("INSERT IGNORE INTO `likes` (`user_id`, `object_id`, `object_type`) VALUES (?, ?, ?)");
		stmt.setInt(1, userID);
		stmt.setInt(2, objectID);
		stmt.setInt(3, type);
		stmt.execute();
	}

	private static void deleteLike(int userID, int objectID, int type) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("DELETE FROM `likes` WHERE `user_id`=? AND `object_id`=? AND `object_type`=?");
		stmt.setInt(1, userID);
		stmt.setInt(2, objectID);
		stmt.setInt(3, type);
		stmt.execute();
	}
}
