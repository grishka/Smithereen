package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration27 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `reports` (
					`id` int unsigned NOT NULL AUTO_INCREMENT,
					`reporter_id` int unsigned NULL DEFAULT NULL,
					`target_type` tinyint unsigned NOT NULL,
					`content_type` tinyint unsigned DEFAULT NULL,
					`target_id` int unsigned NOT NULL,
					`content_id` int unsigned DEFAULT NULL,
					`comment` text NOT NULL,
					`moderator_id` int unsigned DEFAULT NULL,
					`time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
					`action_time` timestamp NULL DEFAULT NULL,
					`server_domain` varchar(100) NULL DEFAULT NULL,
					PRIMARY KEY (`id`),
					KEY `reporter_id` (`reporter_id`),
					KEY `moderator_id` (`moderator_id`),
					CONSTRAINT `reports_ibfk_1` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`),
					CONSTRAINT `reports_ibfk_2` FOREIGN KEY (`moderator_id`) REFERENCES `users` (`id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
