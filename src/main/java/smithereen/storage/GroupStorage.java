package smithereen.storage;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.ContextCollector;
import smithereen.controllers.GroupsController;
import smithereen.data.ForeignGroup;
import smithereen.data.Group;
import smithereen.data.GroupAdmin;
import smithereen.data.PaginatedList;
import smithereen.data.User;
import spark.utils.StringUtils;

public class GroupStorage{

	private static final LruCache<Integer, Group> cacheByID=new LruCache<>(500);
	private static final LruCache<String, Group> cacheByUsername=new LruCache<>(500);
	private static final LruCache<URI, ForeignGroup> cacheByActivityPubID=new LruCache<>(500);

	private static final Object adminUpdateLock=new Object();

	public static int createGroup(String name, String description, String descriptionSrc, int userID, boolean isEvent, Instant eventStart, Instant eventEnd) throws SQLException{
		int id;
		Connection conn=DatabaseConnectionManager.getConnection();
		try{
			conn.createStatement().execute("START TRANSACTION");

			KeyPairGenerator kpg=KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair pair=kpg.generateKeyPair();

			PreparedStatement stmt;
			String username;
			synchronized(GroupStorage.class){
				SQLQueryBuilder bldr=new SQLQueryBuilder(conn)
						.insertInto("groups")
						.value("name", name)
						.value("username", "__tmp"+System.currentTimeMillis())
						.value("public_key", pair.getPublic().getEncoded())
						.value("private_key", pair.getPrivate().getEncoded())
						.value("member_count", 1)
						.value("about", description)
						.value("about_source", descriptionSrc)
						.value("event_start_time", eventStart)
						.value("event_end_time", eventEnd)
						.value("type", isEvent ? Group.Type.EVENT : Group.Type.GROUP);

				stmt=bldr.createStatement(Statement.RETURN_GENERATED_KEYS);
				id=DatabaseUtils.insertAndGetID(stmt);
				username=(isEvent ? "event" : "club")+id;

				new SQLQueryBuilder(conn)
						.update("groups")
						.value("username", username)
						.where("id=?", id)
						.createStatement()
						.execute();
			}

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

			new SQLQueryBuilder(conn)
					.insertInto("qsearch_index")
					.value("string", Utils.transliterate(name)+" "+username)
					.value("group_id", id)
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
				.value("avatar", group.hasAvatar() ? group.icon.get(0).asActivityPubObject(new JsonObject(), new ContextCollector()).toString() : null)
				.value("ap_followers", Objects.toString(group.followers, null))
				.value("ap_wall", Objects.toString(group.getWallURL(), null))
				.value("event_start_time", group.eventStartTime)
				.value("event_end_time", group.eventEndTime)
				.value("type", group.type)
				.value("flags", Utils.serializeEnumSet(group.capabilities, ForeignGroup.Capability.class))
				.valueExpr("last_updated", "CURRENT_TIMESTAMP()");

		stmt=builder.createStatement(Statement.RETURN_GENERATED_KEYS);
		if(existingGroupID==0){
			group.id=DatabaseUtils.insertAndGetID(stmt);
			new SQLQueryBuilder(conn)
					.insertInto("qsearch_index")
					.value("group_id", group.id)
					.value("string", getQSearchStringForGroup(group))
					.createStatement()
					.execute();
		}else{
			group.id=existingGroupID;
			stmt.execute();
			new SQLQueryBuilder(conn)
					.update("qsearch_index")
					.value("string", getQSearchStringForGroup(group))
					.where("group_id=?", existingGroupID)
					.createStatement()
					.execute();
		}
		putIntoCache(group);
		synchronized(adminUpdateLock){
			stmt=new SQLQueryBuilder(conn)
					.selectFrom("group_admins")
					.columns("user_id", "title")
					.where("group_id=?", group.id)
					.createStatement();
			Map<Integer, GroupAdmin> admins=group.adminsForActivityPub.stream().collect(Collectors.toMap(adm->adm.user.id, adm->adm));
			int count=0;
			boolean needUpdate=false;
			try(ResultSet res=stmt.executeQuery()){
				res.beforeFirst();
				while(res.next()){
					count++;
					int id=res.getInt(1);
					String title=res.getString(2);
					if(!admins.containsKey(id)){
						needUpdate=true;
						break;
					}
					GroupAdmin existing=admins.get(id);
					if(!Objects.equals(title, existing.title)){
						needUpdate=true;
						break;
					}
				}
			}
			if(!needUpdate && count!=group.adminsForActivityPub.size())
				needUpdate=true;

			// TODO only update whatever has actually changed
			if(needUpdate){
				new SQLQueryBuilder(conn)
						.deleteFrom("group_admins")
						.where("group_id=?", group.id)
						.createStatement()
						.execute();
				int order=0;
				for(GroupAdmin admin:group.adminsForActivityPub){
					new SQLQueryBuilder(conn)
							.insertInto("group_admins")
							.value("group_id", group.id)
							.value("user_id", admin.user.id)
							.value("title", admin.title)
							.value("level", Group.AdminLevel.MODERATOR.ordinal())
							.value("display_order", order)
							.createStatement()
							.execute();
					order++;
				}
			}
		}
	}

	public static synchronized Group getById(int id) throws SQLException{
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

	public static List<Group> getByIdAsList(Collection<Integer> ids) throws SQLException{
		if(ids.isEmpty())
			return Collections.emptyList();
		if(ids.size()==1){
			for(int id:ids)
				return Collections.singletonList(getById(id));
		}
		Map<Integer, Group> groups=getById(ids);
		return ids.stream().map(groups::get).filter(Objects::nonNull).collect(Collectors.toList());
	}

	public static Map<Integer, Group> getById(Collection<Integer> _ids) throws SQLException{
		if(_ids.isEmpty())
			return Collections.emptyMap();
		if(_ids.size()==1){
			for(int id:_ids){
				return Collections.singletonMap(id, getById(id));
			}
		}
		Set<Integer> ids=new HashSet<>(_ids);
		Map<Integer, Group> result=new HashMap<>(ids.size());
		synchronized(GroupStorage.class){
			Iterator<Integer> itr=ids.iterator();
			while(itr.hasNext()){
				Integer id=itr.next();
				Group group=cacheByID.get(id);
				if(group!=null){
					itr.remove();
					result.put(id, group);
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
			while(res.next()){
				String domain=res.getString("domain");
				Group group;
				if(StringUtils.isNotEmpty(domain))
					group=ForeignGroup.fromResultSet(res);
				else
					group=Group.fromResultSet(res);
				result.put(group.id, group);
			}
			synchronized(GroupStorage.class){
				for(int id:ids){
					putIntoCache(result.get(id));
				}
			}
			return result;
		}
	}

	public static List<User> getRandomMembersForProfile(int groupID, boolean tentative) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT user_id FROM group_memberships WHERE group_id=? AND tentative=? ORDER BY RAND() LIMIT 6", groupID, tentative);
		try(ResultSet res=stmt.executeQuery()){
			return UserStorage.getByIdAsList(DatabaseUtils.intResultSetToList(res));
		}
	}

	public static PaginatedList<User> getMembers(int groupID, int offset, int count, @Nullable Boolean tentative) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		String _tentative=tentative==null ? "" : (" AND tentative="+(tentative ? '1' : '0'));
		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.selectFrom("group_memberships")
				.count()
				.where("group_id=? AND accepted=1"+_tentative, groupID)
				.createStatement();
		int total=DatabaseUtils.oneFieldToInt(stmt.executeQuery());
		if(total==0)
			return PaginatedList.emptyList(count);
		stmt=new SQLQueryBuilder(conn)
				.selectFrom("group_memberships")
				.where("group_id=? AND accepted=1"+_tentative, groupID)
				.limit(count, offset)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			return new PaginatedList<>(UserStorage.getByIdAsList(DatabaseUtils.intResultSetToList(res)), total, offset, count);
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
			return res.getBoolean("tentative") ? Group.MembershipState.TENTATIVE_MEMBER : Group.MembershipState.MEMBER;
		}
	}

	public static void joinGroup(Group group, int userID, boolean tentative, boolean accepted) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		conn.createStatement().execute("START TRANSACTION");
		boolean success=false;
		try{
			new SQLQueryBuilder(conn)
					.insertInto("group_memberships")
					.value("user_id", userID)
					.value("group_id", group.id)
					.value("tentative", tentative)
					.value("accepted", accepted)
					.createStatement()
					.execute();

			String memberCountField=tentative ? "tentative_member_count" : "member_count";
			new SQLQueryBuilder(conn)
					.update("groups")
					.valueExpr(memberCountField, memberCountField+"+1")
					.where("id=?", group.id)
					.createStatement()
					.execute();

			removeFromCache(group);

			success=true;
		}finally{
			conn.createStatement().execute(success ? "COMMIT" : "ROLLBACK");
		}
	}

	public static void updateUserEventDecision(Group group, int userID, boolean tentative) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		DatabaseUtils.doWithTransaction(conn, ()->{
			new SQLQueryBuilder(conn)
					.update("group_memberships")
					.where("user_id=? AND group_id=?", userID, group.id)
					.value("tentative", tentative)
					.createStatement()
					.execute();

			String memberCountFieldOld=tentative ? "member_count" : "tentative_member_count";
			String memberCountFieldNew=tentative ? "tentative_member_count" : "member_count";
			new SQLQueryBuilder(conn)
					.update("groups")
					.valueExpr(memberCountFieldOld, memberCountFieldOld+"-1")
					.valueExpr(memberCountFieldNew, memberCountFieldNew+"+1")
					.where("id=?", group.id)
					.createStatement()
					.execute();
			removeFromCache(group);
		});
	}

	public static void leaveGroup(Group group, int userID, boolean tentative) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		conn.createStatement().execute("START TRANSACTION");
		boolean success=false;
		try{
			new SQLQueryBuilder(conn)
					.deleteFrom("group_memberships")
					.where("user_id=? AND group_id=?", userID, group.id)
					.createStatement()
					.execute();

			String memberCountField=tentative ? "tentative_member_count" : "member_count";
			new SQLQueryBuilder(conn)
					.update("groups")
					.valueExpr(memberCountField, memberCountField+"-1")
					.where("id=?", group.id)
					.createStatement()
					.execute();

			removeFromCache(group);

			success=true;
		}finally{
			conn.createStatement().execute(success ? "COMMIT" : "ROLLBACK");
		}
	}

	public static PaginatedList<Group> getUserGroups(int userID, int offset, int count) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		String query="SELECT %s FROM group_memberships JOIN `groups` ON group_id=`groups`.id WHERE user_id=? AND accepted=1 AND `groups`.`type`=0";
		PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, String.format(Locale.US, query, "COUNT(*)"), userID);
		int total=DatabaseUtils.oneFieldToInt(stmt.executeQuery());
		if(total==0)
			return new PaginatedList<>(Collections.emptyList(), 0, 0, count);
		query+=" ORDER BY group_id ASC LIMIT ? OFFSET ?";
		stmt=SQLQueryBuilder.prepareStatement(conn, String.format(Locale.US, query, "group_id"), userID, count, offset);
		try(ResultSet res=stmt.executeQuery()){
			return new PaginatedList<>(getByIdAsList(DatabaseUtils.intResultSetToList(res)), total, offset, count);
		}
	}

	public static PaginatedList<Group> getUserEvents(int userID, GroupsController.EventsType type, int offset, int count) throws SQLException{
		String query="SELECT %s FROM group_memberships JOIN `groups` ON group_id=`groups`.id WHERE user_id=? AND accepted=1 AND `groups`.`type`=1";
		query+=switch(type){
			case PAST -> " AND event_start_time<=CURRENT_TIMESTAMP()";
			case FUTURE -> " AND event_start_time>CURRENT_TIMESTAMP()";
			case ALL -> "";
		};
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, String.format(Locale.US, query, "COUNT(*)"), userID);
		int total=DatabaseUtils.oneFieldToInt(stmt.executeQuery());
		if(total==0)
			return PaginatedList.emptyList(count);
		query+=" ORDER BY event_start_time "+(type==GroupsController.EventsType.PAST ? "DESC" : "ASC")+" LIMIT ? OFFSET ?";
		stmt=SQLQueryBuilder.prepareStatement(conn, String.format(Locale.US, query, "group_id"), userID, count, offset);
		try(ResultSet res=stmt.executeQuery()){
			return new PaginatedList<>(getByIdAsList(DatabaseUtils.intResultSetToList(res)), total, offset, count);
		}
	}

	public static PaginatedList<URI> getUserGroupIDs(int userID, int offset, int count) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT count(*) FROM group_memberships WHERE user_id=? AND accepted=1");
		stmt.setInt(1, userID);
		int total=DatabaseUtils.oneFieldToInt(stmt.executeQuery());
		if(total==0)
			return new PaginatedList<>(Collections.emptyList(), 0);

		stmt=conn.prepareStatement("SELECT group_id, ap_id FROM group_memberships JOIN groups ON group_id=id WHERE user_id=? AND accepted=1 LIMIT ? OFFSET ?");
		stmt.setInt(1, userID);
		stmt.setInt(2, count);
		stmt.setInt(3, offset);
		try(ResultSet res=stmt.executeQuery()){
			ArrayList<URI> list=new ArrayList<>();
			res.beforeFirst();
			while(res.next()){
				String apID=res.getString(2);
				list.add(apID!=null ? URI.create(apID) : Config.localURI("/groups/"+res.getInt(1)));
			}
			return new PaginatedList<>(list, total);
		}
	}

	public static PaginatedList<Group> getUserManagedGroups(int userID, int offset, int count) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=new SQLQueryBuilder(conn)
				.selectFrom("group_admins")
				.count()
				.where("user_id=?", userID)
				.createStatement();
		int total=DatabaseUtils.oneFieldToInt(stmt.executeQuery());
		if(total==0)
			return PaginatedList.emptyList(count);
		stmt=new SQLQueryBuilder(conn).selectFrom("group_admins").columns("group_id").where("user_id=?", userID).createStatement();
		try(ResultSet res=stmt.executeQuery()){
			return new PaginatedList<>(getByIdAsList(DatabaseUtils.intResultSetToList(res)), total, offset, count);
		}
	}

	public static PaginatedList<URI> getGroupMemberURIs(int groupID, boolean tentative, int offset, int count) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt;
		stmt=new SQLQueryBuilder(conn).selectFrom("group_memberships").count().where("group_id=? AND accepted=1 AND tentative=?", groupID, tentative).createStatement();
		int total=DatabaseUtils.oneFieldToInt(stmt.executeQuery());
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
			return new PaginatedList<>(list, total, offset, count);
		}
		return new PaginatedList<>(Collections.emptyList(), total, offset, count);
	}

	public static List<URI> getGroupMemberInboxes(int groupID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT DISTINCT IFNULL(ap_shared_inbox, ap_inbox) FROM `users` WHERE id IN (SELECT user_id FROM group_memberships WHERE group_id=? AND accepted=1) AND ap_inbox IS NOT NULL");
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
				.where("group_id=?", groupID)
				.orderBy("display_order ASC");
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

	public static GroupAdmin getGroupAdmin(int groupID, int userID) throws SQLException{
		SQLQueryBuilder b=new SQLQueryBuilder()
				.selectFrom("group_admins")
				.columns("level", "user_id", "title")
				.where("group_id=? AND user_id=?", groupID, userID);
		try(ResultSet res=b.createStatement().executeQuery()){
			ArrayList<GroupAdmin> admins=new ArrayList<>();
			if(res.first()){
				GroupAdmin admin=new GroupAdmin();
				admin.level=Group.AdminLevel.values()[res.getInt(1)];
				admin.user=UserStorage.getById(res.getInt(2));
				admin.title=res.getString(3);
				return admin;
			}
			return null;
		}
	}

	public static void addOrUpdateGroupAdmin(int groupID, int userID, String title, @Nullable Group.AdminLevel level) throws SQLException{
		synchronized(adminUpdateLock){
			GroupAdmin existing=getGroupAdmin(groupID, userID);
			if(existing!=null){
				SQLQueryBuilder b=new SQLQueryBuilder()
						.update("group_admins")
						.where("group_id=? AND user_id=?", groupID, userID)
						.value("title", title);
				if(existing.level!=Group.AdminLevel.OWNER && level!=null){
					b.value("level", level.ordinal());
				}
				b.createStatement().execute();
			}else if(level!=null){
				int order=DatabaseUtils.intResultSetToList(new SQLQueryBuilder()
						.selectFrom("group_admins")
						.selectExpr("MAX(display_order)+1")
						.where("group_id=?", groupID)
						.createStatement()
						.executeQuery()).get(0);
				new SQLQueryBuilder()
						.insertInto("group_admins")
						.value("group_id", groupID)
						.value("user_id", userID)
						.value("title", title)
						.value("level", level.ordinal())
						.value("display_order", order)
						.createStatement()
						.execute();
			}
			SessionStorage.removeFromUserPermissionsCache(userID);
		}
	}

	public static void removeGroupAdmin(int groupID, int userID) throws SQLException{
		synchronized(adminUpdateLock){
			Connection conn=DatabaseConnectionManager.getConnection();
			PreparedStatement stmt=new SQLQueryBuilder(conn)
					.selectFrom("group_admins")
					.columns("display_order")
					.where("group_id=? AND user_id=?", groupID, userID)
					.createStatement();
			List<Integer> _order=DatabaseUtils.intResultSetToList(stmt.executeQuery());
			if(_order.isEmpty())
				return;
			int order=_order.get(0);
			new SQLQueryBuilder(conn)
					.deleteFrom("group_admins")
					.where("group_id=? AND user_id=?", groupID, userID)
					.createStatement()
					.execute();
			new SQLQueryBuilder(conn)
					.update("group_admins")
					.valueExpr("display_order", "display_order-1")
					.where("group_id=? AND display_order>?", groupID, order)
					.createStatement()
					.execute();
			SessionStorage.removeFromUserPermissionsCache(userID);
		}
	}

	public static void setGroupAdminOrder(int groupID, int userID, int newOrder) throws SQLException{
		synchronized(adminUpdateLock){
			Connection conn=DatabaseConnectionManager.getConnection();
			PreparedStatement stmt=new SQLQueryBuilder(conn)
					.selectFrom("group_admins")
					.columns("display_order")
					.where("group_id=? AND user_id=?", groupID, userID)
					.createStatement();
			int order=DatabaseUtils.oneFieldToInt(stmt.executeQuery());
			if(order==-1 || order==newOrder)
				return;
			int count=DatabaseUtils.oneFieldToInt(new SQLQueryBuilder(conn).selectFrom("group_admins").count().where("group_id=?", groupID).createStatement().executeQuery());
			if(newOrder>=count)
				return;
			new SQLQueryBuilder(conn)
					.update("group_admins")
					.where("group_id=? AND user_id=?", groupID, userID)
					.value("display_order", newOrder)
					.createStatement()
					.execute();
			if(newOrder<order){
				new SQLQueryBuilder(conn)
						.update("group_admins")
						.where("group_id=? AND display_order>=? AND display_order<? AND user_id<>?", groupID, newOrder, order, userID)
						.valueExpr("display_order", "display_order+1")
						.createStatement()
						.execute();
			}else{
				new SQLQueryBuilder(conn)
						.update("group_admins")
						.where("group_id=? AND display_order<=? AND display_order>? AND user_id<>?", groupID, newOrder, order, userID)
						.valueExpr("display_order", "display_order-1")
						.createStatement()
						.execute();
			}
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

	public static void updateGroupGeneralInfo(Group group, String name, String aboutSrc, String about, Instant eventStart, Instant eventEnd) throws SQLException{
		new SQLQueryBuilder()
				.update("groups")
				.value("name", name)
				.value("about_source", aboutSrc)
				.value("about", about)
				.value("event_start_time", eventStart)
				.value("event_end_time", eventEnd)
				.where("id=?", group.id)
				.createStatement()
				.execute();

		group.name=name;
		new SQLQueryBuilder()
				.update("qsearch_index")
				.value("string", getQSearchStringForGroup(group))
				.where("group_id=?", group.id)
				.createStatement()
				.execute();

		synchronized(GroupStorage.class){
			removeFromCache(group);
		}
	}

	public static boolean isUserBlocked(int ownerID, int targetID) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("blocks_group_user")
				.count()
				.where("owner_id=? AND user_id=?", ownerID, targetID)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			res.first();
			return res.getInt(1)==1;
		}
	}

	public static void blockUser(int selfID, int targetID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		new SQLQueryBuilder(conn)
				.insertInto("blocks_group_user")
				.value("owner_id", selfID)
				.value("user_id", targetID)
				.createStatement()
				.execute();
		new SQLQueryBuilder(conn)
				.deleteFrom("group_memberships")
				.where("user_id=? AND group_id=?", targetID, selfID)
				.createStatement()
				.execute();
	}

	public static void unblockUser(int selfID, int targetID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("blocks_group_user")
				.where("owner_id=? AND user_id=?", selfID, targetID)
				.createStatement()
				.execute();
	}

	public static List<User> getBlockedUsers(int selfID) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("blocks_group_user")
				.columns("user_id")
				.where("owner_id=?", selfID)
				.createStatement();
		return UserStorage.getByIdAsList(DatabaseUtils.intResultSetToList(stmt.executeQuery()));
	}

	public static boolean isDomainBlocked(int selfID, String domain) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("blocks_group_domain")
				.count()
				.where("owner_id=? AND domain=?", selfID, domain)
				.createStatement();
		return DatabaseUtils.oneFieldToInt(stmt.executeQuery())==1;
	}

	public static List<String> getBlockedDomains(int selfID) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("blocks_group_domain")
				.columns("domain")
				.where("owner_id=?", selfID)
				.createStatement();
		try(ResultSet res=stmt.executeQuery()){
			ArrayList<String> arr=new ArrayList<>();
			res.beforeFirst();
			while(res.next()){
				arr.add(res.getString(1));
			}
			return arr;
		}
	}

	public static void blockDomain(int selfID, String domain) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("blocks_group_domain")
				.value("owner_id", selfID)
				.value("domain", domain)
				.createStatement()
				.execute();
	}

	public static void unblockDomain(int selfID, String domain) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("blocks_group_domain")
				.where("owner_id=? AND domain=?", selfID, domain)
				.createStatement()
				.execute();
	}

	public static int getLocalGroupCount() throws SQLException{
		ResultSet res=new SQLQueryBuilder()
				.selectFrom("groups")
				.count()
				.where("domain=''")
				.createStatement()
				.executeQuery();
		return DatabaseUtils.oneFieldToInt(res);
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

	static String getQSearchStringForGroup(Group group){
		String s=Utils.transliterate(group.name)+" "+group.username;
		if(group.domain!=null)
			s+=" "+group.domain;
		return s;
	}

	public static List<Group> getUpcomingEvents(int userID) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT group_id, event_start_time FROM group_memberships JOIN `groups` ON `groups`.id=group_memberships.group_id WHERE user_id=? AND accepted=1 AND `groups`.type=1 AND event_start_time>NOW() AND event_start_time<DATE_ADD(NOW(), INTERVAL 2 DAY)", userID);
		return getByIdAsList(DatabaseUtils.intResultSetToList(stmt.executeQuery()));
	}

	public static IntStream getAllMembersAsStream(int groupID) throws SQLException{
		PreparedStatement stmt=new SQLQueryBuilder()
				.selectFrom("group_memberships")
				.columns("user_id")
				.where("accepted=1 AND group_id=?", groupID)
				.createStatement();
		return DatabaseUtils.intResultSetToStream(stmt.executeQuery());
	}
}
