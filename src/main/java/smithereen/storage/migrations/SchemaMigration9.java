package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration9 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE users ADD `ap_friends` varchar(300) DEFAULT NULL, ADD `ap_groups` varchar(300) DEFAULT NULL");
	}
}
