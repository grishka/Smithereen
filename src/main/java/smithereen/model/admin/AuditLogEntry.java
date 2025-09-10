package smithereen.model.admin;

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
		RESET_USER_PASSWORD,
		END_USER_SESSION,
		BAN_USER,
		DELETE_USER,

		// Blocking rules
		CREATE_EMAIL_DOMAIN_RULE,
		UPDATE_EMAIL_DOMAIN_RULE,
		DELETE_EMAIL_DOMAIN_RULE,
		CREATE_IP_RULE,
		UPDATE_IP_RULE,
		DELETE_IP_RULE,

		// Invites
		DELETE_SIGNUP_INVITE,

		// Server rules
		CREATE_SERVER_RULE,
		UPDATE_SERVER_RULE,
		DELETE_SERVER_RULE,

		// Groups
		BAN_GROUP,
		DELETE_GROUP,
	}

	public enum ObjectType{
		ROLE,
		POST,
		REPORT,
		SIGNUP_INVITE,
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
