package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration45 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE `users` CHANGE `username` `username` varchar(64) NOT NULL");
		conn.createStatement().execute("ALTER TABLE `groups` CHANGE `username` `username` varchar(64) NOT NULL");
	}
}
