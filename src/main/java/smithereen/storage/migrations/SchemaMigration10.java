package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration10 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE likes ADD `ap_id` varchar(300) DEFAULT NULL");
		conn.createStatement().execute("UPDATE likes SET object_type=0");
	}
}
