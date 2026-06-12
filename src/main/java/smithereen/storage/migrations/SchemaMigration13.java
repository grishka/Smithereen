package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration13 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `polls` (
				  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
				  `owner_id` int(10) unsigned NOT NULL,
				  `ap_id` varchar(300) CHARACTER SET ascii DEFAULT NULL,
				  `question` text,
				  `is_anonymous` tinyint(1) NOT NULL DEFAULT '0',
				  `is_multi_choice` tinyint(1) NOT NULL DEFAULT '0',
				  `end_time` timestamp NULL DEFAULT NULL,
				  `num_voted_users` int(10) unsigned NOT NULL DEFAULT '0',
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `ap_id` (`ap_id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE `poll_options` (
				  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
				  `poll_id` int(10) unsigned NOT NULL,
				  `ap_id` varchar(300) CHARACTER SET ascii DEFAULT NULL,
				  `text` text NOT NULL,
				  `num_votes` int(10) unsigned NOT NULL DEFAULT '0',
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `ap_id` (`ap_id`),
				  KEY `poll_id` (`poll_id`),
				  CONSTRAINT `poll_options_ibfk_1` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE `poll_votes` (
				  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
				  `user_id` int(11) unsigned NOT NULL,
				  `poll_id` int(10) unsigned NOT NULL,
				  `option_id` int(10) unsigned NOT NULL,
				  `ap_id` varchar(300) CHARACTER SET ascii DEFAULT NULL,
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `ap_id` (`ap_id`),
				  KEY `user_id` (`user_id`),
				  KEY `poll_id` (`poll_id`),
				  KEY `option_id` (`option_id`),
				  CONSTRAINT `poll_votes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
				  CONSTRAINT `poll_votes_ibfk_2` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `poll_votes_ibfk_3` FOREIGN KEY (`option_id`) REFERENCES `poll_options` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("ALTER TABLE wall_posts ADD `poll_id` int(10) unsigned DEFAULT NULL");
	}
}
