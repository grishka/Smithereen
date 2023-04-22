package smithereen.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import smithereen.storage.DatabaseUtils;

public record StatsPoint(int count, LocalDate date){
	public static StatsPoint fromResultSet(ResultSet res) throws SQLException{
		int count=res.getInt("count");
		LocalDate date=DatabaseUtils.getLocalDate(res, "day");
		return new StatsPoint(count, date);
	}
}
