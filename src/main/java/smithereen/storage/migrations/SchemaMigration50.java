package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration50 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `photo_albums` (
				  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
				  `owner_user_id` int unsigned DEFAULT NULL,
				  `owner_group_id` int unsigned DEFAULT NULL,
				  `title` varchar(200) NOT NULL,
				  `description` text NOT NULL,
				  `privacy` json NOT NULL,
				  `num_photos` int unsigned NOT NULL DEFAULT '0',
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `system_type` tinyint unsigned DEFAULT NULL,
				  `cover_id` bigint unsigned DEFAULT NULL,
				  `flags` bigint NOT NULL DEFAULT '0',
				  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
				  `ap_url` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
				  `display_order` int unsigned NOT NULL DEFAULT '0',
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `ap_id` (`ap_id`),
				  KEY `owner_user_id` (`owner_user_id`),
				  KEY `owner_group_id` (`owner_group_id`),
				  KEY `display_order` (`display_order`),
				  CONSTRAINT `photo_albums_ibfk_1` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `photo_albums_ibfk_2` FOREIGN KEY (`owner_group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE `photos` (
				  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
				  `owner_id` int NOT NULL,
				  `author_id` int unsigned NOT NULL,
				  `album_id` bigint unsigned NOT NULL,
				  `local_file_id` bigint unsigned DEFAULT NULL,
				  `remote_src` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
				  `description` text NOT NULL,
				  `description_source` text,
				  `description_source_format` tinyint unsigned DEFAULT NULL,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `metadata` json DEFAULT NULL,
				  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
				  `display_order` int unsigned NOT NULL,
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `ap_id` (`ap_id`),
				  KEY `owner_id` (`owner_id`),
				  KEY `album_id` (`album_id`),
				  KEY `display_order` (`display_order`),
				  KEY `local_file_id` (`local_file_id`),
				  KEY `author_id` (`author_id`),
				  CONSTRAINT `photos_ibfk_1` FOREIGN KEY (`album_id`) REFERENCES `photo_albums` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `photos_ibfk_2` FOREIGN KEY (`local_file_id`) REFERENCES `media_files` (`id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
