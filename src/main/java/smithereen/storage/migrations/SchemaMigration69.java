package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration69 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE followings ADD `added_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, ADD KEY `added_at` (`added_at`)");
	}
}
