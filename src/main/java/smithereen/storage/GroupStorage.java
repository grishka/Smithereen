package smithereen.storage;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
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
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import smithereen.Config;
import smithereen.LruCache;
import smithereen.Utils;
import smithereen.activitypub.SerializerContext;
import smithereen.activitypub.objects.Actor;
import smithereen.activitypub.objects.LocalImage;
import smithereen.controllers.GroupsController;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.model.ForeignGroup;
import smithereen.model.Group;
import smithereen.model.groups.GroupAdmin;
import smithereen.model.groups.GroupInvitation;
import smithereen.model.PaginatedList;
import smithereen.model.User;
import smithereen.model.groups.GroupLink;
import smithereen.model.notifications.UserNotifications;
import smithereen.model.media.MediaFileRecord;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.storage.utils.IntPair;
import smithereen.storage.utils.Pair;
import smithereen.text.TextProcessor;
import smithereen.util.NamedMutexCollection;
import spark.utils.StringUtils;

public class GroupStorage{
	private static final Logger LOG=LoggerFactory.getLogger(GroupStorage.class);

	private static final LruCache<Integer, Group> cacheByID=new LruCache<>(500);
	private static final LruCache<String, Group> cacheByUsername=new LruCache<>(500);
	private static final LruCache<URI, ForeignGroup> cacheByActivityPubID=new LruCache<>(500);

	private static final Object adminUpdateLock=new Object();
	private static final NamedMutexCollection foreignGroupUpdateLocks=new NamedMutexCollection();

	public static int createGroup(String name, String description, String descriptionSrc, int userID, boolean isEvent, Instant eventStart, Instant eventEnd) throws SQLException{
		int id;
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			try{
				conn.createStatement().execute("START TRANSACTION");

				KeyPairGenerator kpg=KeyPairGenerator.getInstance("RSA");
				kpg.initialize(2048);
				KeyPair pair=kpg.generateKeyPair();

				String username;
				id=new SQLQueryBuilder(conn)
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
						.value("type", isEvent ? Group.Type.EVENT : Group.Type.GROUP)
						.executeAndGetID();

				username=(isEvent ? "event" : "club")+id;

				new SQLQueryBuilder(conn)
						.update("groups")
						.value("username", username)
						.where("id=?", id)
						.executeNoResult();

				new SQLQueryBuilder(conn)
						.insertInto("group_memberships")
						.value("user_id", userID)
						.value("group_id", id)
						.executeNoResult();

				new SQLQueryBuilder(conn)
						.insertInto("group_admins")
						.value("user_id", userID)
						.value("group_id", id)
						.value("level", Group.AdminLevel.OWNER)
						.executeNoResult();

				new SQLQueryBuilder(conn)
						.insertInto("qsearch_index")
						.value("string", TextProcessor.transliterate(name)+" "+username)
						.value("group_id", id)
						.executeNoResult();
			}catch(Exception x){
				conn.createStatement().execute("ROLLBACK");
				throw new SQLException(x);
			}
			SessionStorage.removeFromUserPermissionsCache(userID);
			conn.createStatement().execute("COMMIT");
		}
		return id;
	}

	public static void putOrUpdateForeignGroup(ForeignGroup group) throws SQLException{
		String key=group.activityPubID.toString().toLowerCase();
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			foreignGroupUpdateLocks.acquire(key);
			final int existingGroupID=new SQLQueryBuilder(conn)
					.selectFrom("groups")
					.columns("id")
					.where("ap_id=?", group.activityPubID.toString())
					.executeAndGetInt();

			SQLQueryBuilder builder=new SQLQueryBuilder(conn);
			if(existingGroupID==-1){
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
					.value("public_key", group.publicKey.getEncoded())
					.value("avatar", group.hasAvatar() ? group.icon.get(0).asActivityPubObject(new JsonObject(), new SerializerContext(null, (String)null)).toString() : null)
					.value("event_start_time", group.eventStartTime)
					.value("event_end_time", group.eventEndTime)
					.value("type", group.type)
					.value("flags", Utils.serializeEnumSet(group.capabilities))
					.value("access_type", group.accessType)
					.value("endpoints", group.serializeEndpoints())
					.value("about", group.summary)
					.value("profile_fields", group.serializeProfileFields())
					.valueExpr("last_updated", "CURRENT_TIMESTAMP()");

			if(existingGroupID==-1){
				group.id=builder.executeAndGetID();
				new SQLQueryBuilder(conn)
						.insertInto("qsearch_index")
						.value("group_id", group.id)
						.value("string", getQSearchStringForGroup(group))
						.executeNoResult();
			}else{
				group.id=existingGroupID;
				builder.executeNoResult();
				new SQLQueryBuilder(conn)
						.update("qsearch_index")
						.value("string", getQSearchStringForGroup(group))
						.where("group_id=?", existingGroupID)
						.executeNoResult();
			}
			removeFromCache(group);
			synchronized(adminUpdateLock){
				builder=new SQLQueryBuilder(conn)
						.selectFrom("group_admins")
						.columns("user_id", "title")
						.where("group_id=?", group.id);
				Map<Integer, GroupAdmin> admins=group.adminsForActivityPub.stream().collect(Collectors.toMap(adm->adm.user.id, adm->adm));
				int count=0;
				boolean needUpdate=false;
				try(ResultSet res=builder.execute()){
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
							.executeNoResult();
					int order=0;
					for(GroupAdmin admin: group.adminsForActivityPub){
						new SQLQueryBuilder(conn)
								.insertInto("group_admins")
								.value("group_id", group.id)
								.value("user_id", admin.user.id)
								.value("title", admin.title)
								.value("level", Group.AdminLevel.MODERATOR.ordinal())
								.value("display_order", order)
								.executeNoResult();
						order++;
					}
				}


				List<GroupLink> existingLinks;
				if(existingGroupID==-1)
					existingLinks=List.of();
				else
					existingLinks=getGroupLinks(existingGroupID);
				if(!existingLinks.isEmpty() || !group.linksFromActivityPub.isEmpty()){
					Map<URI, GroupLink> existingLinksByID=existingLinks.stream().collect(Collectors.toMap(l->l.apID, Function.identity(), (a, b)->b));
					for(GroupLink l:group.linksFromActivityPub){
						GroupLink existingLink=existingLinksByID.get(l.apID);
						if(existingLink==null){
							new SQLQueryBuilder(conn)
									.insertInto("group_links")
									.value("group_id", group.id)
									.value("url", l.url.toString())
									.value("title", l.title)
									.value("object_type", l.object==null ? null : l.object.type().id)
									.value("object_id", l.object==null ? null : l.object.id())
									.value("ap_image_url", l.apImageURL==null ? null : l.apImageURL.toString())
									.value("display_order", l.displayOrder)
									.value("ap_id", l.apID.toString())
									.value("is_unresolved_ap_object", l.isUnresolvedActivityPubObject)
									.executeNoResult();
						}else if(!Objects.equals(l.title, existingLink.title) || l.displayOrder!=existingLink.displayOrder){
							new SQLQueryBuilder(conn)
									.update("group_links")
									.where("ap_id=? AND group_id=?", l.apID, group.id)
									.value("title", l.title)
									.value("display_order", l.displayOrder)
									.executeNoResult();
						}
					}
					Set<URI> newLinkIDs=group.linksFromActivityPub.stream().map(l->l.apID).collect(Collectors.toSet());
					HashSet<Long> removedLinkIDs=new HashSet<>();
					for(GroupLink l:existingLinks){
						if(!newLinkIDs.contains(l.apID))
							removedLinkIDs.add(l.id);
					}
					if(!removedLinkIDs.isEmpty()){
						new SQLQueryBuilder(conn)
								.deleteFrom("group_links")
								.whereIn("id", removedLinkIDs)
								.andWhere("group_id=?", group.id)
								.executeNoResult();
					}
				}
			}
		}catch(SQLIntegrityConstraintViolationException x){
			// Rare case: group with a matching username@domain but a different AP ID already exists
			try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
				int oldID=new SQLQueryBuilder(conn)
						.selectFrom("groups")
						.columns("id")
						.where("username=? AND domain=? AND ap_id<>?", group.username, group.domain, group.activityPubID)
						.executeAndGetInt();
				if(oldID<=0){
					LOG.warn("Didn't find an existing group with username {}@{} while trying to deduplicate {}", group.username, group.domain, group.activityPubID);
					throw x;
				}
				LOG.info("Deduplicating group rows: username {}@{}, old local ID {}, new AP ID {}", group.username, group.domain, oldID, group.activityPubID);
				// Assign a temporary random username to this existing group row to get it out of the way
				new SQLQueryBuilder(conn)
						.update("groups")
						.value("username", UUID.randomUUID().toString())
						.where("id=?", oldID)
						.executeNoResult();
				// Try again
				putOrUpdateForeignGroup(group);
			}
		}finally{
			foreignGroupUpdateLocks.release(key);
		}
	}

	public static Group getById(int id) throws SQLException{
		Group g=cacheByID.get(id);
		if(g!=null)
			return g;
		g=new SQLQueryBuilder()
				.selectFrom("groups")
				.allColumns()
				.where("id=?", id)
				.executeAndGetSingleObject(Group::fromResultSet);
		if(g!=null){
			if(g.icon!=null && !g.icon.isEmpty() && g.icon.getFirst() instanceof LocalImage li){
				MediaFileRecord mfr=MediaStorage.getMediaFileRecord(li.fileID);
				if(mfr!=null)
					li.fillIn(mfr);
			}
			putIntoCache(g);
		}
		return g;
	}

	public static Group getByUsername(String username) throws SQLException{
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
		g=new SQLQueryBuilder()
				.selectFrom("groups")
				.allColumns()
				.where("username=? AND domain=?", username, domain)
				.executeAndGetSingleObject(Group::fromResultSet);
		if(g!=null){
			if(g.icon!=null && !g.icon.isEmpty() && g.icon.getFirst() instanceof LocalImage li){
				MediaFileRecord mfr=MediaStorage.getMediaFileRecord(li.fileID);
				if(mfr!=null)
					li.fillIn(mfr);
			}
			putIntoCache(g);
		}
		return g;
	}

	public static int getIdByUsername(@NotNull String username) throws SQLException{
		String domain;
		if(username.contains("@")){
			String[] parts=username.split("@", 2);
			username=parts[0];
			domain=parts[1];
		}else{
			domain="";
		}
		if(username.length()>Actor.USERNAME_MAX_LENGTH)
			username=username.substring(0, Actor.USERNAME_MAX_LENGTH);
		return new SQLQueryBuilder()
				.selectFrom("groups")
				.columns("id")
				.where("username=? AND domain=?", username, domain)
				.executeAndGetInt();
	}

	public static ForeignGroup getForeignGroupByActivityPubID(URI id) throws SQLException{
		ForeignGroup g=cacheByActivityPubID.get(id);
		if(g!=null)
			return g;
		g=new SQLQueryBuilder()
				.selectFrom("groups")
				.allColumns()
				.where("ap_id=?", id.toString())
				.executeAndGetSingleObject(ForeignGroup::fromResultSet);
		if(g!=null)
			putIntoCache(g);
		return g;
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
		Iterator<Integer> itr=ids.iterator();
		while(itr.hasNext()){
			Integer id=itr.next();
			Group group=cacheByID.get(id);
			if(group!=null){
				itr.remove();
				result.put(id, group);
			}
		}
		if(ids.isEmpty())
			return result;
		result.putAll(new SQLQueryBuilder()
				.selectFrom("groups")
				.allColumns()
				.whereIn("id", ids)
				.executeAsStream(res->{
					String domain=res.getString("domain");
					Group group;
					if(StringUtils.isNotEmpty(domain))
						group=ForeignGroup.fromResultSet(res);
					else
						group=Group.fromResultSet(res);
					return group;
				})
				.collect(Collectors.toMap(g->g.id, Function.identity())));
		Set<Long> needAvatars=result.values().stream()
				.map(g->g.icon!=null && !g.icon.isEmpty() && g.icon.getFirst() instanceof LocalImage li ? li : null)
				.filter(li->li!=null && li.fileRecord==null)
				.map(li->li.fileID)
				.collect(Collectors.toSet());
		if(!needAvatars.isEmpty()){
			Map<Long, MediaFileRecord> records=MediaStorage.getMediaFileRecords(needAvatars);
			for(Group group:result.values()){
				if(group.icon!=null && !group.icon.isEmpty() && group.icon.getFirst() instanceof LocalImage li && li.fileRecord==null){
					MediaFileRecord mfr=records.get(li.fileID);
					if(mfr!=null)
						li.fillIn(mfr);
				}
			}
		}
		for(Group g:result.values()){
			putIntoCache(g);
		}
		return result;
	}

	public static List<User> getRandomMembersForProfile(int groupID, boolean tentative) throws SQLException{
		return UserStorage.getByIdAsList(
				new SQLQueryBuilder()
						.selectFrom("group_memberships")
						.where("group_id=? AND tentative=? AND accepted=1", groupID, tentative)
						.orderBy("RAND()")
						.limit(6, 0)
						.executeAndGetIntList()
		);
	}

	public static PaginatedList<Integer> getMembers(int groupID, int offset, int count, @Nullable Boolean tentative) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			String _tentative=tentative==null ? "" : (" AND tentative="+(tentative ? '1' : '0'));
			int total=new SQLQueryBuilder(conn)
					.selectFrom("group_memberships")
					.count()
					.where("group_id=? AND accepted=1"+_tentative, groupID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Integer> ids=new SQLQueryBuilder(conn)
					.selectFrom("group_memberships")
					.where("group_id=? AND accepted=1"+_tentative, groupID)
					.limit(count, offset)
					.executeAndGetIntList();
			return new PaginatedList<>(ids, total, offset, count);
		}
	}

	public static Group.MembershipState getUserMembershipState(int groupID, int userID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			try(ResultSet res=new SQLQueryBuilder(conn).selectFrom("group_memberships").where("group_id=? AND user_id=?", groupID, userID).execute()){
				if(!res.next()){
					return new SQLQueryBuilder(conn).selectFrom("group_invites").count().where("group_id=? AND invitee_id=?", groupID, userID).executeAndGetInt()==0 ? Group.MembershipState.NONE : Group.MembershipState.INVITED;
				}
				if(!res.getBoolean("accepted"))
					return Group.MembershipState.REQUESTED;
				return res.getBoolean("tentative") ? Group.MembershipState.TENTATIVE_MEMBER : Group.MembershipState.MEMBER;
			}
		}
	}

	public static void joinGroup(Group group, int userID, boolean tentative, boolean accepted) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			DatabaseUtils.doWithTransaction(conn, ()->{
				new SQLQueryBuilder(conn)
						.insertInto("group_memberships")
						.value("user_id", userID)
						.value("group_id", group.id)
						.value("tentative", tentative)
						.value("accepted", accepted)
						.executeNoResult();

				if(accepted){
					String memberCountField=tentative ? "tentative_member_count" : "member_count";
					new SQLQueryBuilder(conn)
							.update("groups")
							.valueExpr(memberCountField, memberCountField+"+1")
							.where("id=?", group.id)
							.executeNoResult();
				}

				deleteInvitation(userID, group.id, group.isEvent());
				removeFromCache(group);
			});
		}
	}

	public static void updateUserEventDecision(Group group, int userID, boolean tentative) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			DatabaseUtils.doWithTransaction(conn, ()->{
				new SQLQueryBuilder(conn)
						.update("group_memberships")
						.where("user_id=? AND group_id=?", userID, group.id)
						.value("tentative", tentative)
						.executeNoResult();

				String memberCountFieldOld=tentative ? "member_count" : "tentative_member_count";
				String memberCountFieldNew=tentative ? "tentative_member_count" : "member_count";
				new SQLQueryBuilder(conn)
						.update("groups")
						.valueExpr(memberCountFieldOld, memberCountFieldOld+"-1")
						.valueExpr(memberCountFieldNew, memberCountFieldNew+"+1")
						.where("id=?", group.id)
						.executeNoResult();
				removeFromCache(group);
			});
		}
	}

	public static void leaveGroup(Group group, int userID, boolean tentative, boolean wasAccepted) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			DatabaseUtils.doWithTransaction(conn, ()->{
				new SQLQueryBuilder(conn)
						.deleteFrom("group_memberships")
						.where("user_id=? AND group_id=?", userID, group.id)
						.executeNoResult();

				if(wasAccepted){
					String memberCountField=tentative ? "tentative_member_count" : "member_count";
					new SQLQueryBuilder(conn)
							.update("groups")
							.valueExpr(memberCountField, "GREATEST(0, CAST("+memberCountField+" AS SIGNED)-1)")
							.where("id=?", group.id)
							.executeNoResult();
				}

				removeFromCache(group);
			});
		}
	}

	public static PaginatedList<Group> getUserGroups(int userID, int offset, int count, boolean includePrivate) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			String query="SELECT %s FROM group_memberships JOIN `groups` ON group_id=`groups`.id WHERE user_id=? AND accepted=1 AND `groups`.`type`=0";
			if(!includePrivate){
				query+=" AND `groups`.`access_type`<>2";
			}
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, String.format(Locale.US, query, "COUNT(*)"), userID);
			int total=DatabaseUtils.oneFieldToInt(stmt.executeQuery());
			if(total==0)
				return new PaginatedList<>(Collections.emptyList(), 0, 0, count);
			if(includePrivate)
				query+=" ORDER BY hints_rank DESC";
			else
				query+=" ORDER BY group_id ASC";
			query+=" LIMIT ? OFFSET ?";
			stmt=SQLQueryBuilder.prepareStatement(conn, String.format(Locale.US, query, "group_id"), userID, count, offset);
			try(ResultSet res=stmt.executeQuery()){
				return new PaginatedList<>(getByIdAsList(DatabaseUtils.intResultSetToList(res)), total, offset, count);
			}
		}
	}

	public static PaginatedList<Group> getUserEvents(int userID, GroupsController.EventsType type, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			String query="SELECT %s FROM group_memberships JOIN `groups` ON group_id=`groups`.id WHERE user_id=? AND accepted=1 AND `groups`.`type`=1";
			query+=switch(type){
				case PAST -> " AND event_start_time<=CURRENT_TIMESTAMP()";
				case FUTURE -> " AND event_start_time>CURRENT_TIMESTAMP()";
				case ALL -> "";
			};
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
	}

	public static PaginatedList<URI> getUserGroupIDs(int userID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=conn.prepareStatement("SELECT count(*) FROM group_memberships JOIN `groups` ON group_id=`groups`.id WHERE user_id=? AND accepted=1 AND `groups`.`type`=0 AND `groups`.`access_type`<>2");
			stmt.setInt(1, userID);
			int total=DatabaseUtils.oneFieldToInt(stmt.executeQuery());
			if(total==0)
				return new PaginatedList<>(Collections.emptyList(), 0);

			stmt=conn.prepareStatement("SELECT group_id, ap_id FROM group_memberships JOIN `groups` ON group_id=id WHERE user_id=? AND accepted=1 AND `groups`.`type`=0 AND `groups`.`access_type`<>2 LIMIT ? OFFSET ?");
			stmt.setInt(1, userID);
			stmt.setInt(2, count);
			stmt.setInt(3, offset);
			try(ResultSet res=stmt.executeQuery()){
				ArrayList<URI> list=new ArrayList<>();
				while(res.next()){
					String apID=res.getString(2);
					list.add(apID!=null ? URI.create(apID) : Config.localURI("/groups/"+res.getInt(1)));
				}
				return new PaginatedList<>(list, total);
			}
		}
	}

	public static PaginatedList<Group> getUserManagedGroups(int userID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("group_admins")
					.count()
					.where("user_id=?", userID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			try(ResultSet res=new SQLQueryBuilder(conn).selectFrom("group_admins").columns("group_id").where("user_id=?", userID).execute()){
				return new PaginatedList<>(getByIdAsList(DatabaseUtils.intResultSetToList(res)), total, offset, count);
			}
		}
	}

	public static PaginatedList<URI> getGroupMemberURIs(int groupID, boolean tentative, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn).selectFrom("group_memberships").count().where("group_id=? AND accepted=1 AND tentative=?", groupID, tentative).executeAndGetInt();
			if(count>0){
				PreparedStatement stmt=conn.prepareStatement("SELECT `ap_id`,`id` FROM `group_memberships` INNER JOIN `users` ON `users`.`id`=`user_id` WHERE `group_id`=? AND `accepted`=1 AND tentative=? LIMIT ? OFFSET ?");
				stmt.setInt(1, groupID);
				stmt.setBoolean(2, tentative);
				stmt.setInt(3, count);
				stmt.setInt(4, offset);
				ArrayList<URI> list=new ArrayList<>();
				try(ResultSet res=stmt.executeQuery()){
					while(res.next()){
						String _u=res.getString(1);
						if(_u==null){
							list.add(Config.localURI("/users/"+res.getInt(2)));
						}else{
							list.add(URI.create(_u));
						}
					}
				}
				return new PaginatedList<>(list, total, offset, count);
			}
			return new PaginatedList<>(Collections.emptyList(), total, offset, count);
		}
	}

	public static List<URI> getGroupMemberInboxes(int groupID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=conn.prepareStatement("SELECT DISTINCT IFNULL(ap_shared_inbox, ap_inbox) FROM `users` WHERE id IN (SELECT user_id FROM group_memberships WHERE group_id=? AND accepted=1) AND ap_inbox IS NOT NULL");
			stmt.setInt(1, groupID);
			ArrayList<URI> inboxes=new ArrayList<>();
			try(ResultSet res=stmt.executeQuery()){
				while(res.next()){
					inboxes.add(URI.create(res.getString(1)));
				}
				return inboxes;
			}
		}
	}

	public static Group.AdminLevel getGroupMemberAdminLevel(int groupID, int userID) throws SQLException{
		int level=new SQLQueryBuilder()
				.selectFrom("group_admins")
				.columns("level")
				.where("group_id=? AND user_id=?", groupID, userID)
				.executeAndGetInt();
		return level==-1 ? Group.AdminLevel.REGULAR : Group.AdminLevel.values()[level];
	}

	public static void setMemberAccepted(Group group, int userID, boolean accepted) throws SQLException{
		int groupID=group.id;
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int _tentative=new SQLQueryBuilder(conn)
					.selectFrom("group_memberships")
					.columns("tentative")
					.where("group_id=? AND user_id=? AND accepted=?", groupID, userID, !accepted)
					.executeAndGetInt();
			if(_tentative==-1)
				return;
			boolean tentative=_tentative==1;

			new SQLQueryBuilder(conn)
					.update("group_memberships")
					.value("accepted", accepted)
					.where("group_id=? AND user_id=?", groupID, userID)
					.executeNoResult();

			String memberCountField=tentative ? "tentative_member_count" : "member_count";
			new SQLQueryBuilder(conn)
					.update("groups")
					.valueExpr(memberCountField, "GREATEST(0, CAST("+memberCountField+" AS SIGNED)"+(accepted ? "+1" : "-1")+")")
					.where("id=?", groupID)
					.executeNoResult();
			removeFromCache(group);
		}
	}

	public static List<GroupAdmin> getGroupAdmins(int groupID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("group_admins")
				.columns("level", "user_id", "title")
				.where("group_id=?", groupID)
				.orderBy("display_order ASC")
				.executeAsStream(GroupAdmin::fromResultSet)
				.toList();
	}

	public static GroupAdmin getGroupAdmin(int groupID, int userID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("group_admins")
				.columns("level", "user_id", "title")
				.where("group_id=? AND user_id=?", groupID, userID)
				.executeAndGetSingleObject(GroupAdmin::fromResultSet);
	}

	public static void addOrUpdateGroupAdmin(int groupID, int userID, String title, @Nullable Group.AdminLevel level) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			synchronized(adminUpdateLock){
				GroupAdmin existing=getGroupAdmin(groupID, userID);
				if(existing!=null){
					SQLQueryBuilder b=new SQLQueryBuilder(conn)
							.update("group_admins")
							.where("group_id=? AND user_id=?", groupID, userID)
							.value("title", title);
					if(existing.level!=Group.AdminLevel.OWNER && level!=null){
						b.value("level", level.ordinal());
					}
					b.executeNoResult();
				}else if(level!=null){
					int order=new SQLQueryBuilder(conn)
							.selectFrom("group_admins")
							.selectExpr("MAX(display_order)+1")
							.where("group_id=?", groupID)
							.executeAndGetInt();
					new SQLQueryBuilder(conn)
							.insertInto("group_admins")
							.value("group_id", groupID)
							.value("user_id", userID)
							.value("title", title)
							.value("level", level.ordinal())
							.value("display_order", order)
							.executeNoResult();
				}
				SessionStorage.removeFromUserPermissionsCache(userID);
			}
		}
	}

	public static void removeGroupAdmin(int groupID, int userID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			synchronized(adminUpdateLock){
				int order=new SQLQueryBuilder(conn)
						.selectFrom("group_admins")
						.columns("display_order")
						.where("group_id=? AND user_id=?", groupID, userID)
						.executeAndGetInt();
				if(order==-1)
					return;
				new SQLQueryBuilder(conn)
						.deleteFrom("group_admins")
						.where("group_id=? AND user_id=?", groupID, userID)
						.executeNoResult();
				new SQLQueryBuilder(conn)
						.update("group_admins")
						.valueExpr("display_order", "display_order-1")
						.where("group_id=? AND display_order>?", groupID, order)
						.executeNoResult();
				SessionStorage.removeFromUserPermissionsCache(userID);
			}
		}
	}

	public static void setGroupAdminOrder(int groupID, int userID, int newOrder) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			synchronized(adminUpdateLock){
				int order=new SQLQueryBuilder(conn)
						.selectFrom("group_admins")
						.columns("display_order")
						.where("group_id=? AND user_id=?", groupID, userID)
						.executeAndGetInt();
				if(order==-1 || order==newOrder)
					return;
				int count=new SQLQueryBuilder(conn).selectFrom("group_admins").count().where("group_id=?", groupID).executeAndGetInt();
				if(newOrder>=count)
					return;
				new SQLQueryBuilder(conn)
						.update("group_admins")
						.where("group_id=? AND user_id=?", groupID, userID)
						.value("display_order", newOrder)
						.executeNoResult();
				if(newOrder<order){
					new SQLQueryBuilder(conn)
							.update("group_admins")
							.where("group_id=? AND display_order>=? AND display_order<? AND user_id<>?", groupID, newOrder, order, userID)
							.valueExpr("display_order", "display_order+1")
							.executeNoResult();
				}else{
					new SQLQueryBuilder(conn)
							.update("group_admins")
							.where("group_id=? AND display_order<=? AND display_order>? AND user_id<>?", groupID, newOrder, order, userID)
							.valueExpr("display_order", "display_order-1")
							.executeNoResult();
				}
			}
		}
	}

	public static void updateProfilePicture(Group group, String serializedPic) throws SQLException{
		new SQLQueryBuilder()
				.update("groups")
				.value("avatar", serializedPic)
				.where("id=?", group.id)
				.executeNoResult();
		removeFromCache(group);
	}

	public static void updateGroupGeneralInfo(Group group, String name, String username, String aboutSrc, String about, Instant eventStart, Instant eventEnd, Group.AccessType accessType) throws SQLException{
		new SQLQueryBuilder()
				.update("groups")
				.value("name", name)
				.value("username", username)
				.value("about_source", aboutSrc)
				.value("about", about)
				.value("event_start_time", eventStart)
				.value("event_end_time", eventEnd)
				.value("access_type", accessType)
				.where("id=?", group.id)
				.executeNoResult();

		group.name=name;
		new SQLQueryBuilder()
				.update("qsearch_index")
				.value("string", getQSearchStringForGroup(group))
				.where("group_id=?", group.id)
				.executeNoResult();

		removeFromCache(group);
	}

	public static void updateProfileFields(Group group) throws SQLException{
		new SQLQueryBuilder()
				.update("groups")
				.value("profile_fields", group.serializeProfileFields())
				.where("id=?", group.id)
				.executeNoResult();
		removeFromCache(group);
	}

	public static boolean isUserBlocked(int ownerID, int targetID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("blocks_group_user")
				.count()
				.where("owner_id=? AND user_id=?", ownerID, targetID)
				.executeAndGetInt()==1;
	}

	public static void blockUser(int selfID, int targetID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			new SQLQueryBuilder(conn)
					.insertIgnoreInto("blocks_group_user")
					.value("owner_id", selfID)
					.value("user_id", targetID)
					.executeNoResult();
			new SQLQueryBuilder(conn)
					.deleteFrom("group_memberships")
					.where("user_id=? AND group_id=?", targetID, selfID)
					.executeNoResult();
		}
	}

	public static void unblockUser(int selfID, int targetID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("blocks_group_user")
				.where("owner_id=? AND user_id=?", selfID, targetID)
				.executeNoResult();
	}

	public static List<User> getBlockedUsers(int selfID) throws SQLException{
		return UserStorage.getByIdAsList(new SQLQueryBuilder()
				.selectFrom("blocks_group_user")
				.columns("user_id")
				.where("owner_id=?", selfID)
				.executeAndGetIntList());
	}

	public static boolean isDomainBlocked(int selfID, String domain) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("blocks_group_domain")
				.count()
				.where("owner_id=? AND domain=?", selfID, domain)
				.executeAndGetInt()==1;
	}

	public static List<String> getBlockedDomains(int selfID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("blocks_group_domain")
				.columns("domain")
				.where("owner_id=?", selfID)
				.executeAsStream(r->r.getString(1))
				.toList();
	}

	public static void blockDomain(int selfID, String domain) throws SQLException{
		new SQLQueryBuilder()
				.insertIgnoreInto("blocks_group_domain")
				.value("owner_id", selfID)
				.value("domain", domain)
				.executeNoResult();
	}

	public static void unblockDomain(int selfID, String domain) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("blocks_group_domain")
				.where("owner_id=? AND domain=?", selfID, domain)
				.executeNoResult();
	}

	public static int getLocalGroupCount() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("groups")
				.count()
				.where("domain=''")
				.executeAndGetInt();
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
		String s=TextProcessor.transliterate(group.name)+" "+group.username;
		if(group.domain!=null)
			s+=" "+group.domain;
		return s;
	}

	public static List<Group> getUserEventsInTimeRange(int userID, Instant from, Instant to) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT group_id, event_start_time FROM group_memberships JOIN `groups` ON `groups`.id=group_memberships.group_id WHERE user_id=? AND accepted=1 AND `groups`.type=1 AND event_start_time>=? AND event_start_time<?", userID, from, to);
			return getByIdAsList(DatabaseUtils.intResultSetToList(stmt.executeQuery()));
		}
	}

	public static IntStream getAllMembersAsStream(int groupID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("group_memberships")
				.columns("user_id")
				.where("accepted=1 AND group_id=?", groupID)
				.executeAndGetIntStream();
	}

	public static int putInvitation(int groupID, int inviterID, int inviteeID, boolean isEvent, String apID) throws SQLException{
		return new SQLQueryBuilder()
				.insertInto("group_invites")
				.value("group_id", groupID)
				.value("inviter_id", inviterID)
				.value("invitee_id", inviteeID)
				.value("is_event", isEvent)
				.value("ap_id", apID)
				.executeAndGetID();
	}

	public static List<GroupInvitation> getUserInvitations(int userID, boolean isEvent, int offset, int count) throws SQLException{
		List<IntPair> ids=new SQLQueryBuilder()
				.selectFrom("group_invites")
				.columns("group_id", "inviter_id")
				.where("invitee_id=? AND is_event=?", userID, isEvent)
				.limit(count, offset)
				.executeAsStream(r->new IntPair(r.getInt(1), r.getInt(2)))
				.toList();
		Set<Integer> needGroups=ids.stream().map(IntPair::first).collect(Collectors.toSet());
		Set<Integer> needUsers=ids.stream().map(IntPair::second).collect(Collectors.toSet());
		Map<Integer, Group> groups=getById(needGroups);
		Map<Integer, User> users=UserStorage.getById(needUsers, false);
		// All groups and users must exist, this is taken care of by schema constraints
		return ids.stream().map(i->new GroupInvitation(groups.get(i.first()), users.get(i.second()))).collect(Collectors.toList());
	}

	public static Pair<Integer, URI> getInvitationInviterAndApID(int userID, int groupID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("group_invites")
				.columns("inviter_id", "ap_id")
				.where("invitee_id=? AND group_id=?", userID, groupID)
				.executeAndGetSingleObject(r->new Pair<>(r.getInt(1), r.getString(2)==null ? null : URI.create(r.getString(2))));
	}

	public static int deleteInvitation(int userID, int groupID, boolean isEvent) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int id=new SQLQueryBuilder(conn)
					.selectFrom("group_invites")
					.columns("id")
					.where("invitee_id=? AND group_id=?", userID, groupID)
					.executeAndGetInt();
			if(id<1)
				return id;
			int count=new SQLQueryBuilder(conn)
					.deleteFrom("group_invites")
					.where("id=?", id)
					.executeUpdate();
			if(count>0){
				UserNotifications notifications=NotificationsStorage.getNotificationsFromCache(userID);
				if(notifications!=null){
					if(isEvent)
						notifications.incNewEventInvitationsCount(-count);
					else
						notifications.incNewGroupInvitationsCount(-count);
				}
			}
			return id;
		}
	}

	public static PaginatedList<User> getGroupJoinRequests(int groupID, int offset, int count) throws SQLException{
		int total=getJoinRequestCount(groupID);
		if(total==0)
			return PaginatedList.emptyList(count);
		return new PaginatedList<>(UserStorage.getByIdAsList(new SQLQueryBuilder()
				.selectFrom("group_memberships")
				.columns("user_id")
				.where("group_id=? AND accepted=0", groupID)
				.orderBy("time DESC")
				.limit(count, offset)
				.executeAndGetIntList()), total, offset, count);
	}

	public static int getJoinRequestCount(int groupID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("group_memberships")
				.count()
				.where("group_id=? AND accepted=0", groupID)
				.executeAndGetInt();
	}

	public static PaginatedList<User> getGroupInvitations(int groupID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("group_invites")
					.count()
					.where("group_id=?", groupID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Integer> ids=new SQLQueryBuilder(conn)
					.selectFrom("group_invites")
					.columns("invitee_id")
					.where("group_id=?", groupID)
					.orderBy("id DESC")
					.limit(count, offset)
					.executeAndGetIntList();
			return new PaginatedList<>(UserStorage.getByIdAsList(ids), total, offset, count);
		}
	}

	public static boolean areThereGroupMembersWithDomain(int groupID, String domain) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT COUNT(*) FROM `group_memberships` JOIN `users` ON user_id=`users`.id WHERE group_id=? AND accepted=1 AND `users`.`domain`=?", groupID, domain);
			return DatabaseUtils.oneFieldToInt(stmt.executeQuery())>0;
		}
	}

	public static boolean areThereGroupInvitationsWithDomain(int groupID, String domain) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT COUNT(*) FROM `group_invites` JOIN `users` ON invitee_id=`users`.id WHERE group_id=? AND `users`.`domain`=?", groupID, domain);
			return DatabaseUtils.oneFieldToInt(stmt.executeQuery())>0;
		}
	}

	public static Map<URI, Integer> getMembersByActivityPubIDs(Collection<URI> ids, int groupID, boolean tentative) throws SQLException{
		if(ids.isEmpty())
			return Map.of();
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			ArrayList<Integer> localIDs=new ArrayList<>();
			ArrayList<String> remoteIDs=new ArrayList<>();
			for(URI id: ids){
				if(Config.isLocal(id)){
					String path=id.getPath();
					if(StringUtils.isEmpty(path))
						continue;
					String[] pathSegments=path.split("/");
					if(pathSegments.length!=3 || !"users".equals(pathSegments[1])) // "", "users", id
						continue;
					int uid=Utils.safeParseInt(pathSegments[2]);
					if(uid>0)
						localIDs.add(uid);
				}else{
					remoteIDs.add(id.toString());
				}
			}
			HashMap<Integer, URI> localIdToApIdMap=new HashMap<>();
			if(!remoteIDs.isEmpty()){
				try(ResultSet res=new SQLQueryBuilder(conn).selectFrom("users").columns("id", "ap_id").whereIn("ap_id", remoteIDs).execute()){
					while(res.next()){
						int localID=res.getInt(1);
						localIDs.add(localID);
						localIdToApIdMap.put(localID, URI.create(res.getString(2)));
					}
				}
			}
			if(localIDs.isEmpty())
				return Map.of();
			return new SQLQueryBuilder(conn)
					.selectFrom("group_memberships")
					.columns("user_id")
					.whereIn("user_id", localIDs)
					.andWhere("tentative=? AND accepted=1 AND group_id=?", tentative, groupID)
					.executeAsStream(res->res.getInt(1))
					.collect(Collectors.toMap(id->localIdToApIdMap.computeIfAbsent(id, GroupStorage::localUserURI), Function.identity()));
		}
	}

	public static Map<URI, Integer> getUserGroupsByActivityPubIDs(Collection<URI> ids, int userID) throws SQLException{
		if(ids.isEmpty())
			return Map.of();
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			ArrayList<Integer> localIDs=new ArrayList<>();
			ArrayList<String> remoteIDs=new ArrayList<>();
			for(URI id: ids){
				if(Config.isLocal(id)){
					String path=id.getPath();
					if(StringUtils.isEmpty(path))
						continue;
					String[] pathSegments=path.split("/");
					if(pathSegments.length!=3 || !"groups".equals(pathSegments[1])) // "", "groups", id
						continue;
					int uid=Utils.safeParseInt(pathSegments[2]);
					if(uid>0)
						localIDs.add(uid);
				}else{
					remoteIDs.add(id.toString());
				}
			}
			if(!localIDs.isEmpty()){
				// Filter local IDs to avoid returning private groups
				List<Integer> filteredLocalIDs=new SQLQueryBuilder(conn)
						.selectFrom("groups")
						.columns("id")
						.whereIn("id", localIDs)
						.andWhere("access_type<>"+Group.AccessType.PRIVATE)
						.executeAndGetIntList();
				localIDs.clear();
				localIDs.addAll(filteredLocalIDs);
			}
			HashMap<Integer, URI> localIdToApIdMap=new HashMap<>();
			if(!remoteIDs.isEmpty()){
				try(ResultSet res=new SQLQueryBuilder(conn).selectFrom("groups").columns("id", "ap_id").whereIn("ap_id", remoteIDs).andWhere("access_type<>"+Group.AccessType.PRIVATE.ordinal()).execute()){
					while(res.next()){
						int localID=res.getInt(1);
						localIDs.add(localID);
						localIdToApIdMap.put(localID, URI.create(res.getString(2)));
					}
				}
			}
			if(localIDs.isEmpty())
				return Map.of();
			return new SQLQueryBuilder(conn)
					.selectFrom("group_memberships")
					.columns("group_id")
					.whereIn("group_id", localIDs)
					.andWhere("accepted=1 AND user_id=?", userID)
					.executeAsStream(res->res.getInt(1))
					.collect(Collectors.toMap(id->localIdToApIdMap.computeIfAbsent(id, GroupStorage::localGroupURI), Function.identity()));
		}
	}

	private static URI localUserURI(int id){
		return Config.localURI("/users/"+id);
	}

	private static URI localGroupURI(int id){
		return Config.localURI("/groups/"+id);
	}

	public static void setMemberCount(Group group, int count, boolean tentative) throws SQLException{
		String field=tentative ? "tentative_member_count" : "member_count";
		new SQLQueryBuilder()
				.update("groups")
				.value(field, count)
				.where("id=?", group.id)
				.executeNoResult();
		removeFromCache(group);
	}

	public static int getLocalMembersCount(int groupID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT COUNT(*) FROM `group_memberships` JOIN `users` ON `user_id`=`users`.id WHERE group_id=? AND accepted=1 AND `users`.domain=''", groupID);
			return DatabaseUtils.oneFieldToInt(stmt.executeQuery());
		}
	}

	public static boolean incrementGroupHintsRank(int userID, int groupID, int amount) throws SQLException{
		return new SQLQueryBuilder()
				.update("group_memberships")
				.valueExpr("hints_rank", "hints_rank+?", amount*1000)
				.where("user_id=? AND group_id=?", userID, groupID)
				.executeUpdate()>0;
	}

	public static void normalizeGroupHintsRanksIfNeeded(Set<Integer> userIDs) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			List<Integer> filteredIDs=new SQLQueryBuilder(conn)
					.selectFrom("group_memberships")
					.columns("user_id")
					.whereIn("user_id", userIDs)
					.groupBy("user_id HAVING MAX(hints_rank)>500000")
					.executeAndGetIntList();
			for(int id:filteredIDs){
				new SQLQueryBuilder(conn)
						.update("group_memberships")
						.where("user_id=? AND mutual=1", id)
						.valueExpr("hints_rank", "FLOOR(hints_rank/2)")
						.executeNoResult();
			}
		}
	}

	// region Links

	public static long createLink(int groupID, String url, String title, ObjectLinkResolver.ObjectTypeAndID object, long imageID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int displayOrder=new SQLQueryBuilder(conn)
					.selectFrom("group_links")
					.selectExpr("IFNULL(MAX(display_order), -1)+1")
					.where("group_id=?", groupID)
					.executeAndGetInt();
			return new SQLQueryBuilder(conn)
					.insertInto("group_links")
					.value("group_id", groupID)
					.value("url", url)
					.value("title", title)
					.value("object_type", object==null ? null : object.type().id)
					.value("object_id", object==null ? null : object.id())
					.value("image_id", imageID==0 ? null : imageID)
					.value("display_order", displayOrder)
					.executeAndGetIDLong();
		}
	}

	public static List<GroupLink> getGroupLinks(int groupID) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			List<GroupLink> links=new SQLQueryBuilder(conn)
					.selectFrom("group_links")
					.where("group_id=?", groupID)
					.orderBy("display_order ASC")
					.executeAsStream(GroupLink::fromResultSet)
					.toList();
			Set<Long> needFiles=links.stream()
					.map(l->l.image instanceof LocalImage li ? li : null)
					.filter(Objects::nonNull)
					.map(li->li.fileID)
					.collect(Collectors.toSet());
			if(!needFiles.isEmpty()){
				Map<Long, MediaFileRecord> files=MediaStorage.getMediaFileRecords(needFiles);
				for(GroupLink link:links){
					if(link.image instanceof LocalImage li){
						MediaFileRecord mfr=files.get(li.fileID);
						if(mfr!=null)
							li.fillIn(mfr);
					}
				}
			}
			return links;
		}
	}

	public static GroupLink getGroupLink(int groupID, long linkID) throws SQLException{
		GroupLink link=new SQLQueryBuilder()
				.selectFrom("group_links")
				.where("group_id=? AND id=?", groupID, linkID)
				.executeAndGetSingleObject(GroupLink::fromResultSet);
		if(link!=null){
			if(link.image instanceof LocalImage li){
				MediaFileRecord mfr=MediaStorage.getMediaFileRecord(li.fileID);
				if(mfr!=null)
					li.fillIn(mfr);
			}
		}
		return link;
	}

	public static void setLinkOrder(int groupID, long linkID, int newOrder) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
//			synchronized(adminUpdateLock){
				int order=new SQLQueryBuilder(conn)
						.selectFrom("group_links")
						.columns("display_order")
						.where("group_id=? AND id=?", groupID, linkID)
						.executeAndGetInt();
				if(order==-1 || order==newOrder)
					return;
				int count=new SQLQueryBuilder(conn).selectFrom("group_links").count().where("group_id=?", groupID).executeAndGetInt();
				if(newOrder>=count)
					return;
				new SQLQueryBuilder(conn)
						.update("group_links")
						.where("group_id=? AND id=?", groupID, linkID)
						.value("display_order", newOrder)
						.executeNoResult();
				if(newOrder<order){
					new SQLQueryBuilder(conn)
							.update("group_links")
							.where("group_id=? AND display_order>=? AND display_order<? AND id<>?", groupID, newOrder, order, linkID)
							.valueExpr("display_order", "display_order+1")
							.executeNoResult();
				}else{
					new SQLQueryBuilder(conn)
							.update("group_links")
							.where("group_id=? AND display_order<=? AND display_order>? AND id<>?", groupID, newOrder, order, linkID)
							.valueExpr("display_order", "display_order-1")
							.executeNoResult();
				}
//			}
		}
	}

	public static void updateLinkTitle(int groupID, long linkID, String title) throws SQLException{
		new SQLQueryBuilder()
				.update("group_links")
				.where("group_id=? AND id=?", groupID, linkID)
				.value("title", title)
				.executeNoResult();
	}

	public static void deleteLink(int groupID, long linkID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("group_links")
				.where("group_id=? AND id=?", groupID, linkID)
				.executeNoResult();
	}

	// endregion
}
