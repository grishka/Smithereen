package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration35 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `audit_log` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `admin_id` int unsigned NOT NULL,
				  `action` int unsigned NOT NULL,
				  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  `owner_id` int DEFAULT NULL,
				  `object_id` bigint DEFAULT NULL,
				  `object_type` int unsigned DEFAULT NULL,
				  `extra` json DEFAULT NULL,
				  PRIMARY KEY (`id`),
				  KEY `owner_id` (`owner_id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
