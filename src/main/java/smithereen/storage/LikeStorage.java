package smithereen.storage;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import smithereen.Config;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.activitypub.objects.activities.Like;
import smithereen.model.ForeignUser;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;

public class LikeStorage{

	public static int setObjectLiked(int userID, long objectID, Like.ObjectType objectType, boolean liked, URI apID) throws SQLException{
		if(liked)
			return putLike(userID, objectID, objectType, apID);
		else
			return deleteLike(userID, objectID, objectType);
	}

	public static int putLike(int userID, long objectID, Like.ObjectType type, URI apID) throws SQLException{
		return new SQLQueryBuilder()
				.insertIgnoreInto("likes")
				.value("user_id", userID)
				.value("object_id", objectID)
				.value("object_type", type)
				.value("ap_id", Objects.toString(apID, null))
				.executeAndGetID();
	}

	private static int deleteLike(int userID, long objectID, Like.ObjectType type) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int id=new SQLQueryBuilder(conn)
					.selectFrom("likes")
					.columns("id")
					.where("user_id=? AND object_id=? AND object_type=?", userID, objectID, type.ordinal())
					.executeAndGetInt();
			if(id==-1)
				return -1;
			new SQLQueryBuilder(conn)
					.deleteFrom("likes")
					.where("id=?", id)
					.executeNoResult();
			return id;
		}
	}

	public static void deleteAllLikesForObject(long objectID, Like.ObjectType type) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("likes")
				.where("object_id=? AND object_type=?", objectID, type)
				.executeNoResult();
	}

	public static void deleteAllLikesForObjects(Collection<Long> objectIDs, Like.ObjectType type) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("likes")
				.whereIn("object_id", objectIDs)
				.andWhere("object_type=?", type)
				.executeNoResult();
	}

	public static PaginatedList<Like> getLikes(long objectID, URI objectApID, Like.ObjectType type, int offset, int count) throws SQLException{
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

	public static PaginatedList<Integer> getLikes(long objectID, Like.ObjectType objectType, int selfID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("likes")
					.count()
					.where("object_id=? AND object_type=?", objectID, objectType)
					.executeAndGetInt();

			if(total==0)
				return PaginatedList.emptyList(count);

			List<Integer> userIDs=new SQLQueryBuilder(conn)
					.selectFrom("likes")
					.columns("user_id")
					.where("object_id=? AND object_type=? AND user_id<>?", objectID, objectType, selfID)
					.orderBy("id ASC")
					.limit(count, offset)
					.executeAndGetIntStream()
					.boxed().toList();
			return new PaginatedList<>(userIDs, total, offset, count);
		}
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

	public static PaginatedList<Long> getLikedObjectIDs(int ownerID, Like.ObjectType type, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("likes")
					.count()
					.where("user_id=? AND object_type=?", ownerID, type)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Long> ids=new SQLQueryBuilder(conn)
					.selectFrom("likes")
					.columns("object_id")
					.where("user_id=? AND object_type=?", ownerID, type)
					.orderBy("id DESC")
					.limit(count, offset)
					.executeAndGetLongStream()
					.boxed()
					.toList();
			return new PaginatedList<>(ids, total, offset, count);
		}
	}

	public static PaginatedList<Integer> getLikedPostsTopLevelOnly(int ownerID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=DatabaseUtils.oneFieldToInt(SQLQueryBuilder.prepareStatement(conn,
					"SELECT COUNT(*) FROM likes JOIN wall_posts ON likes.object_id=wall_posts.id WHERE likes.user_id=? AND likes.object_type=0 AND wall_posts.reply_key IS NULL", ownerID).executeQuery());
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Integer> ids=DatabaseUtils.intResultSetToList(SQLQueryBuilder.prepareStatement(conn,
					"SELECT likes.object_id FROM likes JOIN wall_posts ON likes.object_id=wall_posts.id WHERE likes.user_id=? AND likes.object_type=0 AND wall_posts.reply_key IS NULL ORDER BY likes.id DESC LIMIT ? OFFSET ?",
					ownerID, count, offset).executeQuery());
			return new PaginatedList<>(ids, total, offset, count);
		}
	}

	public static void fillLikesInInteractions(Map<Long, UserInteractions> interactions, Like.ObjectType type, int selfID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			ResultSet res=new SQLQueryBuilder(conn)
					.selectFrom("likes")
					.selectExpr("object_id, count(*)")
					.whereIn("object_id", interactions.keySet())
					.andWhere("object_type=?", type)
					.groupBy("object_id")
					.execute();
			try(res){
				while(res.next()){
					interactions.get(res.getLong(1)).likeCount=res.getInt(2);
				}
			}
			if(selfID!=0){
				new SQLQueryBuilder(conn)
						.selectFrom("likes")
						.columns("object_id")
						.whereIn("object_id", interactions.keySet())
						.andWhere("object_type=? AND user_id=?", type, selfID)
						.executeAndGetLongStream()
						.forEach(id->interactions.get(id).isLiked=true);
			}
		}
	}
}
