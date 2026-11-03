package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration30 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `mail_messages` (
				  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
				  `owner_id` int unsigned NOT NULL,
				  `sender_id` int unsigned NOT NULL,
				  `to` varbinary(1024) NOT NULL,
				  `cc` varbinary(1024) DEFAULT NULL,
				  `text` text NOT NULL,
				  `subject` text NOT NULL,
				  `attachments` json DEFAULT NULL,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `updated_at` timestamp NULL DEFAULT NULL,
				  `deleted_at` timestamp NULL DEFAULT NULL,
				  `read_receipts` varbinary(1024) DEFAULT NULL,
				  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
				  `reply_info` json DEFAULT NULL,
				  `related_message_ids` varbinary(1024) DEFAULT NULL,
				  PRIMARY KEY (`id`),
				  KEY `owner_id` (`owner_id`),
				  KEY `sender_id` (`sender_id`),
				  KEY `ap_id` (`ap_id`),
				  KEY `deleted_at` (`deleted_at`),
				  KEY `read_receipts` (`read_receipts`),
				  FULLTEXT KEY `text` (`text`,`subject`),
				  CONSTRAINT `mail_messages_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE `mail_messages_peers` (
				  `owner_id` int unsigned NOT NULL,
				  `peer_id` int unsigned NOT NULL,
				  `message_id` bigint unsigned NOT NULL,
				  KEY `owner_id` (`owner_id`),
				  KEY `message_id` (`message_id`),
				  KEY `peer_id` (`peer_id`),
				  CONSTRAINT `mail_messages_peers_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `mail_messages_peers_ibfk_2` FOREIGN KEY (`message_id`) REFERENCES `mail_messages` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE `mail_privacy_grants` (
				  `owner_id` int unsigned NOT NULL,
				  `user_id` int unsigned NOT NULL,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `messages_remain` int unsigned NOT NULL,
				  PRIMARY KEY (`owner_id`,`user_id`),
				  KEY `user_id` (`user_id`),
				  CONSTRAINT `mail_privacy_grants_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `mail_privacy_grants_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
