package smithereen.storage;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import smithereen.model.StatsPoint;
import smithereen.model.StatsType;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;

public class StatsStorage{

	public static void incrementDaily(StatsType type, int objectID, LocalDate date) throws SQLException{
		try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
			PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "INSERT INTO `stats_daily` (`day`, `type`, `object_id`, `count`) VALUES (?, ?, ?, 1) ON DUPLICATE KEY UPDATE `count`=`count`+1", date, type, objectID);
			stmt.execute();
		}
	}

	public static List<StatsPoint> getDaily(StatsType type, int objectID) throws SQLException{
		return new SQLQueryBuilder()
				.selectFrom("stats_daily")
				.columns("day", "count")
				.where("type=? AND object_id=?", type, objectID)
				.executeAsStream(StatsPoint::fromResultSet)
				.toList();
	}
}
