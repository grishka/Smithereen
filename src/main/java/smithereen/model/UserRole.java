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
		MANAGE_ANNOUNCEMENTS,
		DELETE_USERS_IMMEDIATE,
		MANAGE_GROUPS,
		VISIBLE_IN_STAFF;

		public String getLangKey(){
			return switch(this){
				case SUPERUSER -> "admin_permission_superuser";
				case MANAGE_SERVER_SETTINGS -> "admin_permission_server_settings";
				case MANAGE_SERVER_RULES -> "admin_permission_rules";
				case MANAGE_ROLES -> "admin_permission_roles";
				case VIEW_SERVER_AUDIT_LOG -> "admin_permission_audit_log";
				case MANAGE_USERS -> "admin_permission_manage_users";
				case MANAGE_USER_ACCESS -> "admin_permission_user_access";
				case MANAGE_REPORTS -> "admin_permission_reports";
				case MANAGE_FEDERATION -> "admin_permission_federation";
				case MANAGE_BLOCKING_RULES -> "admin_permission_blocking_rules";
				case MANAGE_INVITES -> "admin_permission_invites";
				case MANAGE_ANNOUNCEMENTS -> "admin_permission_announcements";
				case DELETE_USERS_IMMEDIATE -> "admin_permission_delete_users";
				case MANAGE_GROUPS -> "admin_permission_manage_groups";
				case VISIBLE_IN_STAFF -> "admin_visible_in_staff";
			};
		}

		public String getDescriptionLangKey(){
			return switch(this){
				case SUPERUSER -> "admin_permission_descr_superuser";
				case MANAGE_SERVER_SETTINGS -> "admin_permission_descr_server_settings";
				case MANAGE_SERVER_RULES -> "admin_permission_descr_rules";
				case MANAGE_ROLES -> "admin_permission_descr_roles";
				case VIEW_SERVER_AUDIT_LOG -> "admin_permission_descr_audit_log";
				case MANAGE_USERS -> "admin_permission_descr_manage_users";
				case MANAGE_USER_ACCESS -> "admin_permission_descr_user_access";
				case MANAGE_REPORTS -> "admin_permission_descr_reports";
				case MANAGE_FEDERATION -> "admin_permission_descr_federation";
				case MANAGE_BLOCKING_RULES -> "admin_permission_descr_blocking_rules";
				case MANAGE_INVITES -> "admin_permission_descr_invites";
				case MANAGE_ANNOUNCEMENTS -> "admin_permission_descr_announcements";
				case DELETE_USERS_IMMEDIATE -> "admin_permission_descr_delete_users";
				case MANAGE_GROUPS -> "admin_permission_descr_manage_groups";
				case VISIBLE_IN_STAFF -> "admin_visible_in_staff_descr";
			};
		}

		public boolean isActuallySetting(){
			return this==VISIBLE_IN_STAFF;
		}
	}
}
