package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.DatabaseSchemaUpdater;
import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration38 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE IF NOT EXISTS `media_files` (
				  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
				  `random_id` binary(18) NOT NULL,
				  `size` bigint unsigned NOT NULL,
				  `type` tinyint unsigned NOT NULL,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `metadata` json NOT NULL,
				  `ref_count` int unsigned NOT NULL DEFAULT '0',
				  `original_owner_id` int NOT NULL,
				  PRIMARY KEY (`id`),
				  KEY `ref_count` (`ref_count`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE IF NOT EXISTS `media_file_refs` (
				  `file_id` bigint unsigned NOT NULL,
				  `object_id` bigint NOT NULL,
				  `object_type` tinyint unsigned NOT NULL,
				  `owner_user_id` int unsigned DEFAULT NULL,
				  `owner_group_id` int unsigned DEFAULT NULL,
				  PRIMARY KEY (`object_id`,`object_type`,`file_id`),
				  KEY `file_id` (`file_id`),
				  KEY `owner_user_id` (`owner_user_id`),
				  KEY `owner_group_id` (`owner_group_id`),
				  CONSTRAINT `media_file_refs_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `media_files` (`id`),
				  CONSTRAINT `media_file_refs_ibfk_2` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `media_file_refs_ibfk_3` FOREIGN KEY (`owner_group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("DROP TABLE IF EXISTS `draft_attachments`");
		DatabaseSchemaUpdater.createMediaRefCountTriggers(conn);
	}
}
