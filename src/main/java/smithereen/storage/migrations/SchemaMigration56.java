package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration56 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE photo_albums ADD `ap_comments` varchar(300) CHARACTER SET ascii DEFAULT NULL");
	}
}
