package smithereen.model.admin;

import com.google.gson.reflect.TypeToken;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;

public record UserActionLogEntry(UserActionLogAction action, int userID, Instant time, Map<String, Object> info){
	public static UserActionLogEntry fromResultSet(ResultSet res) throws SQLException{
		return new UserActionLogEntry(
				UserActionLogAction.values()[res.getInt("action")],
				res.getInt("user_id"),
				DatabaseUtils.getInstant(res, "time"),
				Utils.gson.fromJson(res.getString("info"), new TypeToken<>(){})
		);
	}
}
