package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration73 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `board_topics` (
				  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
				  `title` tinytext COLLATE utf8mb4_general_ci NOT NULL,
				  `author_id` int unsigned NOT NULL,
				  `group_id` int unsigned NOT NULL,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `pinned_at` timestamp NULL DEFAULT NULL,
				  `num_comments` int unsigned NOT NULL DEFAULT '0',
				  `last_comment_author_id` int unsigned NOT NULL,
				  `is_closed` tinyint(1) NOT NULL DEFAULT '0',
				  `ap_url` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL,
				  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
				  `first_comment_id` bigint unsigned NOT NULL DEFAULT '0',
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `ap_id` (`ap_id`),
				  KEY `group_id` (`group_id`),
				  KEY `updated_at` (`updated_at`),
				  KEY `created_at` (`created_at`),
				  KEY `pinned_at` (`pinned_at`),
				  CONSTRAINT `board_topics_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
