package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration66 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `fasp_providers` (
				  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
				  `confirmed` tinyint(1) NOT NULL DEFAULT '0',
				  `name` varchar(300) NOT NULL,
				  `base_url` varchar(300) NOT NULL,
				  `sign_in_url` varchar(300) DEFAULT NULL,
				  `remote_id` varchar(64) NOT NULL,
				  `public_key` blob NOT NULL,
				  `private_key` blob NOT NULL,
				  `capabilities` json NOT NULL,
				  `enabled_capabilities` json NOT NULL,
				  `privacy_policy` json DEFAULT NULL,
				  `contact_email` varchar(300) DEFAULT NULL,
				  `actor_id` int DEFAULT NULL,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `base_url` (`base_url`),
				  KEY `confirmed` (`confirmed`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("""
				CREATE TABLE `fasp_debug_callbacks` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `provider_id` bigint unsigned NOT NULL,
				  `ip` binary(16) NOT NULL,
				  `body` text COLLATE utf8mb4_general_ci NOT NULL,
				  `received_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (`id`),
				  KEY `provider_id` (`provider_id`),
				  CONSTRAINT `fasp_debug_callbacks_ibfk_1` FOREIGN KEY (`provider_id`) REFERENCES `fasp_providers` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;""");
	}
}
