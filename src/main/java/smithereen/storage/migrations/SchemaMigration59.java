package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration59 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE photo_tags ADD ap_id varchar(300) CHARACTER SET ascii DEFAULT NULL, ADD UNIQUE KEY ap_id (ap_id)");
	}
}
