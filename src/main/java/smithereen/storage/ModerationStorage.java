package smithereen.storage;

import com.google.gson.JsonObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import smithereen.Utils;
import smithereen.model.ServerAnnouncement;
import smithereen.model.ServerRule;
import smithereen.model.admin.ActorStaffNote;
import smithereen.model.admin.AuditLogEntry;
import smithereen.model.admin.EmailDomainBlockRule;
import smithereen.model.admin.EmailDomainBlockRuleFull;
import smithereen.model.admin.GroupActionLogAction;
import smithereen.model.admin.GroupActionLogEntry;
import smithereen.model.admin.IPBlockRule;
import smithereen.model.admin.IPBlockRuleFull;
import smithereen.model.PaginatedList;
import smithereen.model.SignupInvitation;
import smithereen.model.UserBanStatus;
import smithereen.model.admin.UserRole;
import smithereen.model.admin.ViolationReport;
import smithereen.model.admin.ViolationReportAction;
import smithereen.model.admin.UserActionLogAction;
import smithereen.model.viewmodel.AdminUserViewModel;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.storage.utils.IntPair;
import smithereen.storage.utils.Pair;
import smithereen.text.TextProcessor;
import smithereen.util.InetAddressRange;
import spark.utils.StringUtils;

public class ModerationStorage{
	public static int createViolationReport(int reporterID, int targetID, String comment, String otherServerDomain, String contentJson, boolean hasFileRefs, ViolationReport.Reason reason, Set<Integer> rules) throws SQLException{
		SQLQueryBuilder bldr=new SQLQueryBuilder()
				.insertInto("reports")
				.value("reporter_id", reporterID!=0 ? reporterID : null)
				.value("target_id", targetID)
				.value("comment", comment)
				.value("server_domain", otherServerDomain)
				.value("content", contentJson)
				.value("has_file_refs", hasFileRefs)
				.value("reason", reason)
				.value("rules", Utils.serializeIntList(rules));
		return bldr.executeAndGetID();
	}

	public static int getViolationReportsCount(boolean open) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("reports")
				.count()
				.where("state"+(open ? "=" : "<>")+" ?", ViolationReport.State.OPEN)
				.executeAndGetInt();
	}

	public static PaginatedList<ViolationReport> getViolationReports(boolean open, int offset, int count) throws SQLException{
		int total=getViolationReportsCount(open);
		if(total==0)
			return PaginatedList.emptyList(count);
		List<ViolationReport> reports=new SQLQueryBuilder()
				.selectFrom("reports")
				.allColumns()
				.where("state"+(open ? "=" : "<>")+" ?", ViolationReport.State.OPEN)
				.limit(count, offset)
				.orderBy("id DESC")
				.executeAsStream(ViolationReport::fromResultSet)
				.toList();
		return new PaginatedList<>(reports, total, offset, count);
	}

	public static List<ViolationReport> getResolvedViolationReportsWithFiles() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("reports")
				.allColumns()
				.where("state<>? AND has_file_refs=1", ViolationReport.State.OPEN)
				.executeAsStream(ViolationReport::fromResultSet)
				.toList();
	}

	public static void setViolationReportHasFileRefs(int id, boolean hasRefs) throws SQLException{
		new SQLQueryBuilder()
				.update("reports")
				.where("id=?", id)
				.value("has_file_refs", hasRefs)
				.executeNoResult();
	}

	public static Map<Integer, Instant> getViolationReportLastActionTimes(Collection<Integer> ids) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("report_actions")
				.selectExpr("report_id, MAX(time) AS t")
				.whereIn("report_id", ids)
				.andWhere("action_type<>?", ViolationReportAction.ActionType.COMMENT)
				.groupBy("report_id")
				.executeAsStream(r->new Pair<>(r.getInt(1), DatabaseUtils.getInstant(r, "t")))
				.collect(Collectors.toMap(Pair::first, Pair::second));
	}

	public static PaginatedList<ViolationReport> getViolationReportsOfActor(int actorID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("reports")
					.count()
					.where("target_id=?", actorID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<ViolationReport> reports=new SQLQueryBuilder(conn)
					.selectFrom("reports")
					.allColumns()
					.where("target_id=?", actorID)
					.limit(count, offset)
					.orderBy("id DESC")
					.executeAsStream(ViolationReport::fromResultSet)
					.toList();
			return new PaginatedList<>(reports, total, offset, count);
		}
	}

	public static PaginatedList<ViolationReport> getViolationReportsByUser(int userID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("reports")
					.count()
					.where("reporter_id=?", userID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<ViolationReport> reports=new SQLQueryBuilder(conn)
					.selectFrom("reports")
					.allColumns()
					.where("reporter_id=?", userID)
					.limit(count, offset)
					.orderBy("id DESC")
					.executeAsStream(ViolationReport::fromResultSet)
					.toList();
			return new PaginatedList<>(reports, total, offset, count);
		}
	}

	public static ViolationReport getViolationReportByID(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("reports")
				.allColumns()
				.where("id=?", id)
				.executeAndGetSingleObject(ViolationReport::fromResultSet);
	}

	public static void setViolationReportReason(int id, ViolationReport.Reason reason) throws SQLException{
		new SQLQueryBuilder()
				.update("reports")
				.value("reason", reason)
				.value("rules", null)
				.where("id=?", id)
				.executeNoResult();
	}

	public static void setViolationReportRules(int id, Set<Integer> rules) throws SQLException{
		new SQLQueryBuilder()
				.update("reports")
				.value("rules", Utils.serializeIntList(rules))
				.where("id=?", id)
				.executeNoResult();;
	}

	public static void updateViolationReportContent(int id, String contentJson, boolean hasFileRefs) throws SQLException{
		new SQLQueryBuilder()
				.update("reports")
				.value("content", contentJson)
				.value("has_file_refs", hasFileRefs)
				.where("id=?", id)
				.executeNoResult();
	}

	public static ViolationReportAction getViolationReportActionByID(int reportID, int actionID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("report_actions")
				.where("report_id=? AND id=?", reportID, actionID)
				.executeAndGetSingleObject(ViolationReportAction::fromResultSet);
	}

	public static void setServerRestriction(int id, String restrictionJson) throws SQLException{
		new SQLQueryBuilder()
				.update("servers")
				.value("restriction", restrictionJson)
				.value("is_restricted", restrictionJson!=null)
				.where("id=?", id)
				.executeNoResult();
	}

	public static Map<Integer, Integer> getRoleAccountCounts() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("accounts")
				.selectExpr("role, count(*)")
				.where("role IS NOT NULL")
				.groupBy("role")
				.executeAsStream(rs->new IntPair(rs.getInt(1), rs.getInt(2)))
				.collect(Collectors.toMap(IntPair::first, IntPair::second));
	}

	public static void updateRole(int id, String name, EnumSet<UserRole.Permission> permissions) throws SQLException{
		new SQLQueryBuilder()
				.update("user_roles")
				.value("name", name)
				.value("permissions", Utils.serializeEnumSetToBytes(permissions))
				.where("id=?", id)
				.executeNoResult();
	}

	public static int createRole(String name, EnumSet<UserRole.Permission> permissions) throws SQLException{
		return new SQLQueryBuilder()
				.insertInto("user_roles")
				.value("name", name)
				.value("permissions", Utils.serializeEnumSetToBytes(permissions))
				.executeAndGetID();
	}

	public static void deleteRole(int id) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("user_roles")
				.where("id=?", id)
				.executeNoResult();
	}

	public static void createAuditLogEntry(int adminID, AuditLogEntry.Action action, int ownerID, long objectID, AuditLogEntry.ObjectType objectType, Map<String, Object> extra) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("audit_log")
				.value("admin_id", adminID)
				.value("action", action)
				.value("owner_id", ownerID)
				.value("object_id", objectID)
				.value("object_type", objectType)
				.value("extra", extra==null ? null : Utils.gson.toJson(extra))
				.executeNoResult();
	}

	public static PaginatedList<AuditLogEntry> getGlobalAuditLog(int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("audit_log")
					.count()
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			return new PaginatedList<>(new SQLQueryBuilder(conn)
					.selectFrom("audit_log")
					.allColumns()
					.orderBy("id DESC")
					.limit(count, offset)
					.executeAsStream(AuditLogEntry::fromResultSet)
					.toList(), total, offset, count);
		}
	}

	public static PaginatedList<AuditLogEntry> getActorAuditLog(int actorID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("audit_log")
					.count()
					.where("owner_id=?", actorID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			return new PaginatedList<>(new SQLQueryBuilder(conn)
					.selectFrom("audit_log")
					.allColumns()
					.where("owner_id=?", actorID)
					.orderBy("id DESC")
					.limit(count, offset)
					.executeAsStream(AuditLogEntry::fromResultSet)
					.toList(), total, offset, count);
		}
	}

	public static PaginatedList<AdminUserViewModel> getUsers(String q, Boolean localOnly, String emailDomain, InetAddressRange ipRange, int role, UserBanStatus banStatus, boolean remoteSuspended, int offset, int count) throws SQLException{
		if(StringUtils.isNotEmpty(q)){
			q=Arrays.stream(TextProcessor.transliterate(q).replaceAll("[()\\[\\]*+~<>\\\"@-]", " ").split("[ \t]+")).filter(Predicate.not(String::isBlank)).map(s->'+'+s+'*').collect(Collectors.joining(" "));
		}
		ArrayList<String> whereParts=new ArrayList<>();
		ArrayList<Object> whereArgs=new ArrayList<>();
		String selection="`users`.id AS user_id, accounts.id AS account_id, accounts.role, accounts.last_active, accounts.email, accounts.activation_info, accounts.last_ip";
		String query=" FROM `users` LEFT JOIN accounts ON users.id=accounts.user_id";
		if(StringUtils.isNotEmpty(q)){
			query+=" JOIN qsearch_index ON `users`.id=qsearch_index.user_id";
			whereParts.add("MATCH (qsearch_index.`string`) AGAINST (? IN BOOLEAN MODE)");
			whereArgs.add(q);
		}
		if(localOnly!=null){
			if(localOnly)
				whereParts.add("`users`.ap_id IS NULL");
			else
				whereParts.add("`users`.ap_id IS NOT NULL");
		}
		if(StringUtils.isNotEmpty(emailDomain)){
			whereParts.add("accounts.email_domain=?");
			whereArgs.add(emailDomain);
		}
		if(ipRange!=null){
			if(ipRange.isSingleAddress()){
				whereParts.add("accounts.last_ip=?");
				whereArgs.add(Utils.serializeInetAddress(ipRange.address()));
			}else{
				whereParts.add("accounts.last_ip>=? AND accounts.last_ip<?");
				whereArgs.add(Utils.serializeInetAddress(ipRange.getMinAddress()));
				whereArgs.add(Utils.serializeInetAddress(ipRange.getMaxAddress()));
			}
		}
		if(role>0){
			whereParts.add("accounts.role=?");
			whereArgs.add(role);
		}
		if(banStatus!=null){
			whereParts.add("`users`.ban_status=?");
			whereArgs.add(banStatus);
		}else if(remoteSuspended){
			whereParts.add("`users`.ap_id IS NOT NULL AND `users`.ban_info IS NOT NULL AND `users`.ban_info->'$.suspendedOnRemoteServer'=true");
		}
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			String where;
			if(whereParts.isEmpty())
				where="";
			else
				where=" WHERE ("+String.join(") AND (", whereParts)+")";
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT COUNT(*)"+query+where, whereArgs.toArray(new Object[0]));
			int total;
			try(ResultSet res=stmt.executeQuery()){
				total=DatabaseUtils.oneFieldToInt(res);
			}
			if(total==0)
				return PaginatedList.emptyList(count);
			whereArgs.add(count);
			whereArgs.add(offset);
			stmt=SQLQueryBuilder.prepareStatement(conn, "SELECT "+selection+query+where+" ORDER BY `users`.id ASC LIMIT ? OFFSET ?", whereArgs.toArray(new Object[0]));
			try(ResultSet res=stmt.executeQuery()){
				List<AdminUserViewModel> list=DatabaseUtils.resultSetToObjectStream(res, AdminUserViewModel::fromResultSet, null)
						.toList();
				return new PaginatedList<>(list, total, offset, count);
			}
		}
	}

	public static List<ViolationReportAction> getViolationReportActions(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("report_actions")
				.allColumns()
				.where("report_id=?", id)
				.orderBy("id ASC")
				.executeAsStream(ViolationReportAction::fromResultSet)
				.toList();
	}

	public static void createViolationReportAction(int reportID, int userID, ViolationReportAction.ActionType type, String text, JsonObject extra) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("report_actions")
				.value("report_id", reportID)
				.value("user_id", userID)
				.value("action_type", type)
				.value("text", text)
				.value("extra", extra==null ? null : extra.toString())
				.executeNoResult();
	}

	public static void setViolationReportState(int reportID, ViolationReport.State state) throws SQLException{
		new SQLQueryBuilder()
				.update("reports")
				.value("state", state)
				.where("id=?", reportID)
				.executeNoResult();
	}

	// region Actor staff notes

	public static int getActorStaffNoteCount(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom(id>0 ? "user_staff_notes" : "group_staff_notes")
				.count()
				.where("target_id=?", Math.abs(id))
				.executeAndGetInt();
	}

	public static PaginatedList<ActorStaffNote> getActorStaffNotes(int id, int offset, int count) throws SQLException{
		int total=getActorStaffNoteCount(id);
		if(total==0)
			return PaginatedList.emptyList(count);
		List<ActorStaffNote> notes=new SQLQueryBuilder()
				.selectFrom(id>0 ? "user_staff_notes" : "group_staff_notes")
				.allColumns()
				.where("target_id=?", Math.abs(id))
				.limit(count, offset)
				.executeAsStream(ActorStaffNote::fromResultSet)
				.toList();
		return new PaginatedList<>(notes, total, offset, count);
	}

	public static int createActorStaffNote(int id, int authorID, String text) throws SQLException{
		return new SQLQueryBuilder()
				.insertInto(id>0 ? "user_staff_notes" : "group_staff_notes")
				.value("target_id", Math.abs(id))
				.value("author_id", authorID)
				.value("text", text)
				.executeAndGetID();
	}

	public static void deleteActorStaffNote(int actorID, int id) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom(actorID>0 ? "user_staff_notes" : "group_staff_notes")
				.where("id=?", id)
				.executeNoResult();
	}

	public static ActorStaffNote getActorStaffNote(int actorID, int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom(actorID>0 ? "user_staff_notes" : "group_staff_notes")
				.where("id=?", id)
				.executeAndGetSingleObject(ActorStaffNote::fromResultSet);
	}

	// endregion

	public static void createEmailDomainBlockRule(String domain, EmailDomainBlockRule.Action action, String note, int creatorID) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("blocks_email_domain")
				.value("domain", domain)
				.value("action", action)
				.value("note", note)
				.value("creator_id", creatorID)
				.executeNoResult();
	}

	public static void updateEmailDomainBlockRule(String domain, EmailDomainBlockRule.Action action, String note) throws SQLException{
		new SQLQueryBuilder()
				.update("blocks_email_domain")
				.value("action", action)
				.value("note", note)
				.where("domain=?", domain)
				.executeNoResult();
	}

	public static List<EmailDomainBlockRule> getEmailDomainBlockRules() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("blocks_email_domain")
				.columns("domain", "action")
				.executeAsStream(EmailDomainBlockRule::fromResultSet)
				.toList();
	}

	public static void deleteEmailDomainBlockRule(String domain) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("blocks_email_domain")
				.where("domain=?", domain)
				.executeNoResult();
	}

	public static List<EmailDomainBlockRuleFull> getEmailDomainBlockRulesFull() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("blocks_email_domain")
				.allColumns()
				.orderBy("created_at DESC")
				.executeAsStream(EmailDomainBlockRuleFull::fromResultSet)
				.toList();
	}

	public static EmailDomainBlockRuleFull getEmailDomainBlockRuleFull(String domain) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("blocks_email_domain")
				.allColumns()
				.where("domain=?", domain)
				.executeAndGetSingleObject(EmailDomainBlockRuleFull::fromResultSet);
	}


	public static void createIPBlockRule(InetAddressRange addressRange, IPBlockRule.Action action, Instant expiresAt, String note, int creatorID) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("blocks_ip")
				.value("address", Utils.serializeInetAddress(addressRange.address()))
				.value("prefix_length", addressRange.prefixLength())
				.value("action", action)
				.value("note", note)
				.value("creator_id", creatorID)
				.value("expires_at", expiresAt)
				.executeNoResult();
	}

	public static void updateIPBlockRule(int id, IPBlockRule.Action action, Instant expiresAt, String note) throws SQLException{
		new SQLQueryBuilder()
				.update("blocks_ip")
				.value("action", action)
				.value("note", note)
				.value("expires_at", expiresAt)
				.where("id=?", id)
				.executeNoResult();
	}

	public static List<IPBlockRule> getIPBlockRules() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("blocks_ip")
				.columns("id", "address", "prefix_length", "action", "expires_at")
				.executeAsStream(IPBlockRule::fromResultSet)
				.toList();
	}

	public static void deleteIPBlockRule(int id) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("blocks_ip")
				.where("id=?", id)
				.executeNoResult();
	}

	public static List<IPBlockRuleFull> getIPBlockRulesFull() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("blocks_ip")
				.allColumns()
				.orderBy("created_at DESC")
				.executeAsStream(IPBlockRuleFull::fromResultSet)
				.toList();
	}

	public static IPBlockRuleFull getIPBlockRuleFull(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("blocks_ip")
				.allColumns()
				.where("id=?", id)
				.executeAndGetSingleObject(IPBlockRuleFull::fromResultSet);
	}

	public static PaginatedList<SignupInvitation> getAllSignupInvites(int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("signup_invitations")
					.count()
					.where("signups_remaining>0")
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<SignupInvitation> invites=new SQLQueryBuilder(conn)
					.selectFrom("signup_invitations")
					.allColumns()
					.where("signups_remaining>0")
					.orderBy("id DESC")
					.limit(count, offset)
					.executeAsStream(SignupInvitation::fromResultSet)
					.toList();
			return new PaginatedList<>(invites, total, offset, count);
		}
	}

	public static void deleteSignupInvite(int id) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("signup_invitations")
				.where("id=?", id)
				.executeNoResult();
	}

	public static void addUserActionLogEntry(int userID, UserActionLogAction action, Map<String, Object> info) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("user_action_log")
				.value("action", action)
				.value("user_id", userID)
				.value("info", Utils.gson.toJson(info))
				.executeNoResult();
	}

	// region Server rules

	public static List<ServerRule> getServerRules(boolean includeDeleted) throws SQLException{
		SQLQueryBuilder b=new SQLQueryBuilder()
				.selectFrom("rules")
				.orderBy("priority DESC, id ASC");
		if(!includeDeleted)
			b.where("is_deleted=0");
		return b.executeAsStream(ServerRule::fromResultSet)
				.toList();
	}

	public static int createServerRule(String title, String description, int priority, String translationsJson) throws SQLException{
		return new SQLQueryBuilder()
				.insertInto("rules")
				.value("title", title)
				.value("description", description)
				.value("priority", priority)
				.value("translations", translationsJson)
				.executeAndGetID();
	}

	public static void updateServerRule(int id, String title, String description, int priority, String translationsJson) throws SQLException{
		new SQLQueryBuilder()
				.update("rules")
				.value("title", title)
				.value("description", description)
				.value("priority", priority)
				.value("translations", translationsJson)
				.where("id=?", id)
				.executeNoResult();
	}

	public static List<ServerRule> getServerRulesByIDs(Collection<Integer> ids) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("rules")
				.whereIn("id", ids)
				.executeAsStream(ServerRule::fromResultSet)
				.toList();
	}

	public static void deleteServerRule(int id) throws SQLException{
		new SQLQueryBuilder()
				.update("rules")
				.value("is_deleted", true)
				.where("id=?", id)
				.executeNoResult();
	}

	// endregion
	// region Announcements

	public static List<ServerAnnouncement> getCurrentAndFutureAnnouncements() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("announcements")
				.where("show_to>CURRENT_TIMESTAMP()")
				.executeAsStream(ServerAnnouncement::fromResultSet)
				.toList();
	}

	public static PaginatedList<ServerAnnouncement> getAllAnnouncements(int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("announcements")
					.count()
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);

			List<ServerAnnouncement> announcements=new SQLQueryBuilder(conn)
					.selectFrom("announcements")
					.limit(count, offset)
					.orderBy("id DESC")
					.executeAsStream(ServerAnnouncement::fromResultSet)
					.toList();
			return new PaginatedList<>(announcements, total, offset, count);
		}
	}

	public static ServerAnnouncement getAnnouncement(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("announcements")
				.where("id=?", id)
				.executeAndGetSingleObject(ServerAnnouncement::fromResultSet);
	}

	public static void createAnnouncement(String title, String description, String linkTitle, String linkUrl, Instant showFrom, Instant showTo, String translationsJson) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("announcements")
				.value("title", title)
				.value("description", description)
				.value("link_text", linkTitle)
				.value("link_url", linkUrl)
				.value("show_from", showFrom)
				.value("show_to", showTo)
				.value("translations", translationsJson)
				.executeNoResult();
	}

	public static void updateAnnouncement(int id, String title, String description, String linkTitle, String linkUrl, Instant showFrom, Instant showTo, String translationsJson) throws SQLException{
		new SQLQueryBuilder()
				.update("announcements")
				.where("id=?", id)
				.value("title", title)
				.value("description", description)
				.value("link_text", linkTitle)
				.value("link_url", linkUrl)
				.value("show_from", showFrom)
				.value("show_to", showTo)
				.value("translations", translationsJson)
				.executeNoResult();
	}

	public static void deleteAnnouncement(int id) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("announcements")
				.where("id=?", id)
				.executeNoResult();
	}

	// endregion
	// region Group action log

	public static void createGroupActionLogEntry(int groupID, GroupActionLogAction action, int adminID, Map<String, Object> info) throws SQLException{
		new SQLQueryBuilder()
				.insertInto("group_action_log")
				.value("group_id", groupID)
				.value("action", action)
				.value("admin_id", adminID==0 ? null : adminID)
				.value("info", Utils.gson.toJson(info))
				.executeNoResult();
	}

	public static PaginatedList<GroupActionLogEntry> getGroupActionLog(int groupID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("group_action_log")
					.count()
					.where("group_id=?", groupID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<GroupActionLogEntry> log=new SQLQueryBuilder(conn)
					.selectFrom("group_action_log")
					.where("group_id=?", groupID)
					.orderBy("id DESC")
					.limit(count, offset)
					.executeAsStream(GroupActionLogEntry::fromResultSet)
					.toList();
			return new PaginatedList<>(log, total, offset, count);
		}
	}

	// endregion
}
