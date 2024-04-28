package smithereen.storage;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import smithereen.Config;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.activities.Like;
import smithereen.model.ForeignUser;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;

public class LikeStorage{

	public static int setPostLiked(int userID, int objectID, boolean liked) throws SQLException{
		if(liked)
			return putLike(userID, objectID, Like.ObjectType.POST, null);
		else
			return deleteLike(userID, objectID, Like.ObjectType.POST);
	}

	private static int putLike(int userID, int objectID, Like.ObjectType type, URI apID) throws SQLException{
		return new SQLQueryBuilder()
				.insertIgnoreInto("likes")
				.value("user_id", userID)
				.value("object_id", objectID)
				.value("object_type", type)
				.value("ap_id", Objects.toString(apID, null))
				.executeAndGetID();
	}

	private static int deleteLike(int userID, int objectID, Like.ObjectType type) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int id=new SQLQueryBuilder(conn)
					.selectFrom("likes")
					.columns("id")
					.where("user_id=? AND object_id=? AND object_type=?", userID, objectID, type.ordinal())
					.executeAndGetInt();
			if(id==-1)
				return 0;
			new SQLQueryBuilder(conn)
					.deleteFrom("likes")
					.where("id=?", id)
					.executeNoResult();
			return id;
		}
	}

	public static PaginatedList<Like> getLikes(int objectID, URI objectApID, Like.ObjectType type, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT likes.id AS like_id, user_id, likes.ap_id AS like_ap_id, users.ap_id AS user_ap_id FROM likes JOIN users ON users.id=likes.user_id WHERE object_id=? AND object_type=? ORDER BY likes.id ASC LIMIT ?,?",
					objectID, type, offset, count);
			ArrayList<Like> likes=new ArrayList<>();
			LinkOrObject objApID=new LinkOrObject(objectApID);
			try(ResultSet res=stmt.executeQuery()){
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
			int total=new SQLQueryBuilder(conn)
					.selectFrom("likes")
					.count()
					.where("object_id=? AND object_type=?", objectID, type.ordinal())
					.executeAndGetInt();
			return new PaginatedList<>(likes, total);
		}
	}

	public static List<Integer> getPostLikes(int objectID, int selfID, int offset, int count) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("likes")
				.columns("user_id")
				.where("object_id=? AND object_type=? AND user_id<>?", objectID, Like.ObjectType.POST.ordinal(), selfID)
				.orderBy("id ASC")
				.limit(count, offset)
				.executeAndGetIntStream()
				.boxed().toList();
	}

	public static Like getByID(int id) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=conn.prepareStatement("SELECT user_id, object_id, object_type FROM likes WHERE id=?");
			stmt.setInt(1, id);
			try(ResultSet res=stmt.executeQuery()){
				if(res.next()){
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

}
