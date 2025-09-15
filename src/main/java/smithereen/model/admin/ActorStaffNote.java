package smithereen.model.admin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.storage.DatabaseUtils;

public record ActorStaffNote(int id, int targetID, int authorID, String text, Instant time){
	public static ActorStaffNote fromResultSet(ResultSet res) throws SQLException{
		return new ActorStaffNote(
				res.getInt("id"),
				res.getInt("target_id"),
				res.getInt("author_id"),
				res.getString("text"),
				DatabaseUtils.getInstant(res, "time")
		);
	}
}
