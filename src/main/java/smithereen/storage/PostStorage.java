package smithereen.storage;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.activitypub.objects.activities.Like;
import smithereen.data.FederationState;
import smithereen.data.PaginatedList;
import smithereen.data.Poll;
import smithereen.data.PollOption;
import smithereen.data.UriBuilder;
import smithereen.data.feed.AddFriendNewsfeedEntry;
import smithereen.data.feed.JoinEventNewsfeedEntry;
import smithereen.data.feed.JoinGroupNewsfeedEntry;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.Utils;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.User;
import smithereen.data.UserInteractions;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.data.Post;
import smithereen.data.feed.PostNewsfeedEntry;
import smithereen.data.feed.RetootNewsfeedEntry;
import smithereen.util.BackgroundTaskRunner;
import spark.utils.StringUtils;

public class PostStorage{
	private static final Logger LOG=LoggerFactory.getLogger(PostStorage.class);

	public static int createWallPost(int userID, int ownerUserID, int ownerGroupID, String text, String textSource, int[] replyKey, List<User> mentionedUsers, String attachments, String contentWarning, int pollID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		if(ownerUserID<=0 && ownerGroupID<=0)
			throw new IllegalArgumentException("Need either ownerUserID or ownerGroupID");

		int id=new SQLQueryBuilder(conn)
				.insertInto("wall_posts")
				.value("author_id", userID)
				.value("owner_user_id", ownerUserID>0 ? ownerUserID : null)
				.value("owner_group_id", ownerGroupID>0 ? ownerGroupID : null)
				.value("text", text)
				.value("reply_key", Utils.serializeIntArray(replyKey))
				.value("mentions", mentionedUsers.isEmpty() ? null : Utils.serializeIntArray(mentionedUsers.stream().mapToInt(u->u.id).toArray()))
				.value("attachments", attachments)
				.value("content_warning", contentWarning)
				.value("poll_id", pollID>0 ? pollID : null)
				.value("source", textSource)
				.value("source_format", 0)
				.executeAndGetID();

		if(userID==ownerUserID && replyKey==null){
			 new SQLQueryBuilder(conn)
					 .insertInto("newsfeed")
					 .value("type", NewsfeedEntry.Type.POST)
					 .value("author_id", userID)
					 .value("object_id", id)
					 .executeNoResult();
		}
		if(replyKey!=null && replyKey.length>0){
			 new SQLQueryBuilder(conn)
					 .update("wall_posts")
					 .valueExpr("reply_count", "reply_count+1")
					 .whereIn("id", Arrays.stream(replyKey).boxed().collect(Collectors.toList()))
					 .executeNoResult();

			 SQLQueryBuilder.prepareStatement(conn, "INSERT INTO newsfeed_comments (user_id, object_type, object_id) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE object_id=object_id", userID, 0, replyKey[0]).execute();
			 BackgroundTaskRunner.getInstance().submit(new UpdateCommentBookmarksRunnable(replyKey[0]));
		}
		return id;
	}

	public static void updateWallPost(int id, String text, String textSource, List<User> mentionedUsers, String attachments, String contentWarning, int pollID) throws SQLException{
		new SQLQueryBuilder()
				.update("wall_posts")
				.value("text", text)
				.value("source", textSource)
				.value("mentions", mentionedUsers.isEmpty() ? null : Utils.serializeIntArray(mentionedUsers.stream().mapToInt(u->u.id).toArray()))
				.value("attachments", attachments)
				.value("content_warning", contentWarning)
				.value("poll_id", pollID>0 ? pollID : null)
				.valueExpr("updated_at", "CURRENT_TIMESTAMP()")
				.where("id=?", id)
				.createStatement()
				.execute();
	}

	private static int putForeignPoll(Connection conn, int ownerID, URI activityPubID, Poll poll) throws SQLException{
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
		stmt=new SQLQueryBuilder(conn)
				.insertInto("poll_options")
				.value("poll_id", pollID)
				.value("ap_id", null)
				.value("text", null)
				.value("num_votes", 0)
				.createStatement(Statement.RETURN_GENERATED_KEYS);
		boolean hasIDs=false;
		for(PollOption opt:poll.options){
			if(opt.activityPubID!=null)
				hasIDs=true;
			else if(hasIDs)
				throw new IllegalStateException("all options must either have or not have IDs");
			stmt.setString(2, Objects.toString(opt.activityPubID, null));
			stmt.setString(3, opt.name);
			stmt.setInt(4, opt.getNumVotes());
			stmt.execute();
			try(ResultSet res=stmt.getGeneratedKeys()){
				res.first();
				opt.id=res.getInt(1);
			}
		}
		return pollID;
	}

	public static void putForeignWallPost(Post post) throws SQLException{
		Post existing=getPostByID(post.activityPubID);
		Connection conn=DatabaseConnectionManager.getConnection();

		PreparedStatement stmt;
		if(existing==null){
			if(post.poll!=null){
				post.poll.id=putForeignPoll(conn, post.owner.getOwnerID(), post.activityPubID, post.poll);
			}

			stmt=new SQLQueryBuilder(conn)
					.insertInto("wall_posts")
					.value("author_id", post.user.id)
					.value("owner_user_id", post.owner instanceof User ? post.owner.getLocalID() : null)
					.value("owner_group_id", post.owner instanceof Group ? post.owner.getLocalID() : null)
					.value("text", post.content)
					.value("attachments", post.serializeAttachments())
					.value("content_warning", post.hasContentWarning() ? post.summary : null)
					.value("ap_url", post.url.toString())
					.value("ap_id", post.activityPubID.toString())
					.value("reply_key", Utils.serializeIntArray(post.replyKey))
					.value("created_at", post.published)
					.value("mentions", post.mentionedUsers.isEmpty() ? null : Utils.serializeIntArray(post.mentionedUsers.stream().mapToInt(u->u.id).toArray()))
					.value("ap_replies", Objects.toString(post.getRepliesURL(), null))
					.value("poll_id", post.poll!=null ? post.poll.id : null)
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
					for(PollOption opt:existing.poll.options){
						optMap.put(opt.activityPubID, opt);
					}
					for(PollOption opt:post.poll.options){
						PollOption existingOpt=optMap.get(opt.activityPubID);
						if(existingOpt==null)
							throw new IllegalStateException("option with id "+opt.activityPubID+" not found in existing poll");
						opt.id=existingOpt.id;
						if(opt.getNumVotes()!=existingOpt.getNumVotes()){
							SQLQueryBuilder.prepareStatement(conn, "UPDATE poll_options SET num_votes=? WHERE id=? AND poll_id=?", opt.getNumVotes(), opt.id, post.poll.id).execute();
						}
					}
				}else{ // Match options using titles
					HashMap<String, PollOption> optMap=new HashMap<>(post.poll.options.size());
					for(PollOption opt:existing.poll.options){
						optMap.put(opt.name, opt);
					}
					for(PollOption opt:post.poll.options){
						PollOption existingOpt=optMap.get(opt.name);
						if(existingOpt==null)
							throw new IllegalStateException("option with name '"+opt.name+"' not found in existing poll");
						opt.id=existingOpt.id;
						if(opt.getNumVotes()!=existingOpt.getNumVotes()){
							SQLQueryBuilder.prepareStatement(conn, "UPDATE poll_options SET num_votes=? WHERE id=? AND poll_id=?", opt.getNumVotes(), opt.id, post.poll.id).execute();
						}
					}
				}
			}else if(post.poll!=null && existing.poll!=null){ // poll changed, delete it and recreate again
				// deletes votes and options because of ON DELETE CASCADE
				new SQLQueryBuilder(conn)
						.deleteFrom("polls")
						.where("id=?", existing.poll.id)
						.executeNoResult();
				post.poll.id=putForeignPoll(conn, post.owner.getOwnerID(), post.activityPubID, post.poll);
			}else if(post.poll!=null){ // poll was added
				post.poll.id=putForeignPoll(conn, post.owner.getOwnerID(), post.activityPubID, post.poll);
			}else if(existing.poll!=null){ // poll was removed
				new SQLQueryBuilder(conn)
						.deleteFrom("polls")
						.where("id=?", existing.poll.id)
						.executeNoResult();
			}
			stmt=new SQLQueryBuilder(conn)
					.update("wall_posts")
					.where("ap_id=?", post.activityPubID.toString())
					.value("text", post.content)
					.value("attachments", post.serializeAttachments())
					.value("content_warning", post.hasContentWarning() ? post.summary : null)
					.value("mentions", post.mentionedUsers.isEmpty() ? null : Utils.serializeIntArray(post.mentionedUsers.stream().mapToInt(u->u.id).toArray()))
					.value("poll_id", post.poll!=null ? post.poll.id : null)
					.createStatement();
		}
		if(existing==null){
			post.id=DatabaseUtils.insertAndGetID(stmt);
			if(post.owner.equals(post.user) && post.getReplyLevel()==0){
				new SQLQueryBuilder(conn)
						.insertInto("newsfeed")
						.value("type", NewsfeedEntry.Type.POST)
						.value("author_id", post.user.id)
						.value("object_id", post.id)
						.value("time", post.published)
						.executeNoResult();
			}
			if(post.getReplyLevel()>0){
				new SQLQueryBuilder(conn)
						.update("wall_posts")
						.valueExpr("reply_count", "reply_count+1")
						.whereIn("id", Arrays.stream(post.replyKey).boxed().collect(Collectors.toList()))
						.executeNoResult();
				BackgroundTaskRunner.getInstance().submit(new UpdateCommentBookmarksRunnable(post.replyKey[0]));
			}
		}else{
			stmt.execute();
			post.id=existing.id;
		}
	}

	public static List<NewsfeedEntry> getFeed(int userID, int startFromID, int offset, int count, int[] total) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt;
		if(total!=null){
			stmt=conn.prepareStatement("SELECT COUNT(*) FROM `newsfeed` WHERE `author_id` IN (SELECT followee_id FROM followings WHERE follower_id=? UNION SELECT ?) AND `id`<=? AND `time`>DATE_SUB(CURRENT_TIMESTAMP(), INTERVAL 10 DAY)");
			stmt.setInt(1, userID);
			stmt.setInt(2, userID);
			stmt.setInt(3, startFromID==0 ? Integer.MAX_VALUE : startFromID);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				total[0]=res.getInt(1);
			}
		}
		stmt=conn.prepareStatement("SELECT `type`, `object_id`, `author_id`, `id`, `time` FROM `newsfeed` WHERE (`author_id` IN (SELECT followee_id FROM followings WHERE follower_id=?) OR (type=0 AND author_id=?)) AND `id`<=? ORDER BY `time` DESC LIMIT ?,"+count);
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
					NewsfeedEntry entry=switch(type){
						case POST -> {
							PostNewsfeedEntry _entry=new PostNewsfeedEntry();
							_entry.objectID=res.getInt(2);
							needPosts.add(_entry.objectID);
							yield _entry;
						}
						case RETOOT -> {
							RetootNewsfeedEntry _entry=new RetootNewsfeedEntry();
							_entry.objectID=res.getInt(2);
							_entry.author=UserStorage.getById(res.getInt(3));
							needPosts.add(_entry.objectID);
							yield _entry;
						}
						case ADD_FRIEND -> {
							AddFriendNewsfeedEntry _entry=new AddFriendNewsfeedEntry();
							_entry.objectID=res.getInt(2);
							_entry.friend=UserStorage.getById(_entry.objectID);
							_entry.author=UserStorage.getById(res.getInt(3));
							yield _entry;
						}
						case JOIN_GROUP, CREATE_GROUP -> {
							JoinGroupNewsfeedEntry _entry=new JoinGroupNewsfeedEntry();
							_entry.objectID=res.getInt(2);
							_entry.group=GroupStorage.getById(_entry.objectID);
							_entry.author=UserStorage.getById(res.getInt(3));
							yield _entry;
						}
						case JOIN_EVENT, CREATE_EVENT -> {
							JoinEventNewsfeedEntry _entry=new JoinEventNewsfeedEntry();
							_entry.objectID=res.getInt(2);
							_entry.event=GroupStorage.getById(_entry.objectID);
							_entry.author=UserStorage.getById(res.getInt(3));
							yield _entry;
						}
						default -> throw new IllegalStateException("Unexpected value: "+type);
					};
					entry.type=type;
					entry.id=res.getInt(4);
					entry.time=res.getTimestamp(5).toInstant();
					posts.add(entry);
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

	public static List<Post> getWallPosts(int ownerID, boolean isGroup, int minID, int maxID, int offset, int count, int[] total, boolean ownOnly) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt;
		String ownCondition=ownOnly ? " AND owner_user_id=author_id" : "";
		String ownerField=isGroup ? "owner_group_id" : "owner_user_id";
		if(total!=null){
			stmt=conn.prepareStatement("SELECT COUNT(*) FROM `wall_posts` WHERE `"+ownerField+"`=? AND `reply_key` IS NULL"+ownCondition);
			stmt.setInt(1, ownerID);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				total[0]=res.getInt(1);
			}
		}
		if(minID>0){
			stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `"+ownerField+"`=? AND `id`>? AND `reply_key` IS NULL"+ownCondition+" ORDER BY created_at DESC LIMIT "+count);
			stmt.setInt(2, minID);
		}else if(maxID>0){
			stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `"+ownerField+"`=? AND `id`=<? AND `reply_key` IS NULL"+ownCondition+" ORDER BY created_at DESC LIMIT "+offset+","+count);
			stmt.setInt(2, maxID);
		}else{
			stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `"+ownerField+"`=? AND `reply_key` IS NULL"+ownCondition+" ORDER BY created_at DESC LIMIT "+offset+","+count);
		}
		stmt.setInt(1, ownerID);
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

	public static List<URI> getWallPostActivityPubIDs(int ownerID, boolean isGroup, int offset, int count, int[] total) throws SQLException{
		String ownerField=isGroup ? "owner_group_id" : "owner_user_id";
		Connection conn=DatabaseConnectionManager.getConnection();
		total[0]=new SQLQueryBuilder(conn)
				.selectFrom("wall_posts")
				.count()
				.where(ownerField+"=? AND reply_key IS NULL", ownerID)
				.executeAndGetInt();

		return new SQLQueryBuilder(conn)
				.selectFrom("wall_posts")
				.columns("id", "ap_id")
				.where(ownerField+"=? AND reply_key IS NULL", ownerID)
				.orderBy("id ASC")
				.limit(count, offset)
				.executeAsStream(res->{
					String apID=res.getString(2);
					if(StringUtils.isNotEmpty(apID)){
						return URI.create(apID);
					}else{
						return UriBuilder.local().path("posts", res.getInt(1)+"").build();
					}
				})
				.toList();
	}

	public static List<Post> getWallToWall(int userID, int otherUserID, int offset, int count, int[] total) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt;
		if(total!=null){
			stmt=conn.prepareStatement("SELECT COUNT(*) FROM wall_posts WHERE ((owner_user_id=? AND author_id=?) OR (owner_user_id=? AND author_id=?)) AND `reply_key` IS NULL");
			stmt.setInt(1, userID);
			stmt.setInt(2, otherUserID);
			stmt.setInt(3, otherUserID);
			stmt.setInt(4, userID);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
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
			if(res.first()){
				do{
					posts.add(Post.fromResultSet(res));
				}while(res.next());
			}
		}
		return posts;
	}

	public static @NotNull Post getPostOrThrow(int postID, boolean onlyLocal) throws SQLException{
		if(postID<=0)
			throw new ObjectNotFoundException("err_post_not_found");
		Post post=getPostByID(postID, false);
		if(post==null || (onlyLocal && !Config.isLocal(post.activityPubID)))
			throw new ObjectNotFoundException("err_post_not_found");
		return post;
	}

	public static Post getPostByID(int postID, boolean wantDeleted) throws SQLException{
		PreparedStatement stmt=DatabaseConnectionManager.getConnection().prepareStatement("SELECT * FROM wall_posts WHERE id=?");
		stmt.setInt(1, postID);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				Post post=Post.fromResultSet(res);
				if(post.isDeleted() && !wantDeleted)
					return null;
				return post;
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
			return getPostByID(postID, false);
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
		Post post=getPostByID(id, false);
		if(post==null)
			return;
		PreparedStatement stmt;
		boolean needFullyDelete=true;
		if(post.getReplyLevel()>0){
			stmt=conn.prepareStatement("SELECT COUNT(*) FROM wall_posts WHERE reply_key LIKE BINARY bin_prefix(?) ESCAPE CHAR(255)");
			int[] rk=new int[post.replyKey.length+1];
			System.arraycopy(post.replyKey, 0, rk, 0, post.replyKey.length);
			rk[rk.length-1]=post.id;
			stmt.setBytes(1, Utils.serializeIntArray(rk));
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				needFullyDelete=res.getInt(1)==0;
			}
		}

		if(post.poll!=null && post.poll.ownerID==post.user.id){
			SQLQueryBuilder.prepareStatement(conn, "DELETE FROM polls WHERE id=?", post.poll.id).execute();
		}

		if(needFullyDelete){
			stmt=conn.prepareStatement("DELETE FROM `wall_posts` WHERE `id`=?");
			stmt.setInt(1, id);
			stmt.execute();
		}else{
			// (comments don't exist in the feed anyway)
			stmt=conn.prepareStatement("UPDATE wall_posts SET author_id=NULL, owner_user_id=NULL, owner_group_id=NULL, text=NULL, attachments=NULL, content_warning=NULL, updated_at=NULL, mentions=NULL WHERE id=?");
			stmt.setInt(1, id);
			stmt.execute();
		}
		stmt=conn.prepareStatement("DELETE FROM `newsfeed` WHERE (`type`=0 OR `type`=1) AND `object_id`=?");
		stmt.setInt(1, id);
		stmt.execute();

		if(post.getReplyLevel()>0){
			conn.createStatement().execute("UPDATE wall_posts SET reply_count=GREATEST(1, reply_count)-1 WHERE id IN ("+Arrays.stream(post.replyKey).mapToObj(String::valueOf).collect(Collectors.joining(","))+")");
			BackgroundTaskRunner.getInstance().submit(new UpdateCommentBookmarksRunnable(post.replyKey[0]));
		}else{
			BackgroundTaskRunner.getInstance().submit(new DeleteCommentBookmarksRunnable(id));
		}
	}

	public static Map<Integer, PaginatedList<Post>> getRepliesForFeed(Set<Integer> postIDs) throws SQLException{
		if(postIDs.isEmpty())
			return Collections.emptyMap();
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement(String.join(" UNION ALL ", Collections.nCopies(postIDs.size(), "(SELECT * FROM wall_posts WHERE reply_key=? ORDER BY created_at DESC LIMIT 3)")));
		int i=0;
		for(int id:postIDs){
			stmt.setBytes(i+1, Utils.serializeIntArray(new int[]{id}));
			i++;
		}
		LOG.debug("{}", stmt);
		HashMap<Integer, PaginatedList<Post>> map=new HashMap<>();
		try(ResultSet res=stmt.executeQuery()){
			res.afterLast();
			while(res.previous()){
				Post post=Post.fromResultSet(res);
				List<Post> posts=map.computeIfAbsent(post.getReplyChainElement(0), (k)->new PaginatedList<>(new ArrayList<>(), 0)).list;
				posts.add(post);
			}
		}
		stmt=new SQLQueryBuilder(conn)
				.selectFrom("wall_posts")
				.selectExpr("count(*), reply_key")
				.groupBy("reply_key")
				.whereIn("reply_key", postIDs.stream().map(id->Utils.serializeIntArray(new int[]{id})).collect(Collectors.toList()))
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			res.beforeFirst();
			while(res.next()){
				int id=Utils.deserializeIntArray(res.getBytes(2))[0];
				map.get(id).total=res.getInt(1);
			}
		}
		return map;
	}

	public static List<Post> getReplies(int[] prefix) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT * FROM `wall_posts` WHERE `reply_key` LIKE BINARY bin_prefix(?) ESCAPE CHAR(255) ORDER BY `reply_key` ASC, `created_at` ASC LIMIT 100");
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
					parent.repliesObjects.add(post);
				}
			}
		}
		posts.removeIf(post->post.getReplyLevel()>prefix.length);

		return posts;
	}

	public static List<Post> getRepliesExact(int[] replyKey, int maxID, int limit, int[] total) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		if(total!=null){
			PreparedStatement stmt=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.count()
					.where("reply_key=? AND id<?", Utils.serializeIntArray(replyKey), maxID)
					.createStatement();
			total[0]=DatabaseUtils.oneFieldToInt(stmt.executeQuery());
		}
		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.selectFrom("wall_posts")
				.allColumns()
				.where("reply_key=? AND id<?", Utils.serializeIntArray(replyKey), maxID)
				.limit(limit, 0)
				.orderBy("created_at ASC")
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			ArrayList<Post> posts=new ArrayList<>();
			res.beforeFirst();
			while(res.next()){
				posts.add(Post.fromResultSet(res));
			}
			return posts;
		}
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

	public static HashMap<Integer, UserInteractions> getPostInteractions(Collection<Integer> postIDs, int userID) throws SQLException{
		HashMap<Integer, UserInteractions> result=new HashMap<>();
		if(postIDs.isEmpty())
			return result;
		for(int id:postIDs)
			result.put(id, new UserInteractions());
		String idsStr=postIDs.stream().map(Object::toString).collect(Collectors.joining(","));

		Connection conn=DatabaseConnectionManager.getConnection();
		try(ResultSet res=conn.createStatement().executeQuery("SELECT object_id, COUNT(*) FROM likes WHERE object_type=0 AND object_id IN ("+idsStr+") GROUP BY object_id")){
			if(res.first()){
				do{
					result.get(res.getInt(1)).likeCount=res.getInt(2);
				}while(res.next());
			}
		}
		if(userID!=0){
			PreparedStatement stmt=conn.prepareStatement("SELECT object_id FROM likes WHERE object_type=0 AND object_id IN ("+idsStr+") AND user_id=?");
			stmt.setInt(1, userID);
			try(ResultSet res=stmt.executeQuery()){
				if(res.first()){
					do{
						result.get(res.getInt(1)).isLiked=true;
					}while(res.next());
				}
			}
		}

		PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT option_id FROM poll_votes WHERE user_id=? AND poll_id=?", userID);
		try(ResultSet res=conn.createStatement().executeQuery("SELECT id, reply_count, poll_id FROM wall_posts WHERE id IN ("+idsStr+")")){
			res.beforeFirst();
			while(res.next()){
				UserInteractions interactions=result.get(res.getInt(1));
				interactions.commentCount=res.getInt(2);
				if(userID!=0){
					int pollID=res.getInt(3);
					if(!res.wasNull()){
						stmt.setInt(2, pollID);
						try(ResultSet res2=stmt.executeQuery()){
							res2.beforeFirst();
							interactions.pollChoices=new ArrayList<>();
							while(res2.next()){
								interactions.pollChoices.add(res2.getInt(1));
							}
						}
					}
				}
			}
		}

		return result;
	}

	public static List<URI> getInboxesForPostInteractionForwarding(Post post) throws SQLException{
		// Interaction on a top-level post:
		// - local: send to everyone who replied + the post's original addressees (followers + mentions if any)
		// - remote: send to the owner server only. It forwards as it pleases.
		// On a comment: do all of the above for the parent top-level post, and
		// - local: send to any mentioned users
		// - remote: send to the owner server, if not sent already if the parent post is local
		ArrayList<URI> inboxes=new ArrayList<>();
		Post origPost=post;
		if(post.getReplyLevel()>0){
			post=getPostByID(post.replyKey[0], false);
			if(post==null)
				return Collections.emptyList();
		}
		if(post.user instanceof ForeignUser && origPost.getReplyLevel()==0){
			return Collections.singletonList(((ForeignUser) post.user).inbox);
		}
		Connection conn=DatabaseConnectionManager.getConnection();
		ArrayList<String> queryParts=new ArrayList<>();
		if(post.local){
			queryParts.add("SELECT owner_user_id FROM wall_posts WHERE reply_key LIKE BINARY bin_prefix(?) ESCAPE CHAR(255)");
			if(post.owner instanceof ForeignUser)
				queryParts.add("SELECT "+((ForeignUser)post.owner).id);
			else if(post.owner instanceof User)
				queryParts.add("SELECT follower_id FROM followings WHERE followee_id="+((User)post.owner).id);
			else if(post.owner instanceof ForeignGroup)
				inboxes.add(Objects.requireNonNullElse(post.owner.sharedInbox, post.owner.inbox));
			else if(post.owner instanceof Group)
				queryParts.add("SELECT user_id FROM group_memberships WHERE group_id="+((Group)post.owner).id);

			if(post.mentionedUsers!=null && !post.mentionedUsers.isEmpty()){
				for(User user:post.mentionedUsers){
					if(user instanceof ForeignUser)
						queryParts.add("SELECT "+user.id);
				}
			}
		}else{
			queryParts.add("SELECT "+post.user.id);
		}
		if(origPost!=post){
			if(origPost.local){
				if(origPost.mentionedUsers!=null && !origPost.mentionedUsers.isEmpty()){
					for(User user:origPost.mentionedUsers){
						if(user instanceof ForeignUser)
							queryParts.add("SELECT "+user.id);
					}
				}
			}else{
				queryParts.add("SELECT "+origPost.user.id);
			}
		}
		PreparedStatement stmt=conn.prepareStatement("SELECT DISTINCT IFNULL(ap_shared_inbox, ap_inbox) FROM users WHERE id IN (" +
				String.join(" UNION ", queryParts) +
				") AND ap_inbox IS NOT NULL");
		if(post.local)
			stmt.setBytes(1, Utils.serializeIntArray(new int[]{post.id}));
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				do{
					URI uri=URI.create(res.getString(1));
					if(!inboxes.contains(uri))
						inboxes.add(uri);
				}while(res.next());
			}
		}

		return inboxes;
	}

	public static List<URI> getImmediateReplyActivityPubIDs(int[] replyKey, int offset, int count, int[] total) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		byte[] serializedKey=Utils.serializeIntArray(replyKey);
		PreparedStatement stmt=conn.prepareStatement("SELECT count(*) FROM wall_posts WHERE reply_key=?");
		stmt.setBytes(1, serializedKey);
		try(ResultSet res=stmt.executeQuery()){
			res.first();
			total[0]=res.getInt(1);
		}
		stmt=conn.prepareStatement("SELECT ap_id, id FROM wall_posts WHERE reply_key=? ORDER BY created_at ASC LIMIT ?,?");
		stmt.setBytes(1, serializedKey);
		stmt.setInt(2, offset);
		stmt.setInt(3, count);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				ArrayList<URI> replies=new ArrayList<>();
				do{
					String apID=res.getString(1);
					if(apID!=null)
						replies.add(URI.create(apID));
					else
						replies.add(Config.localURI("/posts/"+res.getInt(2)));
				}while(res.next());
				return replies;
			}
			return Collections.emptyList();
		}
	}

	public static Poll getPoll(int id, URI parentApID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.selectFrom("polls")
				.allColumns()
				.where("id=?", id)
				.createStatement();
		Poll poll;
		try(ResultSet res=stmt.executeQuery()){
			if(!res.first())
				return null;
			poll=Poll.fromResultSet(res);
		}
		stmt=new SQLQueryBuilder(conn)
				.selectFrom("poll_options")
				.where("poll_id=?", id)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			res.beforeFirst();
			while(res.next()){
				poll.options.add(PollOption.fromResultSet(res, poll.activityPubID==null ? parentApID : poll.activityPubID, poll));
			}
		}
		return poll;
	}

	public static synchronized int[] voteInPoll(int userID, int pollID, int[] optionIDs) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.selectFrom("poll_votes")
				.count()
				.where("user_id=? AND poll_id=?", userID, pollID)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			res.first();
			if(res.getInt(1)>0)
				return null;
		}

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
		for(int optID:optionIDs){
			stmt1.setInt(1, optID);
			stmt1.execute();
			stmt2.setInt(1, optID);
			stmt2.execute();
			try(ResultSet res=stmt1.getGeneratedKeys()){
				res.first();
				voteIDs[i++]=res.getInt(1);
			}
		}

		new SQLQueryBuilder(conn)
				.update("polls")
				.valueExpr("num_voted_users", "num_voted_users+1")
				.valueExpr("last_vote_time", "CURRENT_TIMESTAMP()")
				.where("id=?", pollID)
				.executeNoResult();

		return voteIDs;
	}

	// This is called once for each choice in a multiple-choice poll
	public static synchronized int voteInPoll(int userID, int pollID, int optionID, URI voteID, boolean allowMultiple) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.selectFrom("poll_votes")
				.columns("option_id")
				.where("user_id=? AND poll_id=?", userID, pollID)
				.createStatement();
		boolean userVoted=false;
		try(ResultSet res=stmt.executeQuery()){
			res.beforeFirst();
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

		PreparedStatement stmt1=new SQLQueryBuilder(conn)
				.insertInto("poll_votes")
				.value("option_id", optionID)
				.value("user_id", userID)
				.value("poll_id", pollID)
				.value("ap_id", voteID)
				.createStatement(Statement.RETURN_GENERATED_KEYS);

		PreparedStatement stmt2=new SQLQueryBuilder(conn)
				.update("poll_options")
				.where("id=?", optionID)
				.valueExpr("num_votes", "num_votes+1")
				.createStatement();

		int rVoteID;
		stmt1.execute();
		stmt2.execute();
		try(ResultSet res=stmt1.getGeneratedKeys()){
			res.first();
			rVoteID=res.getInt(1);
		}

		if(!userVoted){
			new SQLQueryBuilder(conn)
					.update("polls")
					.valueExpr("num_voted_users", "num_voted_users+1")
					.where("id=?", pollID)
					.executeNoResult();
		}

		return rVoteID;
	}

	public static int createPoll(int ownerID, @NotNull String question, @NotNull List<String> options, boolean anonymous, boolean multiChoice, @Nullable Instant endTime) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		int pollID=new SQLQueryBuilder(conn)
				.insertInto("polls")
				.value("owner_id", ownerID)
				.value("question", question)
				.value("is_anonymous", anonymous)
				.value("is_multi_choice", multiChoice)
				.value("end_time", endTime)
				.executeAndGetID();

		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.insertInto("poll_options")
				.value("text", "")
				.value("poll_id", pollID)
				.createStatement();
		for(String opt:options){
			stmt.setString(1, opt);
			stmt.execute();
		}

		return pollID;
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
		PreparedStatement stmt=SQLQueryBuilder.prepareStatement(DatabaseConnectionManager.getConnection(),
				"SELECT users.id, users.ap_id FROM poll_votes JOIN users ON poll_votes.user_id=users.id WHERE poll_votes.option_id=? LIMIT ? OFFSET ?", optionID, count, offset);
		try(ResultSet res=stmt.executeQuery()){
			res.beforeFirst();
			ArrayList<URI> r=new ArrayList<>();
			while(res.next()){
				String apID=res.getString(2);
				r.add(apID!=null ? URI.create(apID) : Config.localURI("/users/"+res.getInt(1)));
			}
			return r;
		}
	}

	public static void setPostFederationState(int postID, FederationState state) throws SQLException{
		new SQLQueryBuilder()
				.update("wall_posts")
				.value("federation_state", state)
				.where("id=?", postID)
				.createStatement()
				.execute();
	}

	public static PaginatedList<NewsfeedEntry> getCommentsFeed(int userID, int offset, int count) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		int total;
		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.selectFrom("newsfeed_comments")
				.count()
				.where("user_id=?", userID)
				.createStatement();

		try(ResultSet res=stmt.executeQuery()){
			res.first();
			total=res.getInt(1);
		}

		if(total==0)
			return new PaginatedList<>(Collections.emptyList(), 0);

		stmt=new SQLQueryBuilder(conn)
				.selectFrom("newsfeed_comments")
				.columns("object_type", "object_id")
				.where("user_id=?", userID)
				.orderBy("last_comment_time DESC")
				.limit(count, offset)
				.createStatement();
		List<Integer> needPosts=new ArrayList<>();
		List<NewsfeedEntry> entries=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			res.beforeFirst();
			while(res.next()){
				Like.ObjectType type=Like.ObjectType.values()[res.getInt(1)];
				int id=res.getInt(2);
				switch(type){
					case POST -> {
						needPosts.add(id);
						PostNewsfeedEntry entry=new PostNewsfeedEntry();
						entry.objectID=id;
						entry.type=NewsfeedEntry.Type.POST;
						entries.add(entry);
					}
				}
			}
		}
		if(needPosts.isEmpty())
			return new PaginatedList<>(entries, total);

		stmt=new SQLQueryBuilder(conn)
				.selectFrom("wall_posts")
				.allColumns()
				.whereIn("id", needPosts)
				.createStatement();

		Map<Integer, Post> posts;
		posts=DatabaseUtils.resultSetToObjectStream(stmt.executeQuery(), Post::fromResultSet).collect(Collectors.toMap(post->post.id, Function.identity()));
		for(NewsfeedEntry entry : entries){
			if(entry.type==NewsfeedEntry.Type.POST){
				((PostNewsfeedEntry) entry).post=posts.get(entry.objectID);
			}
		}

		return new PaginatedList<>(entries.stream().filter(e->!(e instanceof PostNewsfeedEntry pe) || pe.post!=null).collect(Collectors.toList()), total, offset, count);
	}

	public static Map<URI, Integer> getPostLocalIDsByActivityPubIDs(Collection<URI> ids, int ownerUserID, int ownerGroupID) throws SQLException{
		if(ids.isEmpty())
			return Map.of();

		Connection conn=DatabaseConnectionManager.getConnection();

		List<Integer> localIDs=ids.stream().filter(Config::isLocal).map(u->{
			String path=u.getPath();
			if(StringUtils.isEmpty(path))
				return 0;
			String[] parts=path.split("/"); // "", "posts", id
			return parts.length==3 && "posts".equals(parts[1]) ? Utils.safeParseInt(parts[2]) : 0;
		}).filter(i->i>0).toList();
		List<String> remoteIDs=ids.stream().filter(Predicate.not(Config::isLocal)).map(Object::toString).toList();

		Map<URI, Integer> result=new HashMap<>();
		record IdPair(URI apID, int localID){}

		if(!remoteIDs.isEmpty()){
			SQLQueryBuilder builder=new SQLQueryBuilder(conn)
					.selectFrom("wall_posts")
					.columns("id", "ap_id")
					.whereIn("ap_id", remoteIDs)
					.andWhere("reply_key IS NULL");

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
					.andWhere("reply_key IS NULL");

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

	public static int getPostIdByPollId(int pollID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("wall_posts")
				.columns("id")
				.where("poll_id=?", pollID)
				.executeAndGetInt();
	}

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
					Connection conn=DatabaseConnectionManager.getConnection();
					PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT MAX(created_at) FROM wall_posts WHERE reply_key LIKE BINARY bin_prefix(?)", (Object) Utils.serializeIntArray(new int[]{postID}));
					Timestamp ts;
					try(ResultSet res=stmt.executeQuery()){
						res.first();
						ts=res.getTimestamp(1);
					}
					if(ts==null){
						new SQLQueryBuilder(conn)
								.deleteFrom("newsfeed_comments")
								.where("object_type=? AND object_id=?", 0, postID)
								.executeNoResult();
					}else{
						new SQLQueryBuilder(conn)
								.update("newsfeed_comments")
								.value("last_comment_time", ts)
								.where("object_type=? AND object_id=?", 0, postID)
								.executeNoResult();
					}
				}catch(SQLException x){
					LOG.warn("Error updating comment bookmarks for post {}", postID, x);
				}
			}
		}
}
