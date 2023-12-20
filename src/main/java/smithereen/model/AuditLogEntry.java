package smithereen.model;

import com.google.gson.reflect.TypeToken;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;

public record AuditLogEntry(int id, int adminID, Action action, Instant time, int ownerID, long objectID, ObjectType objectType, Map<String, Object> extra){
	public enum Action{
		// Roles
		CREATE_ROLE,
		EDIT_ROLE,
		DELETE_ROLE,
		ASSIGN_ROLE,

		// Users
		ACTIVATE_ACCOUNT,
		SET_USER_EMAIL,
		RESET_USER_PASSWORD
	}

	public enum ObjectType{
		ROLE,
		POST,
		REPORT,
	}

	public static AuditLogEntry fromResultSet(ResultSet res) throws SQLException{
		int _type=res.getInt("object_type");
		ObjectType objType=res.wasNull() ? null : ObjectType.values()[_type];
		String extra=res.getString("extra");
		return new AuditLogEntry(
				res.getInt("id"),
				res.getInt("admin_id"),
				Action.values()[res.getInt("action")],
				DatabaseUtils.getInstant(res, "time"),
				res.getInt("owner_id"),
				res.getLong("object_id"),
				objType,
				extra==null ? null : Utils.gson.fromJson(extra, new TypeToken<>(){})
		);
	}
}
