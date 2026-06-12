package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration16 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE `wall_posts` ADD `source` text DEFAULT NULL, ADD `source_format` tinyint unsigned DEFAULT NULL");
	}
}
