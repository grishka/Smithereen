package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration83 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE `groups` ADD `ban_status` tinyint unsigned NOT NULL DEFAULT '0', ADD `ban_info` json DEFAULT NULL, ADD KEY `ban_status` (`ban_status`)");
	}
}
