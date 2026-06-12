package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration68 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE `users` ADD KEY num_followers (num_followers)");
	}
}
