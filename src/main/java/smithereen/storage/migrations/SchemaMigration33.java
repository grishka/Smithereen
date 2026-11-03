package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration33 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		// Allow deleting users while keeping their IDs in these tables
		conn.createStatement().execute("ALTER TABLE reports DROP FOREIGN KEY reports_ibfk_1, DROP FOREIGN KEY reports_ibfk_2");
		conn.createStatement().execute("ALTER TABLE wall_posts DROP FOREIGN KEY wall_posts_ibfk_3");
	}
}
