package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.DatabaseSchemaUpdater;
import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration55 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `comments` (
				  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
				  `author_id` int unsigned DEFAULT NULL,
				  `owner_user_id` int unsigned DEFAULT NULL,
				  `owner_group_id` int unsigned DEFAULT NULL,
				  `parent_object_type` int unsigned NOT NULL,
				  `parent_object_id` bigint unsigned NOT NULL,
				  `text` text,
				  `attachments` json DEFAULT NULL,
				  `ap_url` varchar(300) DEFAULT NULL,
				  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `content_warning` text,
				  `updated_at` timestamp NULL DEFAULT NULL,
				  `reply_key` varbinary(2048) DEFAULT NULL,
				  `mentions` varbinary(1024) DEFAULT NULL,
				  `reply_count` int unsigned NOT NULL DEFAULT '0',
				  `ap_replies` varchar(300) DEFAULT NULL,
				  `federation_state` tinyint unsigned NOT NULL DEFAULT '0',
				  `source` text,
				  `source_format` tinyint unsigned DEFAULT NULL,
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `ap_id` (`ap_id`),
				  KEY `owner_user_id` (`owner_user_id`),
				  KEY `author_id` (`author_id`),
				  KEY `reply_key` (`reply_key`),
				  KEY `owner_group_id` (`owner_group_id`),
				  KEY `parent_object_type` (`parent_object_type`,`parent_object_id`),
				  CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`owner_group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		DatabaseSchemaUpdater.createApIdIndexTriggersForComments(conn);
	}
}
