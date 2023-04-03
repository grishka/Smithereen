package smithereen.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import smithereen.data.StatsPoint;
import smithereen.data.StatsType;

public class StatsStorage{

	public static void incrementDaily(StatsType type, int objectID, LocalDate date) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "INSERT INTO `stats_daily` (`day`, `type`, `object_id`, `count`) VALUES (?, ?, ?, 1) ON DUPLICATE KEY UPDATE `count`=`count`+1", date, type, objectID);
		stmt.execute();
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
