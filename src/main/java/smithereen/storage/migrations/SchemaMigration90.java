package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration90 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `api_app_installs` (
				  `user_id` int unsigned NOT NULL,
				  `app_id` bigint unsigned NOT NULL,
				  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (`user_id`,`app_id`),
				  KEY `app_id` (`app_id`),
				  CONSTRAINT `api_app_installs_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `api_app_installs_ibfk_2` FOREIGN KEY (`app_id`) REFERENCES `api_applications` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
