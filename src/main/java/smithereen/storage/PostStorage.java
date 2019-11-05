package smithereen.storage;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import smithereen.data.Post;

public class PostStorage{
	public static int createUserWallPost(int userID, int ownerID, String text) throws SQLException{
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("INSERT INTO `wall_posts` (`author_id`, `owner_user_id`, `text`) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
		stmt.setInt(1, userID);
		stmt.setInt(2, ownerID);
		stmt.setString(3, text);
		stmt.execute();
		try(ResultSet keys=stmt.getGeneratedKeys()){
			keys.first();
			return keys.getInt(1);
		}
	}

	public static void putForeignWallPost(Post post) throws SQLException{
		Post existing=getPostByID(post.activityPubID);

		PreparedStatement stmt;
		if(existing==null){
			stmt=DatabaseConnectionManager.getConnection().prepareStatement("INSERT INTO `wall_posts` (`author_id`, `owner_user_id`, `text`, `attachments`, `content_warning`, `ap_url`, `ap_id`) VALUES (?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			stmt.setInt(1, post.user.id);
			stmt.setInt(2, post.owner.id);
			stmt.setString(3, post.content);
			stmt.setString(4, post.serializeAttachments());
			stmt.setString(5, post.summary);
			stmt.setString(6, post.url.toString());
			stmt.setString(7, post.activityPubID.toString());
		}else{
			stmt=DatabaseConnectionManager.getConnection().prepareStatement("UPDATE `wall_posts` SET `text`=?, `attachments`=?, `content_warning`=? WHERE `ap_id`=?");
			stmt.setString(1, post.content);
			stmt.setString(2, post.serializeAttachments());
			stmt.setString(3, post.summary);

			stmt.setString(4, post.activityPubID.toString());
		}
		stmt.execute();
		if(existing==null){
			try(ResultSet res=stmt.getGeneratedKeys()){
				res.first();
				post.id=res.getInt(1);
			}
		}else{
			post.id=existing.id;
		}
	}

	public static List<Post> getFeed(int userID) throws SQLException{
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT * FROM wall_posts WHERE owner_user_id=author_id AND author_id IN (SELECT followee_id FROM followings WHERE follower_id=? UNION SELECT ?) ORDER BY created_at DESC LIMIT 25");
		stmt.setInt(1, userID);
		stmt.setInt(2, userID);
		ArrayList<Post> posts=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					posts.add(Post.fromResultSet(res));
				}while(res.next());
			}
		}
		return posts;
	}

	public static List<Post> getUserWall(int userID, int minID, int maxID, int[] total) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt;
		if(total!=null){
			stmt=conn.prepareStatement("SELECT COUNT(*) FROM `wall_posts` WHERE `owner_user_id`=?");
			stmt.setInt(1, userID);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				total[0]=res.getInt(1);
			}
		}
		if(minID>0){
			stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `owner_user_id`=? AND `id`>? ORDER BY created_at DESC LIMIT 25");
			stmt.setInt(2, minID);
		}else if(maxID>0){
			stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `owner_user_id`=? AND `id`<? ORDER BY created_at DESC LIMIT 25");
			stmt.setInt(2, maxID);
		}else{
			stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `owner_user_id`=? ORDER BY created_at DESC LIMIT 25");
		}
		stmt.setInt(1, userID);
		ArrayList<Post> posts=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					posts.add(Post.fromResultSet(res));
				}while(res.next());
			}
		}
		return posts;
	}

	public static Post getPostByID(int userID, int postID) throws SQLException{
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT * FROM wall_posts WHERE id=? AND owner_user_id=?");
		stmt.setInt(1, postID);
		stmt.setInt(2, userID);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				return Post.fromResultSet(res);
			}
		}
		return null;
	}

	public static Post getPostByID(URI apID) throws SQLException{
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT * FROM `wall_posts` WHERE `ap_id`=?");
		stmt.setString(1, apID.toString());
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				return Post.fromResultSet(res);
			}
		}
		return null;
	}

	public static void deletePost(int id) throws SQLException{
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("DELETE FROM `wall_posts` WHERE `id`=?");
		stmt.setInt(1, id);
		stmt.execute();
	}
}
