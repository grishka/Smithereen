package smithereen.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

import smithereen.data.StatsType;

public class StatsStorage{

	public static void incrementDaily(StatsType type, int objectID, LocalDate date) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=SQLQueryBuilder.prepareStatement(conn, "INSERT INTO `stats_daily` (`day`, `type`, `object_id`, `count`) VALUES (?, ?, ?, 1) ON DUPLICATE KEY UPDATE `count`=`count`+1", date, type, objectID);
		stmt.execute();
	}
}
