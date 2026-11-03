package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration40 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE `reports` ADD `content` json DEFAULT NULL, ADD `state` tinyint unsigned NOT NULL DEFAULT 0, ADD KEY `state` (`state`), CHANGE `target_id` `target_id` int NOT NULL, ADD KEY `target_id` (`target_id`)");
		conn.createStatement().execute("""
				CREATE TABLE `report_actions` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `report_id` int unsigned NOT NULL,
				  `user_id` int unsigned NOT NULL,
				  `action_type` tinyint unsigned NOT NULL,
				  `text` text COLLATE utf8mb4_general_ci,
				  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `extra` json DEFAULT NULL,
				  PRIMARY KEY (`id`),
				  KEY `report_id` (`report_id`),
				  CONSTRAINT `report_actions_ibfk_1` FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;""");
	}
}
