package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration17 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE `users` ADD `about_source` TEXT NULL AFTER `about`");
		conn.createStatement().execute("ALTER TABLE `groups` ADD `about_source` TEXT NULL AFTER `about`");
		conn.createStatement().execute("ALTER TABLE `friend_requests` ADD `id` INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST");
	}
}
