package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration3 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE users ADD middle_name VARCHAR(100) DEFAULT NULL AFTER lname, ADD maiden_name VARCHAR(100) DEFAULT NULL AFTER middle_name");
	}
}
