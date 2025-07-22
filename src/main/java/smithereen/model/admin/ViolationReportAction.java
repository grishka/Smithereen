package smithereen.model.admin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.storage.DatabaseUtils;

public record ViolationReportAction(int id, int reportID, int userID, ActionType actionType, String text, Instant time, JsonObject extra){

	public static ViolationReportAction fromResultSet(ResultSet res) throws SQLException{
		String extra=res.getString("extra");
		return new ViolationReportAction(
				res.getInt("id"),
				res.getInt("report_id"),
				res.getInt("user_id"),
				ActionType.values()[res.getInt("action_type")],
				res.getString("text"),
				DatabaseUtils.getInstant(res, "time"),
				extra==null ? null : JsonParser.parseString(extra).getAsJsonObject()
		);
	}

	public enum ActionType{
		COMMENT,
		ADD_CONTENT,
		REMOVE_CONTENT,
		DELETE_CONTENT,
		RESOLVE_WITH_ACTION,
		RESOLVE_REJECT,
		REOPEN,
	}
}
