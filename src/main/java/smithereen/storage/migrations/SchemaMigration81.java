package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration81 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `group_action_log` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `action` int unsigned NOT NULL,
				  `group_id` int unsigned NOT NULL,
				  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `admin_id` int unsigned DEFAULT NULL,
				  `info` json NOT NULL,
				  PRIMARY KEY (`id`),
				  KEY `group_id` (`group_id`),
				  CONSTRAINT `group_action_log_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
