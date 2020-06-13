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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.ObjectNotFoundException;
import smithereen.Utils;
import smithereen.data.User;
import smithereen.data.UserInteractions;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.data.Post;
import smithereen.data.feed.PostNewsfeedEntry;
import smithereen.data.feed.RetootNewsfeedEntry;

public class PostStorage{
	public static int createUserWallPost(int userID, int ownerID, String text, int[] replyKey, List<User> mentionedUsers, String attachments) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("INSERT INTO `wall_posts` (`author_id`, `owner_user_id`, `text`, `reply_key`, `mentions`, `attachments`) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
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
		byte[] mentions=null;
		if(!mentionedUsers.isEmpty()){
			ByteArrayOutputStream b=new ByteArrayOutputStream(mentionedUsers.size()*4);
			try{
				DataOutputStream o=new DataOutputStream(b);
				for(User user:mentionedUsers)
					o.writeInt(user.id);
			}catch(IOException ignore){}
			mentions=b.toByteArray();
		}
		stmt.setBytes(5, mentions);
		stmt.setString(6, attachments);
		stmt.execute();
		try(ResultSet keys=stmt.getGeneratedKeys()){
			keys.first();
			int id=keys.getInt(1);
			if(userID==ownerID && replyKey==null){
				stmt=conn.prepareStatement("INSERT INTO `newsfeed` (`type`, `author_id`, `object_id`) VALUES (?, ?, ?)");
				stmt.setInt(1, NewsfeedEntry.Type.POST.ordinal());
				stmt.setInt(2, userID);
				stmt.setInt(3, id);
				stmt.execute();
			}
			return id;
		}
	}

	public static void putForeignWallPost(Post post) throws SQLException{
		Post existing=getPostByID(post.activityPubID);
		Connection conn=DatabaseConnectionManager.getConnection();

		PreparedStatement stmt;
		if(existing==null){
			stmt=conn.prepareStatement("INSERT INTO `wall_posts` (`author_id`, `owner_user_id`, `text`, `attachments`, `content_warning`, `ap_url`, `ap_id`, `reply_key`, `created_at`, `mentions`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
			stmt.setInt(1, post.user.id);
			stmt.setInt(2, post.owner.id);
			stmt.setString(3, post.content);
			stmt.setString(4, post.serializeAttachments());
			stmt.setString(5, post.summary);
			stmt.setString(6, post.url.toString());
			stmt.setString(7, post.activityPubID.toString());
			byte[] replyKey=Utils.serializeIntArray(post.replyKey);
			stmt.setBytes(8, replyKey);
			stmt.setTimestamp(9, new Timestamp(post.published.getTime()));
			byte[] mentions=null;
			if(!post.mentionedUsers.isEmpty()){
				int[] _mentions=new int[post.mentionedUsers.size()];
				for(int i=0;i<post.mentionedUsers.size();i++)
					_mentions[i]=post.mentionedUsers.get(i).id;
				mentions=Utils.serializeIntArray(_mentions);
			}
			stmt.setBytes(10, mentions);
		}else{
			stmt=DatabaseConnectionManager.getConnection().prepareStatement("UPDATE `wall_posts` SET `text`=?, `attachments`=?, `content_warning`=?, `mentions`=? WHERE `ap_id`=?");
			stmt.setString(1, post.content);
			stmt.setString(2, post.serializeAttachments());
			stmt.setString(3, post.summary);
			byte[] mentions=null;
			if(!post.mentionedUsers.isEmpty()){
				int[] _mentions=new int[post.mentionedUsers.size()];
				for(int i=0;i<post.mentionedUsers.size();i++)
					_mentions[i]=post.mentionedUsers.get(i).id;
				mentions=Utils.serializeIntArray(_mentions);
			}
			stmt.setBytes(4, mentions);

			stmt.setString(5, post.activityPubID.toString());
		}
		stmt.execute();
		if(existing==null){
			try(ResultSet res=stmt.getGeneratedKeys()){
				res.first();
				post.id=res.getInt(1);
			}
			if(post.owner.equals(post.user) && post.getReplyLevel()==0){
				stmt=conn.prepareStatement("INSERT INTO `newsfeed` (`type`, `author_id`, `object_id`, `time`) VALUES (?, ?, ?, ?)");
				stmt.setInt(1, NewsfeedEntry.Type.POST.ordinal());
				stmt.setInt(2, post.user.id);
				stmt.setInt(3, post.id);
				stmt.setTimestamp(4, new Timestamp(post.published.getTime()));
				stmt.execute();
			}
		}else{
			post.id=existing.id;
		}
	}

	public static List<NewsfeedEntry> getFeed(int userID, int startFromID, int offset, int[] total) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt;
		if(total!=null){
			stmt=conn.prepareStatement("SELECT COUNT(*) FROM `newsfeed` WHERE `author_id` IN (SELECT followee_id FROM followings WHERE follower_id=? UNION SELECT ?) AND `id`<=?");
			stmt.setInt(1, userID);
			stmt.setInt(2, userID);
			stmt.setInt(3, startFromID==0 ? Integer.MAX_VALUE : startFromID);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				total[0]=res.getInt(1);
			}
		}
		stmt=conn.prepareStatement("SELECT `type`, `object_id`, `author_id`, `id` FROM `newsfeed` WHERE `author_id` IN (SELECT followee_id FROM followings WHERE follower_id=? UNION SELECT ?) AND `id`<=? ORDER BY `time` DESC LIMIT ?,25");
		stmt.setInt(1, userID);
		stmt.setInt(2, userID);
		stmt.setInt(3, startFromID==0 ? Integer.MAX_VALUE : startFromID);
		stmt.setInt(4, offset);
		ArrayList<NewsfeedEntry> posts=new ArrayList<>();
		ArrayList<Integer> needPosts=new ArrayList<>();
		HashMap<Integer, Post> postMap=new HashMap<>();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					NewsfeedEntry.Type type=NewsfeedEntry.Type.values()[res.getInt(1)];
					NewsfeedEntry _entry=null;
					switch(type){
						case POST:{
							PostNewsfeedEntry entry=new PostNewsfeedEntry();
							entry.objectID=res.getInt(2);
							posts.add(entry);
							needPosts.add(entry.objectID);
							_entry=entry;
							break;
						}
						case RETOOT:{
							RetootNewsfeedEntry entry=new RetootNewsfeedEntry();
							entry.objectID=res.getInt(2);
							entry.author=UserStorage.getById(res.getInt(3));
							posts.add(entry);
							needPosts.add(entry.objectID);
							_entry=entry;
							break;
						}
					}
					_entry.type=type;
					_entry.id=res.getInt(4);
				}while(res.next());
			}
		}
		if(!needPosts.isEmpty()){
			StringBuilder sb=new StringBuilder();
			sb.append("SELECT * FROM `wall_posts` WHERE `id` IN (");
			boolean first=true;
			for(int id:needPosts){
				if(!first){
					sb.append(',');
				}else{
					first=false;
				}
				sb.append(id);
			}
			sb.append(')');
			try(ResultSet res=conn.createStatement().executeQuery(sb.toString())){
				if(res.first()){
					do{
						Post post=Post.fromResultSet(res);
						postMap.put(post.id, post);
					}while(res.next());
				}
			}
			for(NewsfeedEntry e:posts){
				if(e instanceof PostNewsfeedEntry){
					Post post=postMap.get(e.objectID);
					if(post!=null)
						((PostNewsfeedEntry) e).post=post;
				}
			}
		}
		return posts;
	}

	public static List<Post> getUserWall(int userID, int minID, int maxID, int offset, int[] total) throws SQLException{
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
			stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `owner_user_id`=? AND `id`=<? AND `reply_key` IS NULL ORDER BY created_at DESC LIMIT "+offset+",25");
			stmt.setInt(2, maxID);
		}else{
			stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `owner_user_id`=? AND `reply_key` IS NULL ORDER BY created_at DESC LIMIT "+offset+",25");
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

	public static Post getPostByID(int postID) throws SQLException{
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT * FROM wall_posts WHERE id=?");
		stmt.setInt(1, postID);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				return Post.fromResultSet(res);
			}
		}
		return null;
	}

	public static Post getPostByID(URI apID) throws SQLException{
		if(Config.isLocal(apID)){
			String[] pathParts=apID.getPath().split("/");
			String posts=pathParts[1];
			int postID=Utils.parseIntOrDefault(pathParts[2], 0);
			if(!"posts".equals(posts) || postID==0){
				throw new ObjectNotFoundException("Invalid local URL "+apID);
			}
			return getPostByID(postID);
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
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("DELETE FROM `wall_posts` WHERE `id`=?");
		stmt.setInt(1, id);
		stmt.execute();
		stmt=conn.prepareStatement("DELETE FROM `newsfeed` WHERE (`type`=0 OR `type`=1) AND `object_id`=?");
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
				return Config.localURI("/posts/"+postID);
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

	public static HashMap<Integer, UserInteractions> getPostInteractions(List<Integer> postIDs, int userID) throws SQLException{
		HashMap<Integer, UserInteractions> result=new HashMap<>();
		if(postIDs.isEmpty())
			return result;
		for(int id:postIDs)
			result.put(id, new UserInteractions());
		String idsStr=postIDs.stream().map(Object::toString).collect(Collectors.joining(","));

		Connection conn=DatabaseConnectionManager.getConnection();
		try(ResultSet res=conn.createStatement().executeQuery("SELECT object_id, COUNT(*) FROM likes WHERE object_type=1 AND object_id IN ("+idsStr+") GROUP BY object_id")){
			if(res.first()){
				do{
					result.get(res.getInt(1)).likeCount=res.getInt(2);
				}while(res.next());
			}
		}
		if(userID!=0){
			PreparedStatement stmt=conn.prepareStatement("SELECT object_id FROM likes WHERE object_type=1 AND object_id IN ("+idsStr+") AND user_id=?");
			stmt.setInt(1, userID);
			try(ResultSet res=stmt.executeQuery()){
				if(res.first()){
					do{
						result.get(res.getInt(1)).isLiked=true;
					}while(res.next());
				}
			}
		}

		return result;
	}
}
