package smithereen.model.admin;

import com.google.gson.reflect.TypeToken;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;

public record GroupActionLogEntry(GroupActionLogAction action, int groupID, Instant time, int adminID, Map<String, Object> info){
	public static GroupActionLogEntry fromResultSet(ResultSet res) throws SQLException{
		return new GroupActionLogEntry(
				GroupActionLogAction.values()[res.getInt("action")],
				res.getInt("group_id"),
				DatabaseUtils.getInstant(res, "time"),
				res.getInt("admin_id"),
				Utils.gson.fromJson(res.getString("info"), new TypeToken<>(){})
		);
	}
}
