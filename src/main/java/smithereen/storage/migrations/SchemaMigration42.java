package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration42 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `blocks_email_domain` (
				  `domain` varchar(100) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
				  `action` tinyint unsigned NOT NULL,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `note` text COLLATE utf8mb4_general_ci NOT NULL,
				  `creator_id` int unsigned NOT NULL,
				  PRIMARY KEY (`domain`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;""");
		conn.createStatement().execute("""
				CREATE TABLE `blocks_ip` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `address` binary(16) NOT NULL,
				  `prefix_length` tinyint unsigned NOT NULL,
				  `action` tinyint unsigned NOT NULL,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `expires_at` timestamp NOT NULL,
				  `note` text COLLATE utf8mb4_general_ci NOT NULL,
				  `creator_id` int unsigned NOT NULL,
				  PRIMARY KEY (`id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;""");
	}
}
