package smithereen.storage;

import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import smithereen.Utils;
import smithereen.model.ActorStaffNote;
import smithereen.model.AuditLogEntry;
import smithereen.model.EmailDomainBlockRule;
import smithereen.model.EmailDomainBlockRuleFull;
import smithereen.model.PaginatedList;
import smithereen.model.Server;
import smithereen.model.UserRole;
import smithereen.model.ViolationReport;
import smithereen.model.ViolationReportAction;
import smithereen.model.viewmodel.AdminUserViewModel;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.storage.utils.IntPair;
import smithereen.util.InetAddressRange;
import spark.utils.StringUtils;

public class ModerationStorage{
	public static int createViolationReport(int reporterID, int targetID, String comment, String otherServerDomain, String contentJson) throws SQLException{
		SQLQueryBuilder bldr=new SQLQueryBuilder()
				.insertInto("reports")
				.value("reporter_id", reporterID!=0 ? reporterID : null)
				.value("target_id", targetID)
				.value("comment", comment)
				.value("server_domain", otherServerDomain)
				.value("content", contentJson);
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

	public static PaginatedList<Server> getAllServers(int offset, int count, @Nullable Server.Availability availability, boolean restrictedOnly, String query) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			String where="";
			Object[] whereArgs={};
			if(availability!=null){
				where=switch(availability){
					case UP -> "is_up=1";
					case DOWN -> "is_up=0";
					case FAILING -> "error_day_count>0";
				};
			}
			if(restrictedOnly){
				if(where.length()>0)
					where+=" AND ";
				where+="is_restricted=1";
			}
			if(StringUtils.isNotEmpty(query)){
				if(where.length()>0)
					where+=" AND ";
				where+="host LIKE ?";
				whereArgs=new Object[]{"%"+query+"%"};
			}
			if(where.isEmpty())
				where=null;

			int total=new SQLQueryBuilder(conn)
					.selectFrom("servers")
					.count()
					.where(where, whereArgs)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			List<Server> servers=new SQLQueryBuilder(conn)
					.selectFrom("servers")
					.allColumns()
					.where(where, whereArgs)
					.limit(count, offset)
					.executeAsStream(Server::fromResultSet)
					.toList();
			return new PaginatedList<>(servers, total, offset, count);
		}
	}

	public static Server getServerByDomain(String domain) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("servers")
				.where("host=?", domain)
				.executeAndGetSingleObject(Server::fromResultSet);
	}

	public static void setServerRestriction(int id, String restrictionJson) throws SQLException{
		new SQLQueryBuilder()
				.update("servers")
				.value("restriction", restrictionJson)
				.value("is_restricted", restrictionJson!=null)
				.where("id=?", id)
				.executeNoResult();
	}

	public static int addServer(String domain) throws SQLException{
		return new SQLQueryBuilder()
				.insertInto("servers")
				.value("host", domain)
				.executeAndGetID();
	}

	public static void setServerAvailability(int id, LocalDate lastErrorDay, int errorDayCount, boolean isUp) throws SQLException{
		new SQLQueryBuilder()
				.update("servers")
				.value("last_error_day", lastErrorDay)
				.value("error_day_count", errorDayCount)
				.value("is_up", isUp)
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

	public static PaginatedList<AuditLogEntry> getUserAuditLog(int userID, int offset, int count) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			int total=new SQLQueryBuilder(conn)
					.selectFrom("audit_log")
					.count()
					.where("owner_id=?", userID)
					.executeAndGetInt();
			if(total==0)
				return PaginatedList.emptyList(count);
			return new PaginatedList<>(new SQLQueryBuilder(conn)
					.selectFrom("audit_log")
					.allColumns()
					.where("owner_id=?", userID)
					.orderBy("id DESC")
					.limit(count, offset)
					.executeAsStream(AuditLogEntry::fromResultSet)
					.toList(), total, offset, count);
		}
	}

	public static PaginatedList<AdminUserViewModel> getUsers(String q, Boolean localOnly, String emailDomain, InetAddressRange ipRange, int role, int offset, int count) throws SQLException{
		if(StringUtils.isNotEmpty(q)){
			q=Arrays.stream(Utils.transliterate(q).replaceAll("[()\\[\\]*+~<>\\\"@-]", " ").split("[ \t]+")).filter(Predicate.not(String::isBlank)).map(s->'+'+s+'*').collect(Collectors.joining(" "));
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

	public static int getUserStaffNoteCount(int userID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("user_staff_notes")
				.count()
				.where("target_id=?", userID)
				.executeAndGetInt();
	}

	public static PaginatedList<ActorStaffNote> getUserStaffNotes(int userID, int offset, int count) throws SQLException{
		int total=getUserStaffNoteCount(userID);
		if(total==0)
			return PaginatedList.emptyList(count);
		List<ActorStaffNote> notes=new SQLQueryBuilder()
				.selectFrom("user_staff_notes")
				.allColumns()
				.where("target_id=?", userID)
				.limit(count, offset)
				.executeAsStream(ActorStaffNote::fromResultSet)
				.toList();
		return new PaginatedList<>(notes, total, offset, count);
	}

	public static int createUserStaffNote(int userID, int authorID, String text) throws SQLException{
		return new SQLQueryBuilder()
				.insertInto("user_staff_notes")
				.value("target_id", userID)
				.value("author_id", authorID)
				.value("text", text)
				.executeAndGetID();
	}

	public static void deleteUserStaffNote(int id) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("user_staff_notes")
				.where("id=?", id)
				.executeNoResult();
	}

	public static ActorStaffNote getUserStaffNote(int id) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("user_staff_notes")
				.where("id=?", id)
				.executeAndGetSingleObject(ActorStaffNote::fromResultSet);
	}

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
}
