package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration25 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `signup_requests` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `email` varchar(200) NOT NULL,
				  `first_name` varchar(100) NOT NULL,
				  `last_name` varchar(100) DEFAULT NULL,
				  `reason` text NOT NULL,
				  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (`id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		conn.createStatement().execute("ALTER TABLE `signup_invitations` ADD `email` varchar(200) DEFAULT NULL, ADD `extra` json DEFAULT NULL, ADD `id` int unsigned NOT NULL AUTO_INCREMENT, ADD UNIQUE (`id`), ADD UNIQUE INDEX (`email`)");
		conn.createStatement().execute("ALTER TABLE `accounts` ADD UNIQUE INDEX (`email`), ADD INDEX (`invited_by`)");
	}
}
