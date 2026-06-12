package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration64 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE group_memberships ADD `hints_rank` int unsigned NOT NULL DEFAULT '0', ADD KEY `hints_rank` (`hints_rank`)");
	}
}
