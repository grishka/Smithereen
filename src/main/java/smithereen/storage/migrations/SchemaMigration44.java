package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration44 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE `wall_posts` ADD `flags` bigint unsigned NOT NULL DEFAULT 0, DROP FOREIGN KEY wall_posts_ibfk_2");
	}
}
