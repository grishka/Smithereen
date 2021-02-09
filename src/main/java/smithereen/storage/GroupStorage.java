package smithereen.storage;

import org.json.JSONObject;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.LruCache;
import smithereen.activitypub.ContextCollector;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.GroupAdmin;
import smithereen.data.User;
import spark.utils.StringUtils;

public class GroupStorage{

	private static final LruCache<Integer, Group> cacheByID=new LruCache<>(500);
	private static final LruCache<String, Group> cacheByUsername=new LruCache<>(500);
	private static final LruCache<URI, ForeignGroup> cacheByActivityPubID=new LruCache<>(500);

	public static int createGroup(String name, String username, int userID) throws SQLException{
		int id;
		Connection conn=DatabaseConnectionManager.getConnection();
		try{
			conn.createStatement().execute("START TRANSACTION");

			KeyPairGenerator kpg=KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair pair=kpg.generateKeyPair();

			PreparedStatement stmt=new SQLQueryBuilder(conn)
					.insertInto("groups")
					.value("name", name)
					.value("username", username)
					.value("public_key", pair.getPublic().getEncoded())
					.value("private_key", pair.getPrivate().getEncoded())
					.value("member_count", 1)
					.createStatement(Statement.RETURN_GENERATED_KEYS);
			id=DatabaseUtils.insertAndGetID(stmt);

			new SQLQueryBuilder(conn)
					.insertInto("group_memberships")
					.value("user_id", userID)
					.value("group_id", id)
					.createStatement()
					.execute();

			new SQLQueryBuilder(conn)
					.insertInto("group_admins")
					.value("user_id", userID)
					.value("group_id", id)
					.value("level", Group.AdminLevel.OWNER)
					.createStatement()
					.execute();
		}catch(Exception x){
			conn.createStatement().execute("ROLLBACK");
			throw new SQLException(x);
		}
		SessionStorage.removeFromUserPermissionsCache(userID);
		conn.createStatement().execute("COMMIT");
		return id;
	}

	public static synchronized void putOrUpdateForeignGroup(ForeignGroup group) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		int existingGroupID;
		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.selectFrom("groups")
				.columns("id")
				.where("ap_id=?", group.activityPubID.toString())
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			existingGroupID=res.first() ? res.getInt(1) : 0;
		}

		SQLQueryBuilder builder=new SQLQueryBuilder(conn);
		if(existingGroupID==0){
			builder.insertInto("groups");
		}else{
			builder.update("groups").where("id=?", existingGroupID);
		}

		builder.value("name", group.name)
				.value("username", group.username)
				.value("domain", group.domain)
				.value("ap_id", group.activityPubID.toString())
				.value("ap_url", Objects.toString(group.url, null))
				.value("ap_inbox", group.inbox.toString())
				.value("ap_shared_inbox", Objects.toString(group.sharedInbox, null))
				.value("ap_outbox", Objects.toString(group.outbox, null))
				.value("public_key", group.publicKey.getEncoded())
				.value("avatar", group.hasAvatar() ? group.icon.get(0).asActivityPubObject(new JSONObject(), new ContextCollector()).toString() : null)
				.value("ap_followers", Objects.toString(group.followers, null))
				.value("ap_wall", Objects.toString(group.getWallURL(), null))
				.valueExpr("last_updated", "CURRENT_TIMESTAMP()");

		stmt=builder.createStatement(Statement.RETURN_GENERATED_KEYS);
		if(existingGroupID==0){
			group.id=DatabaseUtils.insertAndGetID(stmt);
		}else{
			stmt.execute();
		}
		putIntoCache(group);
	}

	public static synchronized Group getByID(int id) throws SQLException{
		Group g=cacheByID.get(id);
		if(g!=null)
			return g;
		PreparedStatement stmt=new SQLQueryBuilder().selectFrom("groups").allColumns().where("id=?", id).createStatement();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				g=Group.fromResultSet(res);
				putIntoCache(g);
				return g;
			}
			return null;
		}
	}

	public static synchronized Group getByUsername(String username) throws SQLException{
		Group g=cacheByUsername.get(username.toLowerCase());
		if(g!=null)
			return g;
		String domain;
		if(username.contains("@")){
			String[] parts=username.split("@", 2);
			username=parts[0];
			domain=parts[1];
		}else{
			domain="";
		}
		PreparedStatement stmt=new SQLQueryBuilder().selectFrom("groups").allColumns().where("username=? AND domain=?", username, domain).createStatement();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				g=Group.fromResultSet(res);
				putIntoCache(g);
				return g;
			}
			return null;
		}
	}

	public static synchronized ForeignGroup getForeignGroupByActivityPubID(URI id) throws SQLException{
		ForeignGroup g=cacheByActivityPubID.get(id);
		if(g!=null)
			return g;
		PreparedStatement stmt=new SQLQueryBuilder().selectFrom("groups").allColumns().where("ap_id=?", id.toString()).createStatement();
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				g=ForeignGroup.fromResultSet(res);
				putIntoCache(g);
				return g;
			}
			return null;
		}
	}

	public static List<Group> getByID(List<Integer> ids) throws SQLException{
		if(ids.isEmpty())
			return Collections.emptyList();
		if(ids.size()==1)
			return Collections.singletonList(getByID(ids.get(0)));
		List<Group> result=new ArrayList<>(ids.size());
		synchronized(GroupStorage.class){
			Iterator<Integer> itr=ids.iterator();
			while(itr.hasNext()){
				Integer id=itr.next();
				Group group=cacheByID.get(id);
				if(group!=null){
					itr.remove();
					result.add(group);
				}
			}
		}
		if(ids.isEmpty())
			return result;
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("groups")
				.allColumns()
				.whereIn("id", ids)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			res.beforeFirst();
			int resultSizeBefore=result.size();
			while(res.next()){
				String domain=res.getString("domain");
				Group group;
				if(StringUtils.isNotEmpty(domain))
					group=ForeignGroup.fromResultSet(res);
				else
					group=Group.fromResultSet(res);
				result.add(group);
			}
			synchronized(GroupStorage.class){
				for(Group group:result.subList(resultSizeBefore, result.size())){
					putIntoCache(group);
				}
			}
//			if(sorted)
//				result.sort(idComparator);
			return result;
		}
	}

	public static List<User> getRandomMembersForProfile(int groupID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT user_id FROM group_memberships WHERE group_id=? ORDER BY RAND() LIMIT 6");
		stmt.setInt(1, groupID);
		try(ResultSet res=stmt.executeQuery()){
			return UserStorage.getById(DatabaseUtils.intResultSetToList(res), false);
		}
	}

	public static List<User> getMembers(int groupID, int offset, int count) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("group_memberships")
				.where("group_id=? AND accepted=1", groupID)
				.limit(count, offset)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			return UserStorage.getById(DatabaseUtils.intResultSetToList(res));
		}
	}

	public static Group.MembershipState getUserMembershipState(int groupID, int userID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT * FROM group_memberships WHERE group_id=? AND user_id=?");
		stmt.setInt(1, groupID);
		stmt.setInt(2, userID);
		try(ResultSet res=stmt.executeQuery()){
			if(!res.first())
				return Group.MembershipState.NONE;
			return Group.MembershipState.MEMBER;
		}
	}

	public static void joinGroup(Group group, int userID, boolean tentative, boolean accepted) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		conn.createStatement().execute("START TRANSACTION");
		boolean success=false;
		try{
			PreparedStatement stmt=conn.prepareStatement("INSERT INTO group_memberships (user_id, group_id, tentative, accepted) VALUES (?, ?, ?, ?)");
			stmt.setInt(1, userID);
			stmt.setInt(2, group.id);
			stmt.setBoolean(3, tentative);
			stmt.setBoolean(4, accepted);
			stmt.execute();

			String memberCountField=tentative ? "tentative_member_count" : "member_count";
			stmt=conn.prepareStatement("UPDATE groups SET "+memberCountField+"="+memberCountField+"+1 WHERE id=?");
			stmt.setInt(1, group.id);
			stmt.execute();

			removeFromCache(group);

			success=true;
		}finally{
			conn.createStatement().execute(success ? "COMMIT" : "ROLLBACK");
		}
	}

	public static void leaveGroup(Group group, int userID, boolean tentative) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		conn.createStatement().execute("START TRANSACTION");
		boolean success=false;
		try{
			PreparedStatement stmt=conn.prepareStatement("DELETE FROM group_memberships WHERE user_id=? AND group_id=?");
			stmt.setInt(1, userID);
			stmt.setInt(2, group.id);
			stmt.execute();

			String memberCountField=tentative ? "tentative_member_count" : "member_count";
			stmt=conn.prepareStatement("UPDATE groups SET "+memberCountField+"="+memberCountField+"-1 WHERE id=?");
			stmt.setInt(1, group.id);
			stmt.execute();

			removeFromCache(group);

			success=true;
		}finally{
			conn.createStatement().execute(success ? "COMMIT" : "ROLLBACK");
		}
	}

	public static List<Group> getUserGroups(int userID) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder().selectFrom("group_memberships").columns("group_id").where("user_id=? AND accepted=1", userID).createStatement();
		try(ResultSet res=stmt.executeQuery()){
			return getByID(DatabaseUtils.intResultSetToList(res));
		}
	}

	public static List<URI> getGroupMemberURIs(int groupID, boolean tentative, int offset, int count, int[] total) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt;
		if(total!=null){
			stmt=new SQLQueryBuilder(conn).selectFrom("group_memberships").count().where("group_id=? AND accepted=1 AND tentative=?", groupID, tentative).createStatement();
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				total[0]=res.getInt(1);
			}
		}
		if(count>0){
			stmt=conn.prepareStatement("SELECT `ap_id`,`id` FROM `group_memberships` INNER JOIN `users` ON `users`.`id`=`user_id` WHERE `group_id`=? AND `accepted`=1 AND tentative=? LIMIT ? OFFSET ?");
			stmt.setInt(1, groupID);
			stmt.setBoolean(2, tentative);
			stmt.setInt(3, count);
			stmt.setInt(4, offset);
			ArrayList<URI> list=new ArrayList<>();
			try(ResultSet res=stmt.executeQuery()){
				if(res.first()){
					do{
						String _u=res.getString(1);
						if(_u==null){
							list.add(Config.localURI("/users/"+res.getInt(2)));
						}else{
							list.add(URI.create(_u));
						}
					}while(res.next());
				}
			}
			return list;
		}
		return Collections.emptyList();
	}

	public static List<URI> getGroupMemberInboxes(int groupID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT DISTINCT IFNULL(ap_shared_inbox, ap_inbox) FROM users WHERE id IN (SELECT user_id FROM group_memberships WHERE group_id=? AND accepted=1) AND ap_inbox IS NOT NULL");
		stmt.setInt(1, groupID);
		ArrayList<URI> inboxes=new ArrayList<>();
		try(ResultSet res=stmt.executeQuery()){
			res.beforeFirst();
			while(res.next()){
				inboxes.add(URI.create(res.getString(1)));
			}
			return inboxes;
		}
	}

	public static Group.AdminLevel getGroupMemberAdminLevel(int groupID, int userID) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("group_admins")
				.columns("level")
				.where("group_id=? AND user_id=?", groupID, userID)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			return res.first() ? Group.AdminLevel.values()[res.getInt(1)] : Group.AdminLevel.REGULAR;
		}
	}

	public static void setMemberAccepted(int groupID, int userID, boolean accepted) throws SQLException{
		new SQLQueryBuilder()
				.update("group_memberships")
				.value("accepted", accepted)
				.where("group_id=? AND user_id=?", groupID, userID)
				.createStatement()
				.execute();
	}

	public static List<GroupAdmin> getGroupAdmins(int groupID) throws SQLException{
		SQLQueryBuilder b=new SQLQueryBuilder()
				.selectFrom("group_admins")
				.columns("level", "user_id", "title")
				.where("group_id=?", groupID);
		try(ResultSet res=b.createStatement().executeQuery()){
			ArrayList<GroupAdmin> admins=new ArrayList<>();
			res.beforeFirst();
			while(res.next()){
				GroupAdmin admin=new GroupAdmin();
				admin.level=Group.AdminLevel.values()[res.getInt(1)];
				admin.user=UserStorage.getById(res.getInt(2));
				admin.title=res.getString(3);
				admins.add(admin);
			}
			return admins;
		}
	}

	public static void updateProfilePicture(Group group, String serializedPic) throws SQLException{
		new SQLQueryBuilder()
				.update("groups")
				.value("avatar", serializedPic)
				.where("id=?", group.id)
				.createStatement()
				.execute();
		synchronized(GroupStorage.class){
			removeFromCache(group);
		}
	}

	public static void updateGroupGeneralInfo(Group group, String name, String about) throws SQLException{
		new SQLQueryBuilder()
				.update("groups")
				.value("name", name)
				.value("about", about)
				.where("id=?", group.id)
				.createStatement()
				.execute();
		synchronized(GroupStorage.class){
			removeFromCache(group);
		}
	}

	private static void putIntoCache(Group group){
		cacheByID.put(group.id, group);
		cacheByUsername.put(group.getFullUsername().toLowerCase(), group);
		if(group instanceof ForeignGroup)
			cacheByActivityPubID.put(group.activityPubID, (ForeignGroup) group);
	}

	private static void removeFromCache(Group group){
		cacheByID.remove(group.id);
		cacheByUsername.remove(group.getFullUsername().toLowerCase());
		if(group instanceof ForeignGroup)
			cacheByActivityPubID.remove(group.activityPubID);
	}
}
