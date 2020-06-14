package smithereen.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

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

	public static List<Integer> getPostLikes(int objectID, int selfID, int offset, int count) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT user_id FROM likes WHERE object_id=? AND object_type=? ORDER BY id ASC LIMIT ?,?");
		stmt.setInt(1, objectID);
		stmt.setInt(2, TYPE_POST);
		stmt.setInt(3, offset);
		stmt.setInt(4, count+1);
		ArrayList<Integer> result=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					int id=res.getInt(1);
					if(id==selfID)
						continue;
					result.add(id);
					if(result.size()==count)
						break;
				}while(res.next());
			}
		}
		return result;
	}
}
