package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration19 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `group_invites` (
				`id` int unsigned NOT NULL AUTO_INCREMENT,
						`inviter_id` int unsigned NOT NULL,
						`invitee_id` int unsigned NOT NULL,
						`group_id` int unsigned NOT NULL,
						`time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						`is_event` tinyint(1) NOT NULL,
						`ap_id` varchar(300) CHARACTER SET ascii DEFAULT NULL,
						PRIMARY KEY (`id`),
						UNIQUE KEY `ap_id` (`ap_id`),
						KEY `inviter_id` (`inviter_id`),
						KEY `invitee_id` (`invitee_id`),
						KEY `group_id` (`group_id`),
						KEY `is_event` (`is_event`),
						CONSTRAINT `group_invites_ibfk_1` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
						CONSTRAINT `group_invites_ibfk_2` FOREIGN KEY (`invitee_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
						CONSTRAINT `group_invites_ibfk_3` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
