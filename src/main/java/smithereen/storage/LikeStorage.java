package smithereen.storage;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import smithereen.Config;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.activities.Like;
import smithereen.data.ForeignUser;
import smithereen.data.ListAndTotal;
import smithereen.data.User;

public class LikeStorage{

	public static int setPostLiked(int userID, int objectID, boolean liked) throws SQLException{
		if(liked)
			return putLike(userID, objectID, Like.ObjectType.POST, null);
		else
			return deleteLike(userID, objectID, Like.ObjectType.POST);
	}

	private static int putLike(int userID, int objectID, Like.ObjectType type, URI apID) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.insertIgnoreInto("likes")
				.value("user_id", userID)
				.value("object_id", objectID)
				.value("object_type", type)
				.value("ap_id", Objects.toString(apID, null))
				.createStatement(Statement.RETURN_GENERATED_KEYS);
		stmt.execute();
		return DatabaseUtils.oneFieldToInt(stmt.getGeneratedKeys());
	}

	private static int deleteLike(int userID, int objectID, Like.ObjectType type) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.selectFrom("likes")
				.columns("id")
				.where("user_id=? AND object_id=? AND object_type=?", userID, objectID, type.ordinal())
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				int id=res.getInt(1);
				new SQLQueryBuilder(conn)
						.deleteFrom("likes")
						.where("id=?", id)
						.createStatement()
						.execute();
				return id;
			}
			return 0;
		}
	}

	public static ListAndTotal<Like> getLikes(int objectID, URI objectApID, Like.ObjectType type, int offset, int count) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT likes.id AS like_id, user_id, likes.ap_id AS like_ap_id, users.ap_id AS user_ap_id FROM likes JOIN users ON users.id=likes.user_id WHERE object_id=? AND object_type=? ORDER BY likes.id ASC LIMIT ?,?",
				objectID, type, offset, count);
		ArrayList<Like> likes=new ArrayList<>();
		LinkOrObject objApID=new LinkOrObject(objectApID);
		try(ResultSet res=stmt.executeQuery()){
			res.beforeFirst();
			while(res.next()){
				Like like=new Like();
				String userID=res.getString("user_ap_id");
				like.actor=new LinkOrObject(userID==null ? Config.localURI("/users/"+res.getInt("user_id")) : URI.create(userID));
				if(userID==null){
					like.activityPubID=Config.localURI("/activitypub/objects/likes/"+res.getInt("like_id"));
				}else{
					String id=res.getString("like_ap_id");
					like.activityPubID=id==null ? null : URI.create(id);
				}
				like.object=objApID;
				likes.add(like);
			}
		}
		stmt=new SQLQueryBuilder(conn)
				.selectFrom("likes")
				.count()
				.where("object_id=? AND object_type=?", objectID, type.ordinal())
				.createStatement();
		return new ListAndTotal<>(likes, DatabaseUtils.oneFieldToInt(stmt.executeQuery()));
	}

	public static List<Integer> getPostLikes(int objectID, int selfID, int offset, int count) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("likes")
				.columns("user_id")
				.where("object_id=? AND object_type=?", objectID, Like.ObjectType.POST.ordinal())
				.orderBy("id ASC")
				.limit(count+1, offset)
				.createStatement();
		ArrayList<Integer> result=DatabaseUtils.intResultSetToList(stmt.executeQuery());
		result.removeIf(i -> i==selfID);
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
