package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration65 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `user_action_log` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `action` int unsigned NOT NULL,
				  `user_id` int unsigned NOT NULL,
				  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `info` json NOT NULL,
				  PRIMARY KEY (`id`),
				  KEY `user_id` (`user_id`),
				  KEY `action` (`action`),
				  CONSTRAINT `user_action_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
