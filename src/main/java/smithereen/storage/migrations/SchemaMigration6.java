package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration6 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `email_codes` (
				  `code` binary(64) NOT NULL,
				  `account_id` int(10) unsigned DEFAULT NULL,
				  `type` int(11) NOT NULL,
				  `extra` text,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (`code`),
				  KEY `account_id` (`account_id`),
				  CONSTRAINT `email_codes_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
