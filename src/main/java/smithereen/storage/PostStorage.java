package smithereen.storage;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import smithereen.Config;
import smithereen.ObjectNotFoundException;
import smithereen.Utils;
import smithereen.data.Post;
import smithereen.data.User;

public class PostStorage{
	public static int createUserWallPost(int userID, int ownerID, String text, int[] replyKey) throws SQLException{
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("INSERT INTO `wall_posts` (`author_id`, `owner_user_id`, `text`, `reply_key`) VALUES (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
		stmt.setInt(1, userID);
		stmt.setInt(2, ownerID);
		stmt.setString(3, text);
		byte[] _replyKey=null;
		if(replyKey!=null){
			ByteArrayOutputStream b=new ByteArrayOutputStream(replyKey.length*4);
			try{
				DataOutputStream o=new DataOutputStream(b);
				for(int id:replyKey)
					o.writeInt(id);
			}catch(IOException ignore){}
			_replyKey=b.toByteArray();
		}
		stmt.setBytes(4, _replyKey);
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
			stmt=DatabaseConnectionManager.getConnection().prepareStatement("INSERT INTO `wall_posts` (`author_id`, `owner_user_id`, `text`, `attachments`, `content_warning`, `ap_url`, `ap_id`, `reply_key`) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			stmt.setInt(1, post.user.id);
			stmt.setInt(2, post.owner.id);
			stmt.setString(3, post.content);
			stmt.setString(4, post.serializeAttachments());
			stmt.setString(5, post.summary);
			stmt.setString(6, post.url.toString());
			stmt.setString(7, post.activityPubID.toString());
			byte[] replyKey=null;
			if(post.replyKey.length>0){
				ByteArrayOutputStream b=new ByteArrayOutputStream(post.replyKey.length*4);
				try{
					DataOutputStream o=new DataOutputStream(b);
					for(int id:post.replyKey)
						o.writeInt(id);
				}catch(IOException ignore){}
				replyKey=b.toByteArray();
			}
			stmt.setBytes(8, replyKey);
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
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT * FROM `wall_posts` WHERE `reply_key` IS NULL AND `owner_user_id`=`author_id` AND `author_id` IN (SELECT followee_id FROM followings WHERE follower_id=? UNION SELECT ?) ORDER BY created_at DESC LIMIT 25");
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
			stmt=conn.prepareStatement("SELECT COUNT(*) FROM `wall_posts` WHERE `owner_user_id`=? AND `reply_key` IS NULL");
			stmt.setInt(1, userID);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				total[0]=res.getInt(1);
			}
		}
		if(minID>0){
			stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `owner_user_id`=? AND `id`>? AND `reply_key` IS NULL ORDER BY created_at DESC LIMIT 25");
			stmt.setInt(2, minID);
		}else if(maxID>0){
			stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `owner_user_id`=? AND `id`<? AND `reply_key` IS NULL ORDER BY created_at DESC LIMIT 25");
			stmt.setInt(2, maxID);
		}else{
			stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `owner_user_id`=? AND `reply_key` IS NULL ORDER BY created_at DESC LIMIT 25");
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
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT * FROM wall_posts WHERE id=?");
		stmt.setInt(1, postID);
		//stmt.setInt(2, userID);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				return Post.fromResultSet(res);
			}
		}
		return null;
	}

	public static Post getPostByID(URI apID) throws SQLException{
		if(apID.getHost().equalsIgnoreCase(Config.domain)){
			String[] pathParts=apID.getPath().split("/");
			// /grishka/posts/1
			String username=pathParts[1];
			String posts=pathParts[2];
			int postID=Utils.parseIntOrDefault(pathParts[3], 0);
			if(!"posts".equals(posts) || postID==0){
				throw new ObjectNotFoundException("Invalid local URL");
			}
			User user=UserStorage.getByUsername(username);
			if(user==null){
				throw new ObjectNotFoundException("Post owner does not exist");
			}
			return getPostByID(user.id, postID);
		}
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

	public static List<Post> getRepliesForFeed(int postID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `reply_key`=? ORDER BY `id` ASC LIMIT 3");
		stmt.setBytes(1, new byte[]{
				(byte)((postID >> 24) & 0xFF),
				(byte)((postID >> 16) & 0xFF),
				(byte)((postID >> 8) & 0xFF),
				(byte)((postID) & 0xFF)
		});
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

	public static List<Post> getReplies(int[] prefix) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `reply_key` LIKE BINARY bin_prefix(?) ESCAPE CHAR(255) ORDER BY `reply_key` ASC, `id` ASC LIMIT 100");
		byte[] replyKey;
		ByteArrayOutputStream b=new ByteArrayOutputStream(prefix.length*4);
		try{
			DataOutputStream o=new DataOutputStream(b);
			for(int id:prefix)
				o.writeInt(id);
		}catch(IOException ignore){}
		replyKey=b.toByteArray();
		stmt.setBytes(1, replyKey);
		ArrayList<Post> posts=new ArrayList<>();
		HashMap<Integer, Post> postMap=new HashMap<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					Post post=Post.fromResultSet(res);
					postMap.put(post.id, post);
					posts.add(post);
				}while(res.next());
			}
		}
		for(Post post:posts){
			if(post.getReplyLevel()>prefix.length){
				Post parent=postMap.get(post.replyKey[post.replyKey.length-1]);
				if(parent!=null){
					parent.replies.add(post);
				}
			}
		}
		Iterator<Post> itr=posts.iterator();
		while(itr.hasNext()){
			Post post=itr.next();
			if(post.getReplyLevel()>prefix.length)
				itr.remove();
		}

		return posts;
	}

	public static URI getActivityPubID(int postID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT `ap_id`,`owner_user_id` FROM `wall_posts` WHERE `id`=?");
		stmt.setInt(1, postID);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				if(res.getString(1)!=null)
					return URI.create(res.getString(1));
				return Config.localURI("/"+UserStorage.getById(res.getInt(2)).getFullUsername()+"/posts/"+postID);
			}
		}
		return null;
	}

	public static int getOwnerForPost(int postID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT `owner_user_id` FROM `wall_posts` WHERE `id`=?");
		stmt.setInt(1, postID);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first())
				return res.getInt(1);
		}
		return 0;
	}

	public static int getLocalPostCount(boolean comments) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		try(ResultSet res=conn.createStatement().executeQuery("SELECT COUNT(*) FROM `wall_posts` WHERE `ap_id` IS NULL AND `reply_key` IS "+(comments ? "NOT " : "")+"NULL")){
			res.first();
			return res.getInt(1);
		}
	}
}
