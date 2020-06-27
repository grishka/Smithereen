package smithereen.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import smithereen.Config;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.activities.Like;
import smithereen.data.ForeignUser;
import smithereen.data.User;

public class LikeStorage{

	private static final int TYPE_POST=1;

	public static int setPostLiked(int userID, int objectID, boolean liked) throws SQLException{
		if(liked)
			return putLike(userID, objectID, TYPE_POST);
		else
			return deleteLike(userID, objectID, TYPE_POST);
	}

	private static int putLike(int userID, int objectID, int type) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("INSERT IGNORE INTO `likes` (`user_id`, `object_id`, `object_type`) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
		stmt.setInt(1, userID);
		stmt.setInt(2, objectID);
		stmt.setInt(3, type);
		stmt.execute();
		try(ResultSet res=stmt.getGeneratedKeys()){
			res.first();
			return res.getInt(1);
		}
	}

	private static int deleteLike(int userID, int objectID, int type) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT id FROM `likes` WHERE `user_id`=? AND `object_id`=? AND `object_type`=?");
		stmt.setInt(1, userID);
		stmt.setInt(2, objectID);
		stmt.setInt(3, type);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				int id=res.getInt(1);
				stmt=conn.prepareStatement("DELETE FROM likes WHERE id=?");
				stmt.setInt(1, id);
				stmt.execute();
				return id;
			}
			return 0;
		}
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

	public static Like getByID(int id) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT user_id, object_id, object_type FROM likes WHERE id=?");
		stmt.setInt(1, id);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				int userID=res.getInt(1);
				User user=UserStorage.getById(userID);
				if(user instanceof ForeignUser)
					return null;
				Like like=new Like();
				like.activityPubID=Config.localURI("/activitypub/objects/likes/"+id);
				like.object=new LinkOrObject(Config.localURI("/posts/"+res.getInt(2)));
				like.actor=new LinkOrObject(Config.localURI("/users/"+userID));
				return like;
			}
			return null;
		}
	}
}
