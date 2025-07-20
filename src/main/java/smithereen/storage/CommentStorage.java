package smithereen.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import smithereen.Utils;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.LocalImage;
import smithereen.model.PaginatedList;
import smithereen.model.PostSource;
import smithereen.model.User;
import smithereen.model.comments.Comment;
import smithereen.model.comments.CommentParentObjectID;
import smithereen.model.comments.CommentableObjectType;
import smithereen.model.feed.CommentsNewsfeedObjectType;
import smithereen.model.media.MediaFileRecord;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.storage.utils.Pair;
import smithereen.text.FormattedTextFormat;
import smithereen.text.FormattedTextSource;
import smithereen.util.BackgroundTaskRunner;

public class CommentStorage{
	private static final Logger LOG=LoggerFactory.getLogger(CommentStorage.class);

	public static long createComment(int authorID, int ownerID, CommentParentObjectID parentID, String text, FormattedTextSource source,
									 List<Long> replyKey, Set<Integer> mentionedUsers, String attachments, String contentWarning) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			long id=new SQLQueryBuilder(conn)
					.insertInto("comments")
					.value("author_id", authorID)
					.value("owner_user_id", ownerID>0 ? ownerID : null)
					.value("owner_group_id", ownerID<0 ? -ownerID : null)
					.value("parent_object_type", parentID.type())
					.value("parent_object_id", parentID.id())
					.value("text", text)
					.value("attachments", attachments)
					.value("content_warning", contentWarning)
					.value("reply_key", replyKey!=null && !replyKey.isEmpty() ? Utils.serializeLongCollection(replyKey) : null)
					.value("mentions", mentionedUsers!=null ? Utils.serializeIntList(mentionedUsers) : null)
					.value("source", source.source())
					.value("source_format", source.format())
					.executeAndGetIDLong();

			if(replyKey!=null && !replyKey.isEmpty()){
				new SQLQueryBuilder(conn)
						.update("comments")
						.valueExpr("reply_count", "reply_count+1")
						.whereIn("id", replyKey)
						.executeNoResult();
			}
			CommentsNewsfeedObjectType mappedType=parentID.type().newsfeedType();
			if(mappedType!=null){
				new SQLQueryBuilder(conn)
						.insertIgnoreInto("newsfeed_comments")
						.value("user_id", authorID)
						.value("object_type", mappedType)
						.value("object_id", parentID.id())
						.executeNoResult();
				BackgroundTaskRunner.getInstance().submit(new UpdateCommentBookmarksRunnable(parentID));
			}

			if(parentID.type()==CommentableObjectType.BOARD_TOPIC){
				new SQLQueryBuilder(conn)
						.update("board_topics")
						.where("id=?", parentID.id())
						.valueExpr("num_comments", "num_comments+1")
						.valueExpr("updated_at", "CURRENT_TIMESTAMP()")
						.value("last_comment_author_id", authorID)
						.executeNoResult();
			}

			return id;
		}
	}

	public static Comment getComment(long id) throws SQLException{
		Comment c=new SQLQueryBuilder()
				.selectFrom("comments")
				.where("id=?", id)
				.executeAndGetSingleObject(Comment::fromResultSet);
		if(c==null)
			return null;
		postprocessComments(List.of(c));
		return c;
	}

	public static Map<Long, Comment> getComments(Collection<Long> ids) throws SQLException{
		Map<Long, Comment> comments=new SQLQueryBuilder()
				.selectFrom("comments")
				.whereIn("id", ids)
				.executeAsStream(Comment::fromResultSet)
				.collect(Collectors.toMap(c->c.id, Function.identity()));
		postprocessComments(comments.values());
		return comments;
	}

	public static Map<Long, Integer> getCommentCounts(CommentableObjectType type, Collection<Long> ids) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("comments")
				.selectExpr("parent_object_id, count(*)")
				.whereIn("parent_object_id", ids)
				.andWhere("parent_object_type=?", type)
				.groupBy("parent_object_id")
				.executeAsStream(res->new Pair<>(res.getLong(1), res.getInt(2)))
				.collect(Collectors.toMap(Pair::first, Pair::second));
	}

	public static Map<CommentParentObjectID, PaginatedList<Comment>> getCommentsForFeed(Collection<CommentParentObjectID> ids, boolean flat, int limit) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			String query=ids.stream().map(id->"(SELECT * FROM comments WHERE parent_object_type="+id.type().ordinal()+" AND parent_object_id="+id.id()+(flat ? "" : " AND reply_key IS NULL")+" ORDER BY created_at DESC LIMIT "+limit+")").collect(Collectors.joining(" UNION ALL "));
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, query);
			HashMap<CommentParentObjectID, PaginatedList<Comment>> result=new HashMap<>();
			HashSet<Comment> allComments=new HashSet<>();
			try(ResultSet res=stmt.executeQuery()){
				DatabaseUtils.resultSetToObjectStream(res, Comment::fromResultSet, null)
						.forEach(c->{
							result.computeIfAbsent(c.parentObjectID, id->new PaginatedList<>(new ArrayList<>(), 0)).list.addFirst(c);
							allComments.add(c);
						});
			}
			postprocessComments(allComments);
			if(!result.isEmpty()){
				stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT parent_object_type, parent_object_id, COUNT(*) FROM comments where (parent_object_type, parent_object_id) IN (" +
						String.join(", ", Collections.nCopies(result.size(), "(?, ?)")) +
						")"+(flat ? "" : " AND reply_key IS NULL")+" GROUP BY parent_object_type, parent_object_id", result.keySet().stream().flatMap(id->Stream.of(id.type(), id.id())).toArray());
				try(ResultSet res=stmt.executeQuery()){
					while(res.next()){
						Objects.requireNonNull(result.get(new CommentParentObjectID(CommentableObjectType.values()[res.getInt(1)], res.getLong(2)))).total=res.getInt(3);
					}
				}
			}
			return result;
		}
	}

	public static PaginatedList<Comment> getCommentsWithMaxID(CommentParentObjectID parentID, long maxID, int limit, boolean flat) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("comments")
					.count()
					.where("parent_object_type=? AND parent_object_id=? AND created_at<=(SELECT `created_at` FROM `comments` WHERE id=?) AND id<>?"+(flat ? "" : " AND reply_key IS NULL"), parentID.type(), parentID.id(), maxID, maxID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(limit);
			List<Comment> list=new SQLQueryBuilder(conn)
					.selectFrom("comments")
					.allColumns()
					.where("parent_object_type=? AND parent_object_id=? AND created_at<=(SELECT `created_at` FROM `comments` WHERE id=?) AND id<>?"+(flat ? "" : " AND reply_key IS NULL"), parentID.type(), parentID.id(), maxID, maxID)
					.limit(limit, 0)
					.orderBy("created_at DESC")
					.executeAsStream(Comment::fromResultSet)
					.toList()
					.reversed();
			postprocessComments(list);
			return new PaginatedList<>(list, total, 0, limit);
		}
	}

	public static PaginatedList<Comment> getCommentsFlat(CommentParentObjectID parentID, List<Long> prefix, int offset, int limit) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			byte[] serializedPrefix=prefix!=null ? Utils.serializeLongCollection(prefix) : null;

			SQLQueryBuilder b=new SQLQueryBuilder(conn)
					.selectFrom("comments")
					.count();
			if(serializedPrefix==null)
				b.where("parent_object_type=? AND parent_object_id=?", parentID.type(), parentID.id());
			else
				b.where("parent_object_type=? AND parent_object_id=? AND reply_key LIKE BINARY bin_prefix(?)", parentID.type(), parentID.id(), serializedPrefix);
			int total=b.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(limit);

			b=new SQLQueryBuilder(conn)
					.selectFrom("comments")
					.allColumns();
			if(serializedPrefix==null)
				b.where("parent_object_type=? AND parent_object_id=?", parentID.type(), parentID.id());
			else
				b.where("parent_object_type=? AND parent_object_id=? AND reply_key LIKE BINARY bin_prefix(?)", parentID.type(), parentID.id(), serializedPrefix);
			List<Comment> list=b.limit(limit, offset)
					.orderBy("created_at ASC")
					.executeAsStream(Comment::fromResultSet)
					.toList();
			postprocessComments(list);
			return new PaginatedList<>(list, total, offset, limit);
		}
	}


	public static ThreadedReplies getRepliesThreaded(CommentParentObjectID parentID, List<Long> prefix, int topLevelOffset, int topLevelLimit, int secondaryLimit, boolean twoLevel) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			byte[] serializedPrefix=prefix.isEmpty() ? null : Utils.serializeLongCollection(prefix);

			SQLQueryBuilder b=new SQLQueryBuilder(conn)
					.selectFrom("comments")
					.count();
			if(serializedPrefix!=null)
				b.where("parent_object_type=? AND parent_object_id=? AND reply_key=?", parentID.type(), parentID.id(), serializedPrefix);
			else
				b.where("parent_object_type=? AND parent_object_id=? AND reply_key IS NULL", parentID.type(), parentID.id());
			int total=b.executeAndGetInt();

			if(total==0)
				return new ThreadedReplies(List.of(), List.of(), 0);

			b=new SQLQueryBuilder(conn)
					.selectFrom("comments")
					.allColumns();
			if(serializedPrefix!=null)
				b.where("parent_object_type=? AND parent_object_id=? AND reply_key=?", parentID.type(), parentID.id(), serializedPrefix);
			else
				b.where("parent_object_type=? AND parent_object_id=? AND reply_key IS NULL", parentID.type(), parentID.id());
			List<Comment> comments=b.limit(topLevelLimit, topLevelOffset)
					.orderBy("created_at ASC")
					.executeAsStream(Comment::fromResultSet)
					.toList();
			postprocessComments(comments);

			ArrayList<String> wheres=new ArrayList<>();
			ArrayList<Object> whereArgs=new ArrayList<>();
			whereArgs.add(parentID.type());
			whereArgs.add(parentID.id());
			for(Comment comment:comments){
				if(comment.replyCount>0){
					wheres.add("reply_key LIKE BINARY bin_prefix(?)");
					whereArgs.add(Utils.serializeLongCollection(comment.getReplyKeyForReplies()));
				}
			}

			List<Comment> replies;
			if(!wheres.isEmpty()){
				whereArgs.add(secondaryLimit);
				PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT * FROM comments WHERE parent_object_type=? AND parent_object_id=? AND ("+String.join(" OR ", wheres)+") ORDER BY created_at ASC"+(twoLevel ? "" : ", LENGTH(reply_key) ASC")+" LIMIT ?",
						whereArgs.toArray());
				replies=DatabaseUtils.resultSetToObjectStream(stmt.executeQuery(), Comment::fromResultSet, null).toList();
				postprocessComments(replies);
			}else{
				replies=List.of();
			}

			return new ThreadedReplies(comments, replies, total);
		}
	}


	public static Map<Long, Integer> getCommentAuthors(Collection<Long> ids) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("comments")
				.columns("id", "author_id")
				.whereIn("id", ids)
				.executeAsStream(res->new Pair<>(res.getLong("id"), res.getInt("author_id")))
				.collect(Collectors.toMap(Pair::first, Pair::second));
	}

	public static void deleteComment(Comment comment) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt;
			boolean needFullyDelete=new SQLQueryBuilder(conn)
					.selectFrom("comments")
					.count()
					.where("reply_key LIKE BINARY bin_prefix(?)", (Object) Utils.serializeLongCollection(comment.getReplyKeyForReplies()))
					.executeAndGetInt()==0;

			if(needFullyDelete){
				new SQLQueryBuilder(conn)
						.deleteFrom("comments")
						.where("id=?", comment.id)
						.executeNoResult();
			}else{
				new SQLQueryBuilder(conn)
						.update("comments")
						.value("author_id", null)
						.value("owner_user_id", null)
						.value("owner_group_id", null)
						.value("text", null)
						.value("source", null)
						.value("attachments", null)
						.value("content_warning", null)
						.value("updated_at", null)
						.value("mentions", null)
						.where("id=?", comment.id)
						.executeNoResult();
			}

			if(comment.getReplyLevel()>0){
				new SQLQueryBuilder(conn)
						.update("comments")
						.valueExpr("reply_count", "GREATEST(1, reply_count)-1")
						.whereIn("id", comment.replyKey)
						.andWhere("parent_object_type=? AND parent_object_id=?", comment.parentObjectID.type(), comment.parentObjectID.id())
						.executeNoResult();
			}

			if(comment.parentObjectID.type()==CommentableObjectType.BOARD_TOPIC){
				Timestamp updatedAt;
				int lastAuthorID;
				try(ResultSet res=new SQLQueryBuilder(conn)
						.selectFrom("comments")
						.columns("created_at", "author_id")
						.where("parent_object_type=? AND parent_object_id=?", comment.parentObjectID.type(), comment.parentObjectID.id())
						.orderBy("created_at DESC")
						.limit(1, 0)
						.execute()){
					res.next();
					updatedAt=res.getTimestamp(1);
					lastAuthorID=res.getInt(2);
				}
				new SQLQueryBuilder(conn)
						.update("board_topics")
						.where("id=?", comment.parentObjectID.id())
						.valueExpr("num_comments", "num_comments-1")
						.value("updated_at", updatedAt)
						.value("last_comment_author_id", lastAuthorID)
						.executeNoResult();
			}
			BackgroundTaskRunner.getInstance().submit(new UpdateCommentBookmarksRunnable(comment.parentObjectID));
		}
	}

	public static Set<Long> getPhotoAlbumCommentIDsForDeletion(long albumID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("comments")
				.columns("id")
				.where("parent_object_type=? AND parent_object_id IN (SELECT id FROM photos WHERE album_id=?)", CommentableObjectType.PHOTO, albumID)
				.limit(500, 0)
				.executeAndGetLongStream()
				.boxed()
				.collect(Collectors.toSet());
	}

	public static void deleteComments(Collection<Long> ids) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("comments")
				.whereIn("id", ids)
				.executeNoResult();
	}

	public static PostSource getCommentSource(long id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("comments")
				.columns("source", "source_format")
				.where("id=?", id)
				.executeAndGetSingleObject(r->new PostSource(r.getString(1), FormattedTextFormat.values()[r.getInt(2)]));
	}

	public static void updateComment(long id, String text, String textSource, FormattedTextFormat sourceFormat, Set<User> mentionedUsers, String attachments, String contentWarning) throws SQLException{
		new SQLQueryBuilder()
				.update("comments")
				.value("text", text)
				.value("source", textSource)
				.value("source_format", sourceFormat)
				.value("mentions", mentionedUsers.isEmpty() ? null : Utils.serializeIntArray(mentionedUsers.stream().mapToInt(u->u.id).toArray()))
				.value("attachments", attachments)
				.value("content_warning", contentWarning)
				.valueExpr("updated_at", "CURRENT_TIMESTAMP()")
				.where("id=?", id)
				.executeNoResult();
	}

	public static PaginatedList<Comment> getPhotoAlbumComments(long albumID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("comments")
					.count()
					.where("parent_object_type=? AND parent_object_id IN (SELECT id FROM photos WHERE album_id=?)", CommentableObjectType.PHOTO, albumID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Comment> comments=new SQLQueryBuilder(conn)
					.selectFrom("comments")
					.allColumns()
					.where("parent_object_type=? AND parent_object_id IN (SELECT id FROM photos WHERE album_id=?)", CommentableObjectType.PHOTO, albumID)
					.orderBy("created_at ASC")
					.limit(count, offset)
					.executeAsStream(Comment::fromResultSet)
					.toList();
			postprocessComments(comments);
			return new PaginatedList<>(comments, total, offset, count);
		}
	}

	public static Set<URI> getPhotoAlbumForeignComments(long albumID, Set<URI> filter) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("comments")
				.columns("ap_id")
				.whereIn("ap_id", filter.stream().map(Object::toString).collect(Collectors.toSet()))
				.andWhere("parent_object_type=? AND parent_object_id IN (SELECT id FROM photos WHERE album_id=?)", CommentableObjectType.PHOTO, albumID)
				.executeAsStream(r->URI.create(r.getString(1)))
				.collect(Collectors.toSet());
	}

	public static Set<Long> getPhotoAlbumLocalComments(long albumID, Set<Long> filter) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("comments")
				.columns("id")
				.whereIn("id", filter)
				.andWhere("parent_object_type=? AND parent_object_id IN (SELECT id FROM photos WHERE album_id=?)", CommentableObjectType.PHOTO, albumID)
				.executeAndGetLongStream()
				.boxed()
				.collect(Collectors.toSet());
	}

	public static Set<URI> getObjectForeignComments(CommentParentObjectID id, Set<URI> filter) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("comments")
				.columns("ap_id")
				.whereIn("ap_id", filter.stream().map(Object::toString).collect(Collectors.toSet()))
				.andWhere("parent_object_type=? AND parent_object_id=?", id.type(), id.id())
				.executeAsStream(r->URI.create(r.getString(1)))
				.collect(Collectors.toSet());
	}

	public static Set<Long> getObjectLocalComments(CommentParentObjectID id, Set<Long> filter) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("comments")
				.columns("id")
				.whereIn("id", filter)
				.andWhere("parent_object_type=? AND parent_object_id=?", id.type(), id.id())
				.executeAndGetLongStream()
				.boxed()
				.collect(Collectors.toSet());
	}

	public static PaginatedList<Comment> getCommentReplies(CommentParentObjectID parentID, List<Long> replyKey, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			SQLQueryBuilder b=new SQLQueryBuilder(conn)
					.selectFrom("comments")
					.count()
					.where("parent_object_type=? AND parent_object_id=?", parentID.type(), parentID.id());
			if(replyKey!=null && !replyKey.isEmpty())
				b.andWhere("reply_key=?", (Object) Utils.serializeLongCollection(replyKey));
			else
				b.andWhere("reply_key IS NULL");
			int total=b.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);

			b=new SQLQueryBuilder(conn)
					.selectFrom("comments")
					.allColumns()
					.limit(count, offset)
					.orderBy("created_at ASC")
					.where("parent_object_type=? AND parent_object_id=?", parentID.type(), parentID.id());
			if(replyKey!=null && !replyKey.isEmpty())
				b.andWhere("reply_key=?", (Object) Utils.serializeLongCollection(replyKey));
			else
				b.andWhere("reply_key IS NULL");
			List<Comment> comments=b.executeAsStream(Comment::fromResultSet)
					.toList();
			postprocessComments(comments);
			return new PaginatedList<>(comments, total, offset, count);
		}
	}

	public static void putOrUpdateForeignComment(Comment comment) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			if(comment.id==0){
				comment.id=new SQLQueryBuilder(conn)
						.insertInto("comments")
						.value("author_id", comment.authorID)
						.value("owner_user_id", comment.ownerID>0 ? comment.ownerID : null)
						.value("owner_group_id", comment.ownerID<0 ? -comment.ownerID : null)
						.value("parent_object_type", comment.parentObjectID.type())
						.value("parent_object_id", comment.parentObjectID.id())
						.value("text", comment.text)
						.value("attachments", comment.serializeAttachments())
						.value("ap_id", comment.getActivityPubID().toASCIIString())
						.value("ap_url", comment.getActivityPubURL().toASCIIString())
						.value("created_at", comment.createdAt)
						.value("content_warning", comment.contentWarning)
						.value("reply_key", Utils.serializeLongCollection(comment.replyKey))
						.value("mentions", Utils.serializeIntList(comment.mentionedUserIDs))
						.value("ap_replies", comment.activityPubReplies==null ? null : comment.activityPubReplies.toASCIIString())
						.executeAndGetIDLong();
				if(!comment.replyKey.isEmpty()){
					new SQLQueryBuilder(conn)
							.update("comments")
							.valueExpr("reply_count", "reply_count+1")
							.whereIn("id", comment.replyKey)
							.executeNoResult();
				}
				if(comment.parentObjectID.type()==CommentableObjectType.BOARD_TOPIC){
					Timestamp updatedAt;
					int lastAuthorID;
					try(ResultSet res=new SQLQueryBuilder(conn)
							.selectFrom("comments")
							.columns("created_at", "author_id")
							.where("parent_object_type=? AND parent_object_id=?", comment.parentObjectID.type(), comment.parentObjectID.id())
							.orderBy("created_at DESC")
							.limit(1, 0)
							.execute()){
						res.next();
						updatedAt=res.getTimestamp(1);
						lastAuthorID=res.getInt(2);
					}
					new SQLQueryBuilder(conn)
							.update("board_topics")
							.where("id=?", comment.parentObjectID.id())
							.valueExpr("num_comments", "num_comments+1")
							.value("updated_at", updatedAt)
							.value("last_comment_author_id", lastAuthorID)
							.executeNoResult();
				}
			}else{
				new SQLQueryBuilder(conn)
						.update("comments")
						.where("id=?", comment.id)
						.value("text", comment.text)
						.value("attachments", comment.serializeAttachments())
						.value("content_warning", comment.contentWarning)
						.value("mentions", Utils.serializeIntList(comment.mentionedUserIDs))
						.value("updated_at", comment.updatedAt==null ? Instant.now() : comment.updatedAt)
						.executeNoResult();
			}
		}
	}

	public static long getCommentIdByActivityPubId(URI activityPubID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("comments")
				.columns("id")
				.where("ap_id=?", activityPubID.toString())
				.executeAndGetLong();
	}

	public static List<Long> getCommentIDsForDeletion(CommentParentObjectID parentID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("comments")
				.columns("id")
				.limit(500, 0)
				.where("parent_object_type=? AND parent_object_id=?", parentID.type(), parentID.id())
				.executeAndGetLongStream()
				.boxed()
				.toList();
	}

	public static void deleteCommentBookmarks(CommentParentObjectID parentID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("newsfeed_comments")
				.where("object_type=? AND object_id=?", parentID.type().newsfeedType(), parentID.id())
				.executeNoResult();
	}

	public static void deleteCommentBookmarks(CommentableObjectType type, Collection<Long> ids) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("newsfeed_comments")
				.whereIn("object_id", ids)
				.andWhere("object_type=?", type.newsfeedType())
				.executeNoResult();
	}

	public static void deleteCommentBookmarksForPhotoAlbum(long albumID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("newsfeed_comments")
				.where("object_type=? AND object_id IN (SELECT id FROM photos WHERE album_id=?)", CommentsNewsfeedObjectType.PHOTO, albumID)
				.executeNoResult();
	}

	public static List<Comment> getUserReplies(int userID, Collection<List<Long>> replyKeys) throws SQLException{
		List<Comment> comments=new SQLQueryBuilder()
				.selectFrom("comments")
				.allColumns()
				.whereIn("reply_key", replyKeys.stream().map(Utils::serializeLongCollection).toList())
				.andWhere("author_id=?", userID)
				.executeAsStream(Comment::fromResultSet)
				.toList();
		postprocessComments(comments);
		return comments;
	}

	public static Set<URI> getInboxesForCommentInteractionForwarding(CommentParentObjectID parentID, Set<Integer> exceptUsers) throws SQLException{
		SQLQueryBuilder b=new SQLQueryBuilder()
				.selectFrom("users")
				.distinct()
				.selectExpr("IFNULL(ap_shared_inbox, ap_inbox)")
				.where("id IN (SELECT DISTINCT author_id FROM comments WHERE parent_object_type=? AND parent_object_id=?) AND ap_id IS NOT NULL", parentID.type(), parentID.id());

		if(!exceptUsers.isEmpty())
			b.andWhere("id NOT IN ("+String.join(", ", Collections.nCopies(exceptUsers.size(), "?"))+")", exceptUsers.toArray());

		return b.executeAsStream(r->URI.create(r.getString(1)))
				.collect(Collectors.toSet());
	}

	public static int getCommentIndex(CommentParentObjectID parentID, long id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("comments")
				.selectExpr("ROW_NUMBER() OVER(ORDER BY created_at ASC) AS rownum")
				.where("parent_object_type=? AND parent_object_id=?", parentID.type(), parentID.id())
				.orderBy("(id="+id+") DESC")
				.limit(1, 0)
				.executeAndGetInt();
	}

	private static void postprocessComments(Collection<Comment> posts) throws SQLException{
		Set<Long> needFileIDs=posts.stream()
				.filter(p->p.attachments!=null && !p.attachments.isEmpty())
				.flatMap(p->p.attachments.stream())
				.map(att->att instanceof LocalImage li ? li.fileID : 0L)
				.filter(id->id!=0)
				.collect(Collectors.toSet());
		if(needFileIDs.isEmpty())
			return;
		Map<Long, MediaFileRecord> mediaFiles=MediaStorage.getMediaFileRecords(needFileIDs);
		for(Comment post:posts){
			if(post.attachments!=null){
				for(ActivityPubObject attachment:post.attachments){
					if(attachment instanceof LocalImage li){
						MediaFileRecord mfr=mediaFiles.get(li.fileID);
						if(mfr!=null)
							li.fillIn(mfr);
					}
				}
			}
		}
	}

	private record UpdateCommentBookmarksRunnable(CommentParentObjectID parentID) implements Runnable{
		@Override
		public void run(){
			try{
				try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
					Timestamp ts=new SQLQueryBuilder(conn)
							.selectFrom("comments")
							.selectExpr("MAX(created_at)")
							.where("parent_object_type=? AND parent_object_id=?", parentID.type(), parentID.id())
							.executeAndGetSingleObject(res->res.getTimestamp(1));
					if(ts==null){
						new SQLQueryBuilder(conn)
								.deleteFrom("newsfeed_comments")
								.where("object_type=? AND object_id=?", parentID.type().newsfeedType(), parentID.id())
								.executeNoResult();
					}else{
						new SQLQueryBuilder(conn)
								.update("newsfeed_comments")
								.value("last_comment_time", ts)
								.where("object_type=? AND object_id=?", parentID.type().newsfeedType(), parentID.id())
								.executeNoResult();
					}
				}
			}catch(SQLException x){
				LOG.warn("Error updating comment bookmarks for {}", parentID, x);
			}
		}
	}

	public record ThreadedReplies(List<Comment> posts, List<Comment> replies, int total){}
}
