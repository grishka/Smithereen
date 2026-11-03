package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration75 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE reports ADD has_file_refs tinyint(1) NOT NULL DEFAULT 1, ADD KEY has_file_refs (has_file_refs)");
	}
}
