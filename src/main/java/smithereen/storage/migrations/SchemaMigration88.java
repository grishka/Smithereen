package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.DatabaseSchemaUpdater;
import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration88 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `api_applications` (
				  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
				  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
				  `username` varchar(64) DEFAULT NULL,
				  `domain` varchar(100) NOT NULL DEFAULT '',
				  `public_key` blob,
				  `private_key` blob,
				  `type` tinyint unsigned NOT NULL,
				  `name` varchar(50) NOT NULL,
				  `description` text,
				  `logo` json DEFAULT NULL,
				  `developer_id` int DEFAULT NULL,
				  `extra` json DEFAULT NULL,
				  `ap_inbox` varchar(300) DEFAULT NULL,
				  `ap_shared_inbox` varchar(300) DEFAULT NULL,
				  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `ap_id` (`ap_id`),
				  UNIQUE KEY `username` (`username`,`domain`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE `api_codes` (
				  `id` binary(64) NOT NULL,
				  `account_id` int unsigned NOT NULL,
				  `app_id` bigint unsigned NOT NULL,
				  `permissions` bit(64) NOT NULL DEFAULT b'0',
				  `expires_at` timestamp NOT NULL,
				  `extra` json DEFAULT NULL,
				  PRIMARY KEY (`id`),
				  KEY `expires_at` (`expires_at`),
				  KEY `account_id` (`account_id`),
				  KEY `app_id` (`app_id`),
				  CONSTRAINT `api_codes_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `api_codes_ibfk_2` FOREIGN KEY (`app_id`) REFERENCES `api_applications` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE `api_grants` (
				  `account_id` int unsigned NOT NULL,
				  `app_id` bigint unsigned NOT NULL,
				  `granted_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `permissions` bit(64) NOT NULL DEFAULT b'0',
				  PRIMARY KEY (`account_id`,`app_id`),
				  KEY `app_id` (`app_id`),
				  CONSTRAINT `api_grants_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `api_grants_ibfk_2` FOREIGN KEY (`app_id`) REFERENCES `api_applications` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE `api_tokens` (
				  `id` binary(64) NOT NULL,
				  `account_id` int unsigned NOT NULL,
				  `app_id` bigint unsigned NOT NULL,
				  `last_active` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `ip` binary(16) NOT NULL,
				  `user_agent` bigint NOT NULL,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `expires_at` timestamp NULL DEFAULT NULL,
				  `permissions` bit(64) NOT NULL,
				  PRIMARY KEY (`id`),
				  KEY `account_id` (`account_id`),
				  KEY `app_id` (`app_id`),
				  KEY `expires_at` (`expires_at`),
				  CONSTRAINT `api_tokens_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `api_tokens_ibfk_2` FOREIGN KEY (`app_id`) REFERENCES `api_applications` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		DatabaseSchemaUpdater.createApIdIndexTriggersForApps(conn);
	}
}
