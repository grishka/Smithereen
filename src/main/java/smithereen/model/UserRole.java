package smithereen.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumSet;

import smithereen.Utils;

public record UserRole(int id, String name, EnumSet<Permission> permissions){

	public static UserRole fromResultSet(ResultSet res) throws SQLException{
		EnumSet<Permission> permissions=EnumSet.noneOf(Permission.class);
		Utils.deserializeEnumSet(permissions, Permission.class, res.getBytes("permissions"));
		return new UserRole(res.getInt("id"), res.getString("name"), permissions);
	}

	public String getLangKey(){
		// Translatable names for default roles
		return switch(name){
			case "Owner" -> "role_owner";
			case "Admin" -> "role_admin";
			case "Moderator" -> "role_moderator";
			default -> null;
		};
	}

	public enum Permission{
		SUPERUSER,

		MANAGE_SERVER_SETTINGS,
		MANAGE_SERVER_RULES,
		MANAGE_ROLES,
		VIEW_SERVER_AUDIT_LOG,
		MANAGE_USERS,
		MANAGE_USER_ACCESS,
		MANAGE_REPORTS,
		MANAGE_FEDERATION,
		MANAGE_BLOCKING_RULES,
		MANAGE_INVITES,
		CREATE_INVITES,
		DELETE_USERS_IMMEDIATE,
		MANAGE_GROUPS,
		VISIBLE_IN_STAFF
	}
}
