package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration5 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `blocks_group_domain` (
				  `owner_id` int(10) unsigned NOT NULL,
				  `domain` varchar(100) CHARACTER SET ascii NOT NULL,
				  UNIQUE KEY `owner_id` (`owner_id`,`domain`),
				  KEY `domain` (`domain`),
				  CONSTRAINT `blocks_group_domain_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE `blocks_group_user` (
				  `owner_id` int(10) unsigned NOT NULL,
				  `user_id` int(10) unsigned NOT NULL,
				  UNIQUE KEY `owner_id` (`owner_id`,`user_id`),
				  KEY `user_id` (`user_id`),
				  CONSTRAINT `blocks_group_user_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `blocks_group_user_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE `blocks_user_domain` (
				  `owner_id` int(10) unsigned NOT NULL,
				  `domain` varchar(100) CHARACTER SET ascii NOT NULL,
				  UNIQUE KEY `owner_id` (`owner_id`,`domain`),
				  KEY `domain` (`domain`),
				  CONSTRAINT `blocks_user_domain_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE `blocks_user_user` (
				  `owner_id` int(10) unsigned NOT NULL,
				  `user_id` int(10) unsigned NOT NULL,
				  UNIQUE KEY `owner_id` (`owner_id`,`user_id`),
				  KEY `user_id` (`user_id`),
				  CONSTRAINT `blocks_user_user_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `blocks_user_user_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
