package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration60 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `newsfeed_groups` (
				  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
				  `type` int unsigned NOT NULL,
				  `object_id` bigint unsigned DEFAULT NULL,
				  `group_id` int unsigned NOT NULL,
				  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `type` (`type`,`group_id`,`object_id`),
				  KEY `time` (`time`),
				  KEY `group_id` (`group_id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
