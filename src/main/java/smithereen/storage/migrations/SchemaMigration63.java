package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration63 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE users ADD `presence` json DEFAULT NULL, ADD `is_online` BOOL AS (IFNULL(CAST(presence->'$.isOnline' AS UNSIGNED), 0)) NOT NULL, "+
				"ADD KEY `is_online` (`is_online`), ADD `num_followers` bigint NOT NULL DEFAULT '0', ADD `num_following` bigint NOT NULL DEFAULT '0', ADD `num_friends` bigint NOT NULL DEFAULT '0'");
	}
}
