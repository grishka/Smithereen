package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration58 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `photo_tags` (
				  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
				  `photo_id` bigint unsigned NOT NULL,
				  `placer_id` int unsigned NOT NULL,
				  `user_id` int unsigned DEFAULT NULL,
				  `name` varchar(300) NOT NULL,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `approved` tinyint unsigned NOT NULL DEFAULT '0',
				  `x1` float NOT NULL,
				  `y1` float NOT NULL,
				  `x2` float NOT NULL,
				  `y2` float NOT NULL,
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `photo_id` (`photo_id`,`user_id`),
				  KEY `user_id` (`user_id`),
				  KEY `approved` (`approved`),
				  CONSTRAINT `photo_tags_ibfk_1` FOREIGN KEY (`photo_id`) REFERENCES `photos` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `photo_tags_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
				) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4;""");
	}
}
