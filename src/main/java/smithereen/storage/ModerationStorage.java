package smithereen.storage;

import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import smithereen.Utils;
import smithereen.model.PaginatedList;
import smithereen.model.Server;
import smithereen.model.UserRole;
import smithereen.model.ViolationReport;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import spark.utils.StringUtils;

public class ModerationStorage{
	public static int createViolationReport(int reporterID, ViolationReport.TargetType targetType, int targetID, ViolationReport.ContentType contentType, long contentID, String comment, String otherServerDomain) throws SQLException{
		SQLQueryBuilder bldr=new SQLQueryBuilder()
				.insertInto("reports")
				.value("reporter_id", reporterID!=0 ? reporterID : null)
				.value("target_type", targetType.ordinal())
				.value("target_id", targetID)
				.value("comment", comment)
				.value("server_domain", otherServerDomain);
		if(contentType!=null){
			bldr.value("content_type", contentType.ordinal())
					.value("content_id", contentID);
		}
		return bldr.executeAndGetID();
	}

	public static int getViolationReportsCount(boolean open) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("reports")
				.count()
				.where("action_time IS"+(open ? "" : " NOT")+" NULL")
				.executeAndGetInt();
	}

	public static PaginatedList<ViolationReport> getViolationReports(boolean open, int offset, int count) throws SQLException{
		int total=getViolationReportsCount(open);
		if(total==0)
			return PaginatedList.emptyList(count);
		List<ViolationReport> reports=new SQLQueryBuilder()
				.selectFrom("reports")
				.allColumns()
				.where("action_time IS"+(open ? "" : " NOT")+" NULL")
				.limit(count, offset)
				.orderBy("id DESC")
				.executeAsStream(ViolationReport::fromResultSet)
				.toList();
		return new PaginatedList<>(reports, total, offset, count);
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

	public static void setViolationReportResolved(int reportID, int moderatorID) throws SQLException{
		new SQLQueryBuilder()
				.update("reports")
				.value("moderator_id", moderatorID)
				.valueExpr("action_time", "CURRENT_TIMESTAMP()")
				.where("id=?", reportID)
				.executeNoResult();
	}

	public static Map<Integer, Integer> getRoleAccountCounts() throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("accounts")
				.selectExpr("role, count(*)")
				.where("role IS NOT NULL")
				.groupBy("role")
				.executeAsStream(rs->new IdPair(rs.getInt(1), rs.getInt(2)))
				.collect(Collectors.toMap(IdPair::first, IdPair::second));
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
}
