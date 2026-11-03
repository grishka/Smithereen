package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration43 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("DROP FUNCTION `bin_prefix`");
		conn.createStatement().execute("""
				CREATE FUNCTION `bin_prefix`(p VARBINARY(1024)) RETURNS varbinary(2048) DETERMINISTIC
				RETURN CONCAT(REPLACE(REPLACE(REPLACE(p, '\\\\', '\\\\\\\\'), '%', '\\\\%'), '_', '\\\\_'), '%');""");
	}
}
