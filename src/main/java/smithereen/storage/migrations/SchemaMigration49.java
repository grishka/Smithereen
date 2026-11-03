package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.DatabaseSchemaUpdater;
import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration49 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		DatabaseSchemaUpdater.createApIdIndexTriggers(conn);
	}
}
