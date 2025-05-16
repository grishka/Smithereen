package smithereen.storage;

import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import smithereen.Utils;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.model.PaginatedList;
import smithereen.model.Server;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import spark.utils.StringUtils;

public class FederationStorage{
	public static ObjectLinkResolver.ObjectTypeAndID getObjectTypeAndID(URI apID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("ap_id_index")
				.columns("object_type", "object_id")
				.where("ap_id=?", apID.toString())
				.executeAndGetSingleObject(ObjectLinkResolver.ObjectTypeAndID::fromResultSet);
	}

	public static void deleteFromApIdIndex(URI apID) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("ap_id_index")
				.where("ap_id=?", apID.toString())
				.executeNoResult();
	}

	public static void deleteFromApIdIndex(ObjectLinkResolver.ObjectType type, long id) throws SQLException{
		new SQLQueryBuilder()
				.deleteFrom("ap_id_index")
				.where("object_type=? AND object_id=?", type, id)
				.executeNoResult();
	}

	public static void addToApIdIndex(URI apID, ObjectLinkResolver.ObjectType type, long id) throws SQLException{
		new SQLQueryBuilder()
				.insertIgnoreInto("ap_id_index")
				.value("ap_id", apID.toString())
				.value("object_type", type)
				.value("object_id", id)
				.executeNoResult();
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
				if(!where.isEmpty())
					where+=" AND ";
				where+="is_restricted=1";
			}
			if(StringUtils.isNotEmpty(query)){
				if(!where.isEmpty())
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

	public static void setServerFeatures(int id, EnumSet<Server.Feature> features) throws SQLException{
		new SQLQueryBuilder()
				.update("servers")
				.value("features", Utils.serializeEnumSet(features))
				.where("id=?", id)
				.executeNoResult();
	}
}
