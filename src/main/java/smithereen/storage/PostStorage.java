package smithereen.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.ActivityPubObject;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.controllers.WallController;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.FederationState;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.model.PaginatedList;
import smithereen.model.Poll;
import smithereen.model.PollOption;
import smithereen.model.Post;
import smithereen.model.PostSource;
import smithereen.model.User;
import smithereen.model.UserInteractions;
import smithereen.model.feed.CommentsNewsfeedObjectType;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.model.media.MediaFileRecord;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.storage.utils.Pair;
import smithereen.text.FormattedTextFormat;
import smithereen.util.BackgroundTaskRunner;
import smithereen.util.NamedMutexCollection;
import smithereen.util.UriBuilder;
import spark.utils.StringUtils;

public class PostStorage{
	private static final Logger LOG=LoggerFactory.getLogger(PostStorage.class);

	private static final NamedMutexCollection foreignPostUpdateLocks=new NamedMutexCollection();
	private static final NamedMutexCollection pollVoteLocks=new NamedMutexCollection();

	public static int createWallPost(int userID, int ownerUserID, int ownerGroupID, String text, String textSource, FormattedTextFormat sourceFormat, List<Integer> replyKey,
									 Set<User> mentionedUsers, String attachments, String contentWarning, int pollID, int repostOf, Post.Action action, EnumSet<Post.Flag> flags) throws SQLException{
		if(ownerUserID<=0 && ownerGroupID<=0)
			throw new IllegalArgumentException("Need either ownerUserID or ownerGroupID");

		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int id=new SQLQueryBuilder(conn)
					.insertInto("wall_posts")
					.value("author_id", userID)
					.value("owner_user_id", ownerUserID>0 ? ownerUserID : null)
					.value("owner_group_id", ownerGroupID>0 ? ownerGroupID : null)
					.value("text", text)
					.value("reply_key", Utils.serializeIntList(replyKey))
					.value("mentions", mentionedUsers.isEmpty() ? null : Utils.serializeIntArray(mentionedUsers.stream().mapToInt(u->u.id).toArray()))
					.value("attachments", attachments)
					.value("content_warning", contentWarning)
					.value("poll_id", pollID>0 ? pollID : null)
					.value("source", textSource)
					.value("source_format", sourceFormat)
					.value("repost_of", repostOf!=0 ? repostOf : null)
					.value("action", action)
					.value("flags", Utils.serializeEnumSet(flags))
					.executeAndGetID();

			if(replyKey!=null && !replyKey.isEmpty()){
				new SQLQueryBuilder(conn)
						.update("wall_posts")
						.valueExpr("reply_count", "reply_count+1")
						.whereIn("id", replyKey)
						.executeNoResult();

				SQLQueryBuilder.prepareStatement(conn, "INSERT INTO newsfeed_comments (user_id, object_type, object_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE object_id=object_id", userID, 0, replyKey.getFirst()).execute();
				BackgroundTaskRunner.getInstance().submit(new UpdateCommentBookmarksRunnable(replyKey.getFirst()));
			}
			return id;
		}
	}

	public static void updateWallPost(int id, String text, String textSource, FormattedTextFormat sourceFormat, Set<User> mentionedUsers, String attachments, String contentWarning, int pollID) throws SQLException{
		new SQLQueryBuilder()
				.update("wall_posts")
				.value("text", text)
				.value("source", textSource)
				.value("source_format", sourceFormat)
				.value("mentions", mentionedUsers.isEmpty() ? null : Utils.serializeIntArray(mentionedUsers.stream().mapToInt(u->u.id).toArray()))
				.value("attachments", attachments)
				.value("content_warning", contentWarning)
				.value("poll_id", pollID>0 ? pollID : null)
				.valueExpr("updated_at", "CURRENT_TIMESTAMP()")
				.where("id=?", id)
				.executeNoResult();
	}

	public static void updateWallPostCW(int id, String contentWarning) throws SQLException{
		new SQLQueryBuilder()
				.update("wall_posts")
				.value("content_warning", contentWarning)
				.where("id=?", id)
				.executeNoResult();
	}

	private static int putForeignPoll(DatabaseConnection conn, int ownerID, URI activityPubID, Poll poll) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.insertInto("polls")
				.value("ap_id", activityPubID.toString())
				.value("owner_id", ownerID)
				.value("question", poll.question)
				.value("is_anonymous", poll.anonymous)
				.value("end_time", poll.endTime)
				.value("is_multi_choice", poll.multipleChoice)
				.value("num_voted_users",poll.numVoters)
				.createStatement(Statement.RETURN_GENERATED_KEYS);
		int pollID=DatabaseUtils.insertAndGetID(stmt);
		boolean hasIDs=false;
		for(PollOption opt:poll.options){
			if(opt.activityPubID!=null)
				hasIDs=true;
			else if(hasIDs)
				throw new IllegalStateException("all options must either have or not have IDs");
			opt.id=new SQLQueryBuilder(conn)
					.insertInto("poll_options")
					.value("poll_id", pollID)
					.value("ap_id", Objects.toString(opt.activityPubID, null))
					.value("text", opt.text)
					.value("num_votes", opt.numVotes)
					.executeAndGetID();
		}
		return pollID;
	}

	public static void putForeignWallPost(Post post) throws SQLException{
		if(post.isReplyToUnknownPost){
			throw new IllegalArgumentException("This post needs its parent thread to be fetched first");
		}
		String key=post.getActivityPubID().toString().toLowerCase();
		foreignPostUpdateLocks.acquire(key);
		Post existing=getPostByID(post.getActivityPubID());
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			DatabaseUtils.doWithTransaction(conn, ()->{
				PreparedStatement stmt;
				if(existing==null){
					if(post.poll!=null){
						post.poll.id=putForeignPoll(conn, post.ownerID, post.getActivityPubID(), post.poll);
					}

					stmt=new SQLQueryBuilder(conn)
							.insertInto("wall_posts")
							.value("author_id", post.authorID)
							.value("owner_user_id", post.ownerID>0 ? post.ownerID : null)
							.value("owner_group_id", post.ownerID<0 ? -post.ownerID : null)
							.value("text", post.text)
							.value("attachments", post.serializeAttachments())
							.value("content_warning", post.contentWarning)
							.value("ap_url", post.activityPubURL.toString())
							.value("ap_id", post.getActivityPubID().toString())
							.value("reply_key", Utils.serializeIntList(post.replyKey))
							.value("created_at", post.createdAt)
							.value("updated_at", post.updatedAt)
							.value("mentions", Utils.serializeIntList(post.mentionedUserIDs))
							.value("ap_replies", Objects.toString(post.activityPubReplies, null))
							.value("poll_id", post.poll!=null ? post.poll.id : null)
							.value("privacy", post.privacy)
							.value("repost_of", post.repostOf!=0 ? post.repostOf : null)
							.value("flags", Utils.serializeEnumSet(post.flags))
							.value("action", post.action)
							.createStatement(Statement.RETURN_GENERATED_KEYS);
				}else{
					if(post.poll!=null && Objects.equals(post.poll, existing.poll)){ // poll is unchanged, update vote counts
						stmt=new SQLQueryBuilder(conn)
								.update("polls")
								.value("num_voted_users", post.poll.numVoters)
								.where("id=?", existing.poll.id)
								.createStatement();
						stmt.execute();
						post.poll.id=existing.poll.id;

						if(post.poll.options.get(0).activityPubID!=null){ // Match options using IDs
							HashMap<URI, PollOption> optMap=new HashMap<>(post.poll.options.size());
							for(PollOption opt: existing.poll.options){
								optMap.put(opt.activityPubID, opt);
							}
							for(PollOption opt: post.poll.options){
								PollOption existingOpt=optMap.get(opt.activityPubID);
								if(existingOpt==null)
									throw new IllegalStateException("option with id "+opt.activityPubID+" not found in existing poll");
								opt.id=existingOpt.id;
								if(opt.numVotes!=existingOpt.numVotes){
									SQLQueryBuilder.prepareStatement(conn, "UPDATE poll_options SET num_votes=? WHERE id=? AND poll_id=?", opt.numVotes, opt.id, post.poll.id).execute();
								}
							}
						}else{ // Match options using titles
							HashMap<String, PollOption> optMap=new HashMap<>(post.poll.options.size());
							for(PollOption opt: existing.poll.options){
								optMap.put(opt.text, opt);
							}
							for(PollOption opt: post.poll.options){
								PollOption existingOpt=optMap.get(opt.text);
								if(existingOpt==null)
									throw new IllegalStateException("option with name '"+opt.text+"' not found in existing poll");
								opt.id=existingOpt.id;
								if(opt.numVotes!=existingOpt.numVotes){
									SQLQueryBuilder.prepareStatement(conn, "UPDATE poll_options SET num_votes=? WHERE id=? AND poll_id=?", opt.numVotes, opt.id, post.poll.id).execute();
								}
							}
						}
					}else if(post.poll!=null && existing.poll!=null){ // poll changed, delete it and recreate again
						// deletes votes and options because of ON DELETE CASCADE
						new SQLQueryBuilder(conn)
								.deleteFrom("polls")
								.where("id=?", existing.poll.id)
								.executeNoResult();
						post.poll.id=putForeignPoll(conn, post.ownerID, post.getActivityPubID(), post.poll);
					}else if(post.poll!=null){ // poll was added
						post.poll.id=putForeignPoll(conn, post.ownerID, post.getActivityPubID(), post.poll);
					}else if(existing.poll!=null){ // poll was removed
						new SQLQueryBuilder(conn)
								.deleteFrom("polls")
								.where("id=?", existing.poll.id)
								.executeNoResult();
					}
					stmt=new SQLQueryBuilder(conn)
							.update("wall_posts")
							.where("ap_id=?", post.getActivityPubID().toString())
							.value("text", post.text)
							.value("attachments", post.serializeAttachments())
							.value("content_warning", post.contentWarning)
							.value("mentions", Utils.serializeIntList(post.mentionedUserIDs))
							.value("poll_id", post.poll!=null ? post.poll.id : null)
							.createStatement();
				}
				if(existing==null){
					post.id=DatabaseUtils.insertAndGetID(stmt);
					if(post.ownerID==post.authorID && post.getReplyLevel()==0){
						new SQLQueryBuilder(conn)
								.insertInto("newsfeed")
								.value("type", NewsfeedEntry.Type.POST)
								.value("author_id", post.authorID)
								.value("object_id", post.id)
								.value("time", post.createdAt)
								.executeNoResult();
					}
					if(post.getReplyLevel()>0){
						new SQLQueryBuilder(conn)
								.update("wall_posts")
								.valueExpr("reply_count", "reply_count+1")
								.whereIn("id", post.replyKey)
								.executeNoResult();
						BackgroundTaskRunner.getInstance().submit(new UpdateCommentBookmarksRunnable(post.replyKey.getFirst()));
					}
				}else{
					stmt.execute();
					post.id=existing.id;
				}
			});
		}finally{
			foreignPostUpdateLocks.release(key);
		}
	}

	public static List<Post> getWallPosts(int ownerID, boolean isGroup, int minID, int maxID, int offset, int count, int[] total, boolean ownOnly, Set<Post.Privacy> allowedPrivacy) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt;
			String condition=ownOnly ? " AND owner_user_id=author_id" : "";
			String ownerField=isGroup ? "owner_group_id" : "owner_user_id";
			if(allowedPrivacy.size()<Post.Privacy.values().length){
				condition+=" AND privacy IN ("+allowedPrivacy.stream().map(p->String.valueOf(p.ordinal())).collect(Collectors.joining(", "))+")";
			}
			if(total!=null){
				stmt=conn.prepareStatement("SELECT COUNT(*) FROM `wall_posts` WHERE `"+ownerField+"`=? AND `reply_key` IS NULL"+condition);
				stmt.setInt(1, ownerID);
				try(ResultSet res=stmt.executeQuery()){
					res.next();
					total[0]=res.getInt(1);
				}
			}
			if(minID>0){
				stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `"+ownerField+"`=? AND `id`>? AND `reply_key` IS NULL"+condition+" ORDER BY created_at DESC LIMIT "+count);
				stmt.setInt(2, minID);
			}else if(maxID>0){
				stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `"+ownerField+"`=? AND `id`<=? AND `reply_key` IS NULL"+condition+" ORDER BY created_at DESC LIMIT "+offset+","+count);
				stmt.setInt(2, maxID);
			}else{
				stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `"+ownerField+"`=? AND `reply_key` IS NULL"+condition+" ORDER BY created_at DESC LIMIT "+offset+","+count);
			}
			stmt.setInt(1, ownerID);
			ArrayList<Post> posts=new ArrayList<>();
			try(ResultSet res=stmt.executeQuery()){
				while(res.next()){
					posts.add(Post.fromResultSet(res));
				}
			}
			postprocessPosts(posts);
			return posts;
		}
	}

	public static PaginatedList<URI> getWallPostActivityPubIDs(int ownerID, boolean isGroup, int offset, int count, boolean includeAll) throws SQLException{
		String ownerField=isGroup ? "owner_group_id" : "owner_user_id";
		String extraWhere=includeAll ? "" : " AND owner_user_id=author_id";
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){

			int total=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.count()
					.where(ownerField+"=? AND reply_key IS NULL"+extraWhere, ownerID)
					.executeAndGetInt();

			if(total==0)
				return PaginatedList.emptyList(count);

			List<URI> ids=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.columns("id", "ap_id")
					.where(ownerField+"=? AND reply_key IS NULL"+extraWhere, ownerID)
					.orderBy("id ASC")
					.limit(count, offset)
					.executeAsStream(res->{
						String apID=res.getString(2);
						if(StringUtils.isNotEmpty(apID)){
							return URI.create(apID);
						}else{
							return UriBuilder.local().path("posts", String.valueOf(res.getInt(1))).build();
						}
					})
					.toList();
			return new PaginatedList<>(ids, total, offset, count);
		}
	}

	public static PaginatedList<URI> getWallCommentActivityPubIDs(int ownerID, boolean isGroup, int offset, int count, boolean includeAll) throws SQLException{
		String ownerField=isGroup ? "owner_group_id" : "owner_user_id";
		String extraWhere=includeAll ? "" : " AND top_parent_is_wall_to_wall=0";
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){

			int total=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.count()
					.where(ownerField+"=? AND reply_key IS NOT NULL"+extraWhere, ownerID)
					.executeAndGetInt();

			if(total==0)
				return PaginatedList.emptyList(count);

			List<URI> ids=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.columns("id", "ap_id")
					.where(ownerField+"=? AND reply_key IS NOT NULL"+extraWhere, ownerID)
					.orderBy("id ASC")
					.limit(count, offset)
					.executeAsStream(res->{
						String apID=res.getString(2);
						if(StringUtils.isNotEmpty(apID)){
							return URI.create(apID);
						}else{
							return UriBuilder.local().path("posts", String.valueOf(res.getInt(1))).build();
						}
					})
					.toList();
			return new PaginatedList<>(ids, total, offset, count);
		}
	}

	public static PaginatedList<Post> getWallComments(int ownerID, int offset, int count) throws SQLException{
		String ownerField=ownerID<0 ? "owner_group_id" : "owner_user_id";
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.count()
					.where(ownerField+"=? AND reply_key IS NOT NULL", Math.abs(ownerID))
					.executeAndGetInt();

			if(total==0)
				return PaginatedList.emptyList(count);

			List<Post> posts=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.allColumns()
					.where(ownerField+"=? AND reply_key IS NOT NULL", Math.abs(ownerID))
					.orderBy("id ASC")
					.limit(count, offset)
					.executeAsStream(Post::fromResultSet)
					.toList();
			return new PaginatedList<>(posts, total, offset, count);
		}
	}

	public static List<Post> getWallToWall(int userID, int otherUserID, int offset, int count, int[] total) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt;
			if(total!=null){
				stmt=conn.prepareStatement("SELECT COUNT(*) FROM wall_posts WHERE ((owner_user_id=? AND author_id=?) OR (owner_user_id=? AND author_id=?)) AND `reply_key` IS NULL");
				stmt.setInt(1, userID);
				stmt.setInt(2, otherUserID);
				stmt.setInt(3, otherUserID);
				stmt.setInt(4, userID);
				try(ResultSet res=stmt.executeQuery()){
					res.next();
					total[0]=res.getInt(1);
				}
			}
			stmt=conn.prepareStatement("SELECT * FROM wall_posts WHERE ((owner_user_id=? AND author_id=?) OR (owner_user_id=? AND author_id=?)) AND `reply_key` IS NULL ORDER BY created_at DESC LIMIT "+offset+","+count);
			stmt.setInt(1, userID);
			stmt.setInt(2, otherUserID);
			stmt.setInt(3, otherUserID);
			stmt.setInt(4, userID);
			ArrayList<Post> posts=new ArrayList<>();
			try(ResultSet res=stmt.executeQuery()){
				while(res.next()){
					posts.add(Post.fromResultSet(res));
				}
			}
			postprocessPosts(posts);
			return posts;
		}
	}

	public static @NotNull Post getPostOrThrow(int postID, boolean onlyLocal) throws SQLException{
		if(postID<=0)
			throw new ObjectNotFoundException("err_post_not_found");
		Post post=getPostByID(postID, false);
		if(post==null || (onlyLocal && !post.isLocal()))
			throw new ObjectNotFoundException("err_post_not_found");
		return post;
	}

	public static Post getPostByID(int postID, boolean wantDeleted) throws SQLException{
		Post post=new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.allColumns()
				.where("id=?", postID)
				.executeAndGetSingleObject(Post::fromResultSet);
		if(post==null || (post.isDeleted() && !wantDeleted))
			return null;
		postprocessPosts(Set.of(post));
		return post;
	}

	public static Map<Integer, Post> getPostsByID(Collection<Integer> ids) throws SQLException{
		if(ids.isEmpty())
			return Map.of();
		Map<Integer, Post> posts=new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.allColumns()
				.whereIn("id", ids)
				.executeAsStream(Post::fromResultSet)
				.filter(p->!p.isDeleted())
				.collect(Collectors.toMap(p->p.id, Function.identity()));
		postprocessPosts(posts.values());
		return posts;
	}

	public static Post getPostByID(URI apID) throws SQLException{
		if(Config.isLocal(apID)){
			String[] pathParts=apID.getPath().split("/");
			String posts=pathParts[1];
			int postID=Utils.parseIntOrDefault(pathParts[2], 0);
			if(!"posts".equals(posts) || postID==0){
				throw new ObjectNotFoundException("Invalid local URL "+apID);
			}
			return getPostByID(postID, false);
		}
		Post post=new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.allColumns()
				.where("ap_id=?", apID)
				.executeAndGetSingleObject(Post::fromResultSet);
		if(post!=null)
			postprocessPosts(Set.of(post));
		return post;
	}

	public static Map<Integer, int[]> getPostOwnerAndAuthorIDs(Collection<Integer> postIDs) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.columns("id", "owner_user_id", "owner_group_id", "author_id")
				.whereIn("id", postIDs)
				.executeAsStream(r->{
					int uid=r.getInt("owner_user_id");
					return new Pair<>(r.getInt("id"), new int[]{uid>0 ? uid : -r.getInt("owner_group_id"), r.getInt("author_id")});
				})
				.collect(Collectors.toMap(Pair::first, Pair::second));
	}

	public static int getLocalIDByActivityPubID(URI apID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.columns("id")
				.where("ap_id=?", apID)
				.executeAndGetInt();
	}

	public static void deletePost(int id) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			Post post=getPostByID(id, false);
			if(post==null)
				return;
			PreparedStatement stmt;
			boolean needFullyDelete=true;
			if(post.getReplyLevel()>0){
				stmt=conn.prepareStatement("SELECT COUNT(*) FROM wall_posts WHERE reply_key LIKE BINARY bin_prefix(?)");
				ArrayList<Integer> rk=new ArrayList<>(post.replyKey);
				rk.add(post.id);
				stmt.setBytes(1, Utils.serializeIntList(rk));
				try(ResultSet res=stmt.executeQuery()){
					res.next();
					needFullyDelete=res.getInt(1)==0;
				}
			}

			if(post.poll!=null && post.poll.ownerID==post.authorID){
				SQLQueryBuilder.prepareStatement(conn, "DELETE FROM polls WHERE id=?", post.poll.id).execute();
			}

			// Delete Mastodon-style reposts as well
			Set<Integer> reposts=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.columns("id")
					.where("repost_of=? AND (flags & 1)=1", id)
					.executeAndGetIntStream()
					.boxed()
					.collect(Collectors.toSet());
			if(!reposts.isEmpty()){
				new SQLQueryBuilder(conn)
						.deleteFrom("newsfeed")
						.whereIn("object_id", reposts)
						.andWhere("`type`=?", NewsfeedEntry.Type.POST)
						.executeNoResult();
				new SQLQueryBuilder(conn)
						.deleteFrom("wall_posts")
						.whereIn("id", reposts)
						.executeNoResult();
			}

			if(needFullyDelete){
				stmt=conn.prepareStatement("DELETE FROM `wall_posts` WHERE `id`=?");
				stmt.setInt(1, id);
				stmt.execute();
			}else{
				// (comments don't exist in the feed anyway)
				stmt=conn.prepareStatement("UPDATE wall_posts SET author_id=NULL, owner_user_id=NULL, owner_group_id=NULL, text=NULL, attachments=NULL, content_warning=NULL, updated_at=NULL, mentions=NULL, source=NULL WHERE id=?");
				stmt.setInt(1, id);
				stmt.execute();
			}
			stmt=conn.prepareStatement("DELETE FROM `newsfeed` WHERE (`type`=0 OR `type`=1) AND `object_id`=?");
			stmt.setInt(1, id);
			stmt.execute();
			new SQLQueryBuilder(conn)
					.deleteFrom("newsfeed_groups")
					.where("type=? AND object_id=?", NewsfeedEntry.Type.POST, id)
					.executeNoResult();

			if(post.getReplyLevel()>0){
				conn.createStatement().execute("UPDATE wall_posts SET reply_count=GREATEST(1, reply_count)-1 WHERE id IN ("+post.replyKey.stream().map(String::valueOf).collect(Collectors.joining(","))+")");
				BackgroundTaskRunner.getInstance().submit(new UpdateCommentBookmarksRunnable(post.replyKey.get(0)));
			}else{
				BackgroundTaskRunner.getInstance().submit(new DeleteCommentBookmarksRunnable(id));
			}
		}
	}

	public static Map<Integer, PaginatedList<Post>> getRepliesForFeed(Set<List<Integer>> postReplyKeys, boolean flat) throws SQLException{
		if(postReplyKeys.isEmpty())
			return Collections.emptyMap();
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=conn.prepareStatement(String.join(" UNION ALL ", Collections.nCopies(postReplyKeys.size(), "(SELECT *, ? AS `parent_id` FROM wall_posts WHERE reply_key"+(flat ? " LIKE BINARY bin_prefix(?)" : "=?")+" ORDER BY created_at DESC LIMIT 3)")));
			int i=0;
			for(List<Integer> id:postReplyKeys){
				stmt.setInt(i+1, id.getLast());
				stmt.setBytes(i+2, Utils.serializeIntList(id));
				i+=2;
			}
			LOG.debug("{}", stmt);
			HashMap<Integer, PaginatedList<Post>> map=new HashMap<>();
			try(ResultSet res=stmt.executeQuery()){
				while(res.next()){
					Post post=Post.fromResultSet(res);
					int parentID=res.getInt("parent_id");
					List<Post> posts=map.computeIfAbsent(parentID, (k)->new PaginatedList<>(new ArrayList<>(), 0)).list;
					posts.addFirst(post);
				}
			}
			postprocessPosts(map.values().stream().flatMap(l->l.list.stream()).toList());
			stmt=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.selectExpr("count(*), reply_key")
					.groupBy("reply_key")
					.whereIn("reply_key", postReplyKeys.stream().map(Utils::serializeIntList).collect(Collectors.toList()))
					.createStatement();
			try(ResultSet res=stmt.executeQuery()){
				while(res.next()){
					int id=Utils.deserializeIntList(res.getBytes(2)).getLast();
					map.get(id).total=res.getInt(1);
				}
			}
			return map;
		}
	}

	public static ThreadedReplies getRepliesThreaded(List<Integer> prefix, int topLevelOffset, int topLevelLimit, int secondaryLimit, boolean twoLevel, boolean reversed) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){

			byte[] serializedPrefix=Utils.serializeIntList(prefix);

			int total=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.count()
					.where("reply_key=?", (Object) serializedPrefix)
					.executeAndGetInt();

			if(total==0)
				return new ThreadedReplies(List.of(), List.of(), 0);

			List<Post> posts=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.allColumns()
					.where("reply_key=?", (Object) serializedPrefix)
					.limit(topLevelLimit, topLevelOffset)
					.orderBy("created_at "+(reversed ? "DESC" : "ASC"))
					.executeAsStream(Post::fromResultSet)
					.collect(Collectors.toList());
			postprocessPosts(posts);

			ArrayList<String> wheres=new ArrayList<>();
			ArrayList<Object> whereArgs=new ArrayList<>();
			for(Post post:posts){
				if(post.replyCount>0){
					wheres.add("reply_key LIKE BINARY bin_prefix(?)");
					whereArgs.add(Utils.serializeIntList(post.getReplyKeyForReplies()));
				}
			}

			List<Post> replies;
			if(!whereArgs.isEmpty()){
				whereArgs.add(secondaryLimit);
				PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT * FROM wall_posts WHERE "+String.join(" OR ", wheres)+" ORDER BY created_at ASC"+(twoLevel ? "" : ", LENGTH(reply_key) ASC")+" LIMIT ?",
						whereArgs.toArray());
				replies=DatabaseUtils.resultSetToObjectStream(stmt.executeQuery(), Post::fromResultSet, null).toList();
				postprocessPosts(replies);
			}else{
				replies=List.of();
			}

			return new ThreadedReplies(posts, replies, total);
		}
	}

	public static PaginatedList<Post> getRepliesFlat(List<Integer> prefix, int offset, int limit, boolean reversed) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			byte[] serializedPrefix=Utils.serializeIntList(prefix);

			int total=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.count()
					.where("reply_key LIKE BINARY bin_prefix(?) AND author_id IS NOT NULL", (Object) serializedPrefix)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(limit);

			List<Post> list=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.allColumns()
					.where("reply_key LIKE BINARY bin_prefix(?) AND author_id IS NOT NULL", (Object) serializedPrefix)
					.limit(limit, offset)
					.orderBy("created_at "+(reversed ? "DESC" : "ASC"))
					.executeAsStream(Post::fromResultSet)
					.toList();
			postprocessPosts(list);
			return new PaginatedList<>(list, total, offset, limit);
		}
	}

	public static PaginatedList<Post> getRepliesFlatWithMaxID(List<Integer> prefix, int maxID, int limit) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			byte[] serializedPrefix=Utils.serializeIntList(prefix);

			int total=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.count()
					.where("reply_key LIKE BINARY bin_prefix(?) AND created_at<=(SELECT `created_at` FROM `wall_posts` WHERE id=?) AND id<>?", (Object) serializedPrefix, maxID, maxID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(limit);

			List<Post> list=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.allColumns()
					.where("reply_key LIKE BINARY bin_prefix(?) AND created_at<=(SELECT `created_at` FROM `wall_posts` WHERE id=?) AND id<>?", (Object) serializedPrefix, maxID, maxID)
					.limit(limit, 0)
					.orderBy("created_at DESC")
					.executeAsStream(Post::fromResultSet)
					.toList()
					.reversed();
			postprocessPosts(list);
			return new PaginatedList<>(list, total, 0, limit);
		}
	}

	public static PaginatedList<Post> getRepliesExact(int[] replyKey, int maxID, int limit) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.count()
					.where("reply_key=? AND created_at<=(SELECT `created_at` FROM `wall_posts` WHERE id=?) AND id<>?", Utils.serializeIntArray(replyKey), maxID, maxID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(limit);

			List<Post> posts=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.allColumns()
					.where("reply_key=? AND created_at<=(SELECT `created_at` FROM `wall_posts` WHERE id=?) AND id<>?", Utils.serializeIntArray(replyKey), maxID, maxID)
					.limit(limit, 0)
					.orderBy("created_at ASC")
					.executeAsStream(Post::fromResultSet)
					.toList();
			postprocessPosts(posts);
			return new PaginatedList<>(posts, total, 0, limit);
		}
	}

	public static URI getActivityPubID(int postID) throws SQLException{
		String apID=new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.columns("ap_id")
				.where("id=?", postID)
				.executeAndGetSingleObject(r->r.getString(1));
		return apID==null ? Config.localURI("/posts/"+postID) : URI.create(apID);
	}

	public static int getLocalPostCount(boolean comments) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.count()
				.where("ap_id IS NULL AND reply_key IS "+(comments ? "NOT " : "")+"NULL")
				.executeAndGetInt();
	}

	public static HashMap<Integer, UserInteractions> getPostInteractions(Collection<Integer> postIDs, int userID) throws SQLException{
		HashMap<Integer, UserInteractions> result=new HashMap<>();
		if(postIDs.isEmpty())
			return result;
		for(int id:postIDs)
			result.put(id, new UserInteractions());
		String idsStr=postIDs.stream().map(Object::toString).collect(Collectors.joining(","));

		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			try(ResultSet res=conn.createStatement().executeQuery("SELECT object_id, COUNT(*) FROM likes WHERE object_type=0 AND object_id IN ("+idsStr+") GROUP BY object_id")){
				if(res.next()){
					do{
						result.get(res.getInt(1)).likeCount=res.getInt(2);
					}while(res.next());
				}
			}
			if(userID!=0){
				PreparedStatement stmt=conn.prepareStatement("SELECT object_id FROM likes WHERE object_type=0 AND object_id IN ("+idsStr+") AND user_id=?");
				stmt.setInt(1, userID);
				try(ResultSet res=stmt.executeQuery()){
					while(res.next()){
						result.get(res.getInt(1)).isLiked=true;
					}
				}
			}

			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT option_id FROM poll_votes WHERE user_id=? AND poll_id=?", userID);
			try(ResultSet res=conn.createStatement().executeQuery("SELECT id, reply_count, poll_id FROM wall_posts WHERE id IN ("+idsStr+")")){
				while(res.next()){
					UserInteractions interactions=result.get(res.getInt(1));
					interactions.commentCount=res.getInt(2);
					if(userID!=0){
						int pollID=res.getInt(3);
						if(!res.wasNull()){
							stmt.setInt(2, pollID);
							try(ResultSet res2=stmt.executeQuery()){
								interactions.pollChoices=new ArrayList<>();
								while(res2.next()){
									interactions.pollChoices.add(res2.getInt(1));
								}
							}
						}
					}
				}
			}

			new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.selectExpr("repost_of, COUNT(*)")
					.whereIn("repost_of", postIDs)
					.groupBy("repost_of")
					.executeAsStream(res->new Pair<>(res.getInt(1), res.getInt(2)))
					.forEach(count->result.get(count.first()).repostCount=count.second());
		}

		return result;
	}

	public static Set<URI> getInboxesForPostInteractionForwarding(Post post) throws SQLException{
		// Interaction on a top-level post:
		// - local: send to everyone who replied + reposted + the post's original addressees (followers + mentions if any)
		// - remote: send to the owner server only. It forwards as it pleases.
		// On a comment: do all of the above for the parent top-level post, and
		// - local: send to any mentioned users
		// - remote: send to the owner server, if not sent already if the parent post is local
		User author=UserStorage.getById(post.authorID);
		Actor owner;
		if(post.ownerID<0)
			owner=GroupStorage.getById(-post.ownerID);
		else
			owner=UserStorage.getById(post.ownerID);
		HashSet<URI> inboxes=new HashSet<>();
		Post origPost=post;
		if(post.getReplyLevel()>0){
			post=getPostByID(post.replyKey.get(0), false);
			if(post==null)
				return Set.of();
		}
		if(author instanceof ForeignUser fu && origPost.getReplyLevel()==0){
			return Set.of(fu.inbox);
		}
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			ArrayList<String> queryParts=new ArrayList<>();
			if(post.isLocal()){
				queryParts.add("SELECT author_id FROM wall_posts WHERE reply_key LIKE BINARY bin_prefix(?)");
				queryParts.add("SELECT author_id FROM wall_posts WHERE repost_of="+post.id);
				if(owner instanceof ForeignUser fu)
					queryParts.add("SELECT "+fu.id);
				else if(owner instanceof User u)
					queryParts.add("SELECT follower_id FROM followings WHERE followee_id="+u.id);
				else if(owner instanceof ForeignGroup fg)
					inboxes.add(Objects.requireNonNullElse(fg.sharedInbox, fg.inbox));
				else if(owner instanceof Group g)
					queryParts.add("SELECT user_id FROM group_memberships WHERE group_id="+g.id);

				if(!post.mentionedUserIDs.isEmpty()){
					for(int user:post.mentionedUserIDs){
						queryParts.add("SELECT "+user);
					}
				}
			}else{
				queryParts.add("SELECT "+post.authorID);
			}
			if(origPost!=post){
				if(origPost.isLocal()){
					if(!origPost.mentionedUserIDs.isEmpty()){
						for(int user:origPost.mentionedUserIDs){
							queryParts.add("SELECT "+user);
						}
					}
				}else{
					queryParts.add("SELECT "+origPost.authorID);
				}
			}
			PreparedStatement stmt=conn.prepareStatement("SELECT DISTINCT IFNULL(ap_shared_inbox, ap_inbox) FROM users WHERE id IN ("+
					String.join(" UNION ", queryParts)+
					") AND ap_inbox IS NOT NULL");
			if(post.isLocal())
				stmt.setBytes(1, Utils.serializeIntList(post.getReplyKeyForReplies()));
			try(ResultSet res=stmt.executeQuery()){
				while(res.next()){
					URI uri=URI.create(res.getString(1));
					inboxes.add(uri);
				}
			}
		}

		return inboxes;
	}

	public static List<URI> getImmediateReplyActivityPubIDs(List<Integer> replyKey, int offset, int count, int[] total) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			byte[] serializedKey=Utils.serializeIntList(replyKey);
			PreparedStatement stmt=conn.prepareStatement("SELECT count(*) FROM wall_posts WHERE reply_key=?");
			stmt.setBytes(1, serializedKey);
			try(ResultSet res=stmt.executeQuery()){
				res.next();
				total[0]=res.getInt(1);
			}
			stmt=conn.prepareStatement("SELECT ap_id, id FROM wall_posts WHERE reply_key=? ORDER BY created_at ASC LIMIT ?,?");
			stmt.setBytes(1, serializedKey);
			stmt.setInt(2, offset);
			stmt.setInt(3, count);
			try(ResultSet res=stmt.executeQuery()){
				ArrayList<URI> replies=new ArrayList<>();
				while(res.next()){
					String apID=res.getString(1);
					if(apID!=null)
						replies.add(URI.create(apID));
					else
						replies.add(Config.localURI("/posts/"+res.getInt(2)));
				}
				return replies;
			}
		}
	}

	public static Poll getPoll(int id, URI parentApID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			Poll poll=new SQLQueryBuilder(conn)
					.selectFrom("polls")
					.allColumns()
					.where("id=?", id)
					.executeAndGetSingleObject(Poll::fromResultSet);
			if(poll==null)
				return null;
			new SQLQueryBuilder(conn)
					.selectFrom("poll_options")
					.where("poll_id=?", id)
					.executeAsStream(res->PollOption.fromResultSet(res, poll.activityPubID==null ? parentApID : poll.activityPubID, poll))
					.forEach(poll.options::add);
			return poll;
		}
	}

	public static int[] voteInPoll(int userID, int pollID, int[] optionIDs) throws SQLException{
		String key=userID+"|"+pollID;
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			pollVoteLocks.acquire(key);
			int count=new SQLQueryBuilder(conn)
					.selectFrom("poll_votes")
					.count()
					.where("user_id=? AND poll_id=?", userID, pollID)
					.executeAndGetInt();
			if(count>0)
				return null;

			PreparedStatement stmt1=new SQLQueryBuilder(conn)
					.insertInto("poll_votes")
					.value("option_id", 0)
					.value("user_id", userID)
					.value("poll_id", pollID)
					.createStatement(Statement.RETURN_GENERATED_KEYS);

			PreparedStatement stmt2=new SQLQueryBuilder(conn)
					.update("poll_options")
					.where("id=?", 0)
					.valueExpr("num_votes", "num_votes+1")
					.createStatement();

			int[] voteIDs=new int[optionIDs.length];
			int i=0;
			for(int optID: optionIDs){
				stmt1.setInt(1, optID);
				stmt1.execute();
				stmt2.setInt(1, optID);
				stmt2.execute();
				try(ResultSet res=stmt1.getGeneratedKeys()){
					res.next();
					voteIDs[i++]=res.getInt(1);
				}
			}
			stmt1.close();
			stmt2.close();

			new SQLQueryBuilder(conn)
					.update("polls")
					.valueExpr("num_voted_users", "num_voted_users+1")
					.valueExpr("last_vote_time", "CURRENT_TIMESTAMP()")
					.where("id=?", pollID)
					.executeNoResult();

			return voteIDs;
		}finally{
			pollVoteLocks.release(key);
		}
	}

	// This is called once for each choice in a multiple-choice poll
	public static int voteInPoll(int userID, int pollID, int optionID, URI voteID, boolean allowMultiple) throws SQLException{
		String key=userID+"|"+pollID;
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			pollVoteLocks.acquire(key);
			PreparedStatement stmt=new SQLQueryBuilder(conn)
					.selectFrom("poll_votes")
					.columns("option_id")
					.where("user_id=? AND poll_id=?", userID, pollID)
					.createStatement();
			boolean userVoted=false;
			try(ResultSet res=stmt.executeQuery()){
				while(res.next()){
					// this is a single-choice poll and there's already a vote
					if(!allowMultiple)
						return 0;
					userVoted=true;
					int optID=res.getInt(1);
					if(optID==optionID)
						return 0;
				}
			}

			int rVoteID=new SQLQueryBuilder(conn)
					.insertInto("poll_votes")
					.value("option_id", optionID)
					.value("user_id", userID)
					.value("poll_id", pollID)
					.value("ap_id", voteID)
					.executeAndGetID();

			new SQLQueryBuilder(conn)
					.update("poll_options")
					.where("id=?", optionID)
					.valueExpr("num_votes", "num_votes+1")
					.executeNoResult();

			if(!userVoted){
				new SQLQueryBuilder(conn)
						.update("polls")
						.valueExpr("num_voted_users", "num_voted_users+1")
						.where("id=?", pollID)
						.executeNoResult();
			}

			return rVoteID;
		}finally{
			pollVoteLocks.release(key);
		}
	}

	public static int createPoll(int ownerID, @NotNull String question, @NotNull List<String> options, boolean anonymous, boolean multiChoice, @Nullable Instant endTime) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int pollID=new SQLQueryBuilder(conn)
					.insertInto("polls")
					.value("owner_id", ownerID)
					.value("question", question)
					.value("is_anonymous", anonymous)
					.value("is_multi_choice", multiChoice)
					.value("end_time", endTime)
					.executeAndGetID();

			for(String opt:options){
				new SQLQueryBuilder(conn)
						.insertInto("poll_options")
						.value("text", opt)
						.value("poll_id", pollID)
						.executeNoResult();
			}

			return pollID;
		}
	}

	public static void deletePoll(int pollID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("polls")
				.where("id=?", pollID)
				.executeNoResult();
	}

	public static List<Integer> getPollOptionVoters(int optionID, int offset, int count) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("poll_votes")
				.columns("user_id")
				.where("option_id=?", optionID)
				.orderBy("id ASC")
				.limit(count, offset)
				.executeAndGetIntList();
	}

	public static List<URI> getPollOptionVotersApIDs(int optionID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn,
					"SELECT users.id, users.ap_id FROM poll_votes JOIN users ON poll_votes.user_id=users.id WHERE poll_votes.option_id=? LIMIT ? OFFSET ?", optionID, count, offset);
			try(ResultSet res=stmt.executeQuery()){
				ArrayList<URI> r=new ArrayList<>();
				while(res.next()){
					String apID=res.getString(2);
					r.add(apID!=null ? URI.create(apID) : Config.localURI("/users/"+res.getInt(1)));
				}
				return r;
			}
		}
	}

	public static void setPostFederationState(int postID, FederationState state) throws SQLException{
		new SQLQueryBuilder()
				.update("wall_posts")
				.value("federation_state", state)
				.where("id=?", postID)
				.executeNoResult();
	}

	public static PaginatedList<NewsfeedEntry> getCommentsFeed(int userID, int offset, int count, EnumSet<CommentsNewsfeedObjectType> filter) throws SQLException{
		if(filter.isEmpty())
			return PaginatedList.emptyList(count);
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("newsfeed_comments")
					.count()
					.whereIn("object_type", filter)
					.andWhere("user_id=?", userID)
					.executeAndGetInt();

			if(total==0)
				return new PaginatedList<>(Collections.emptyList(), 0);

			List<NewsfeedEntry> entries=new SQLQueryBuilder(conn)
					.selectFrom("newsfeed_comments")
					.columns("object_type", "object_id")
					.whereIn("object_type", filter)
					.andWhere("user_id=?", userID)
					.orderBy("last_comment_time DESC")
					.limit(count, offset)
					.executeAsStream(res->{
						CommentsNewsfeedObjectType type=CommentsNewsfeedObjectType.values()[res.getInt(1)];
						NewsfeedEntry entry=new NewsfeedEntry();
						entry.objectID=res.getLong(2);
						entry.type=switch(type){
							case POST -> NewsfeedEntry.Type.POST;
							case PHOTO -> NewsfeedEntry.Type.PHOTO;
							case BOARD_TOPIC -> NewsfeedEntry.Type.BOARD_TOPIC;
						};
						return entry;
					}).toList();

			return new PaginatedList<>(entries, total, offset, count);
		}
	}

	public static Map<URI, Integer> getPostLocalIDsByActivityPubIDs(Collection<URI> ids, int ownerUserID, int ownerGroupID, boolean comments) throws SQLException{
		if(ids.isEmpty())
			return Map.of();

		String replyKeyCondition=comments ? "reply_key IS NOT NULL" : "reply_key IS NULL";

		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){

			List<Integer> localIDs=ids.stream().filter(Config::isLocal).map(u->{
				String path=u.getPath();
				if(StringUtils.isEmpty(path))
					return 0;
				String[] parts=path.split("/"); // "", "posts", id
				return parts.length==3 && "posts".equals(parts[1]) ? Utils.safeParseInt(parts[2]) : 0;
			}).filter(i->i>0).toList();
			List<String> remoteIDs=ids.stream().filter(Predicate.not(Config::isLocal)).map(Object::toString).toList();

			Map<URI, Integer> result=new HashMap<>();
			record IdPair(URI apID, int localID){
			}

			if(!remoteIDs.isEmpty()){
				SQLQueryBuilder builder=new SQLQueryBuilder(conn)
						.selectFrom("wall_posts")
						.columns("id", "ap_id")
						.whereIn("ap_id", remoteIDs)
						.andWhere(replyKeyCondition);

				if(ownerUserID>0 && ownerGroupID==0){
					builder.andWhere("owner_user_id=?", ownerUserID).andWhere("owner_group_id IS NULL");
				}else if(ownerGroupID>0 && ownerUserID==0){
					builder.andWhere("owner_group_id=?", ownerGroupID).andWhere("owner_user_id IS NULL");
				}else{
					throw new IllegalArgumentException("either ownerUserID or ownerGroupID must be >0");
				}
				result.putAll(builder.executeAsStream(rs->new IdPair(URI.create(rs.getString(2)), rs.getInt(1))).collect(Collectors.toMap(IdPair::apID, IdPair::localID)));
			}
			if(!localIDs.isEmpty()){
				SQLQueryBuilder builder=new SQLQueryBuilder(conn)
						.selectFrom("wall_posts")
						.columns("id")
						.whereIn("id", localIDs)
						.andWhere(replyKeyCondition);

				if(ownerUserID>0 && ownerGroupID==0){
					builder.andWhere("owner_user_id=?", ownerUserID).andWhere("owner_group_id IS NULL");
				}else if(ownerGroupID>0 && ownerUserID==0){
					builder.andWhere("owner_group_id=?", ownerGroupID).andWhere("owner_user_id IS NULL");
				}else{
					throw new IllegalArgumentException("either ownerUserID or ownerGroupID must be >0");
				}
				result.putAll(builder.executeAsStream(rs->new IdPair(Config.localURI("/posts/"+rs.getInt(1)), rs.getInt(1))).collect(Collectors.toMap(IdPair::apID, IdPair::localID)));
			}
			return result;
		}
	}

	public static int getPostIdByPollId(int pollID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.columns("id")
				.where("poll_id=?", pollID)
				.executeAndGetInt();
	}

	public static PostSource getPostSource(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.columns("source", "source_format")
				.where("id=?", id)
				.executeAndGetSingleObject(r->new PostSource(r.getString(1), FormattedTextFormat.values()[r.getInt(2)]));
	}

	public static int getUserPostCount(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.count()
				.where("owner_user_id=? AND author_id=owner_user_id AND reply_key IS NULL", id)
				.executeAndGetInt();
	}

	public static int getUserPostCommentCount(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.count()
				.where("author_id=? AND reply_key IS NOT NULL", id)
				.executeAndGetInt();
	}

	private static void postprocessPosts(Collection<Post> posts) throws SQLException{
		Set<Long> needFileIDs=posts.stream()
				.filter(p->p.attachments!=null && !p.attachments.isEmpty())
				.flatMap(p->p.attachments.stream())
				.map(att->att instanceof LocalImage li ? li.fileID : 0L)
				.filter(id->id!=0)
				.collect(Collectors.toSet());
		if(needFileIDs.isEmpty())
			return;
		Map<Long, MediaFileRecord> mediaFiles=MediaStorage.getMediaFileRecords(needFileIDs);
		for(Post post:posts){
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

	public static List<Integer> getRepostedUsers(int postID, int count) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.columns("author_id")
				.where("repost_of=?", postID)
				.orderBy("MAX(created_at) DESC")
				.groupBy("author_id")
				.limit(count, 0)
				.executeAndGetIntList();
	}

	public static PaginatedList<Post> getPostReposts(int postID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.count()
					.where("repost_of=?", postID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Post> posts=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.allColumns()
					.where("repost_of=?", postID)
					.orderBy("created_at DESC")
					.limit(count, offset)
					.executeAsStream(Post::fromResultSet)
					.toList();
			return new PaginatedList<>(posts, total, offset, count);
		}
	}

	public static Map<Integer, Integer> getPostAuthors(Collection<Integer> ids) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.columns("id", "author_id")
				.whereIn("id", ids)
				.executeAsStream(res->new Pair<Integer, Integer>(res.getInt("id"), res.getInt("author_id")))
				.collect(Collectors.toMap(Pair::first, Pair::second));
	}

	public static List<Post> getUserReplies(int userID, Collection<List<Integer>> replyKeys) throws SQLException{
		List<Post> posts=new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.allColumns()
				.whereIn("reply_key", replyKeys.stream().map(Utils::serializeIntList).toList())
				.andWhere("author_id=?", userID)
				.executeAsStream(Post::fromResultSet)
				.toList();
		postprocessPosts(posts);
		return posts;
	}

	public static PaginatedList<Post> getAllPostsByAuthor(int userID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.count()
					.where("author_id=?", userID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);

			List<Post> posts=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.where("author_id=?", userID)
					.orderBy("created_at DESC")
					.limit(count, offset)
					.executeAsStream(Post::fromResultSet)
					.toList();
			postprocessPosts(posts);
			return new PaginatedList<>(posts, total, offset, count);
		}
	}

	public static Map<Integer, URI> getActivityPubIDsByLocalIDs(Collection<Integer> ids) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.columns("id", "ap_id")
				.whereIn("id", ids)
				.executeAsStream(res->new Pair<>(res.getInt("id"), res.getString("ap_id")))
				.collect(Collectors.toMap(Pair::first, p->{
					String apID=p.second();
					if(apID==null)
						return Config.localURI("/posts/"+p.first());
					return URI.create(apID);
				}));
	}

	// region Pinned posts

	public static void pinPost(int ownerID, int postID, boolean keepPrevious) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int order;
			if(!keepPrevious){
				new SQLQueryBuilder(conn)
						.deleteFrom("wall_pinned_posts")
						.where("owner_user_id=?", ownerID)
						.executeNoResult();
				order=0;
			}else{
				order=new SQLQueryBuilder(conn)
						.selectFrom("wall_pinned_posts")
						.selectExpr("MAX(display_order)")
						.where("owner_user_id=?", ownerID)
						.executeAndGetInt()+1;
			}
			new SQLQueryBuilder(conn)
					.insertIgnoreInto("wall_pinned_posts")
					.value("owner_user_id", ownerID)
					.value("post_id", postID)
					.value("display_order", order)
					.executeNoResult();

			if(!keepPrevious){
				int count=new SQLQueryBuilder(conn)
						.selectFrom("wall_pinned_posts")
						.count()
						.where("owner_user_id=?", ownerID)
						.executeAndGetInt();
				if(count>WallController.MAX_PINNED_POSTS){
					Set<Integer> toDelete=new SQLQueryBuilder(conn)
							.selectFrom("wall_pinned_posts")
							.columns("post_id")
							.where("owner_user_id=?", ownerID)
							.limit(count-WallController.MAX_PINNED_POSTS, 0)
							.orderBy("display_order ASC")
							.executeAndGetIntStream()
							.boxed()
							.collect(Collectors.toSet());
					new SQLQueryBuilder(conn)
							.deleteFrom("wall_pinned_posts")
							.whereIn("post_id", toDelete)
							.andWhere("owner_user_id=?", ownerID)
							.executeNoResult();
				}
			}
		}
	}

	public static void unpinPost(int ownerID, int postID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("wall_pinned_posts")
				.where("owner_user_id=? AND post_id=?", ownerID, postID)
				.executeNoResult();
	}

	public static List<Post> getPinnedPosts(int ownerID) throws SQLException{
		List<Post> posts=new SQLQueryBuilder()
				.selectFrom("wall_pinned_posts")
				.selectExpr("wall_posts.*")
				.join("JOIN wall_posts ON wall_pinned_posts.post_id=wall_posts.id")
				.where("wall_pinned_posts.owner_user_id=?", ownerID)
				.orderBy("wall_pinned_posts.display_order DESC")
				.executeAsStream(Post::fromResultSet)
				.toList();
		postprocessPosts(posts);
		return posts;
	}

	public static boolean isPostPinned(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("wall_pinned_posts")
				.count()
				.where("post_id=?", id)
				.executeAndGetInt()>0;
	}

	public static void clearPinnedPosts(int userID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("wall_pinned_posts")
				.where("owner_user_id=?", userID)
				.executeNoResult();
	}

	// endregion

	private record DeleteCommentBookmarksRunnable(int postID) implements Runnable{
		@Override
		public void run(){
			try{
				new SQLQueryBuilder()
						.deleteFrom("newsfeed_comments")
						.where("object_type=? AND object_id=?", 0, postID)
						.executeNoResult();
			}catch(SQLException x){
				LOG.warn("Error deleting comment bookmarks for post {}", postID, x);
			}
		}
	}

	private record UpdateCommentBookmarksRunnable(int postID) implements Runnable{
		@Override
		public void run(){
			try{
				try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
					PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT MAX(created_at) FROM wall_posts WHERE reply_key LIKE BINARY bin_prefix(?)", (Object) Utils.serializeIntArray(new int[]{postID}));
					Timestamp ts;
					try(ResultSet res=stmt.executeQuery()){
						res.next();
						ts=res.getTimestamp(1);
					}
					if(ts==null){
						new SQLQueryBuilder(conn)
								.deleteFrom("newsfeed_comments")
								.where("object_type=? AND object_id=?", CommentsNewsfeedObjectType.POST, postID)
								.executeNoResult();
					}else{
						new SQLQueryBuilder(conn)
								.update("newsfeed_comments")
								.value("last_comment_time", ts)
								.where("object_type=? AND object_id=?", CommentsNewsfeedObjectType.POST, postID)
								.executeNoResult();
					}
				}
			}catch(SQLException x){
				LOG.warn("Error updating comment bookmarks for post {}", postID, x);
			}
		}
	}

	public record ThreadedReplies(List<Post> posts, List<Post> replies, int total){}
}
