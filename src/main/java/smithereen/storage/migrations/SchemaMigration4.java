package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration4 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `groups` (
				  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
				  `name` varchar(200) NOT NULL DEFAULT '',
				  `username` varchar(50) NOT NULL DEFAULT '',
				  `domain` varchar(100) NOT NULL DEFAULT '',
				  `ap_id` varchar(300) CHARACTER SET ascii DEFAULT NULL,
				  `ap_url` varchar(300) DEFAULT NULL,
				  `ap_inbox` varchar(300) DEFAULT NULL,
				  `ap_shared_inbox` varchar(300) DEFAULT NULL,
				  `ap_outbox` varchar(300) DEFAULT NULL,
				  `public_key` blob NOT NULL,
				  `private_key` blob,
				  `avatar` text,
				  `about` text,
				  `profile_fields` text,
				  `event_start_time` timestamp NULL DEFAULT NULL,
				  `event_end_time` timestamp NULL DEFAULT NULL,
				  `type` tinyint(3) unsigned NOT NULL DEFAULT '0',
				  `member_count` int(10) unsigned NOT NULL DEFAULT '0',
				  `tentative_member_count` int(10) unsigned NOT NULL DEFAULT '0',
				  `ap_followers` varchar(300) DEFAULT NULL,
				  `ap_wall` varchar(300) DEFAULT NULL,
				  `last_updated` timestamp NULL DEFAULT NULL,
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `username` (`username`,`domain`),
				  UNIQUE KEY `ap_id` (`ap_id`),
				  KEY `type` (`type`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");

		conn.createStatement().execute("""
				CREATE TABLE `group_admins` (
				  `user_id` int(11) unsigned NOT NULL,
				  `group_id` int(11) unsigned NOT NULL,
				  `level` int(11) unsigned NOT NULL,
				  `title` varchar(300) DEFAULT NULL,
				  `display_order` int(10) unsigned NOT NULL DEFAULT '0',
				  KEY `user_id` (`user_id`),
				  KEY `group_id` (`group_id`),
				  CONSTRAINT `group_admins_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `group_admins_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");

		conn.createStatement().execute("""
				CREATE TABLE `group_memberships` (
				  `user_id` int(11) unsigned NOT NULL,
				  `group_id` int(11) unsigned NOT NULL,
				  `post_feed_visibility` tinyint(4) unsigned NOT NULL DEFAULT '0',
				  `tentative` tinyint(1) NOT NULL DEFAULT '0',
				  `accepted` tinyint(1) NOT NULL DEFAULT '1',
				  UNIQUE KEY `user_id` (`user_id`,`group_id`),
				  KEY `group_id` (`group_id`),
				  CONSTRAINT `group_memberships_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `group_memberships_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");

		conn.createStatement().execute("ALTER TABLE users ADD `ap_wall` varchar(300) DEFAULT NULL");
	}
}
