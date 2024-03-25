package smithereen.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.storage.DatabaseUtils;

public record IPBlockRuleFull(IPBlockRule rule, Instant createdAt, int creatorID, String note){
	public static IPBlockRuleFull fromResultSet(ResultSet res) throws SQLException{
		return new IPBlockRuleFull(
				IPBlockRule.fromResultSet(res),
				DatabaseUtils.getInstant(res, "created_at"),
				res.getInt("creator_id"),
				res.getString("note")
		);
	}
}
