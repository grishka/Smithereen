package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration47 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `bookmarks_group` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `owner_id` int unsigned NOT NULL,
				  `group_id` int unsigned NOT NULL,
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `owner_id_2` (`owner_id`,`group_id`),
				  KEY `group_id` (`group_id`),
				  CONSTRAINT `bookmarks_group_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;""");
		conn.createStatement().execute("""
				CREATE TABLE `bookmarks_user` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `owner_id` int unsigned NOT NULL,
				  `user_id` int unsigned NOT NULL,
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `owner_id` (`owner_id`,`user_id`),
				  KEY `user_id` (`user_id`),
				  CONSTRAINT `bookmarks_user_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;""");
	}
}
