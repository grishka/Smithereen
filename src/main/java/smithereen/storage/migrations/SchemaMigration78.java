package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration78 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `announcements` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `title` varchar(300) DEFAULT NULL,
				  `description` text NOT NULL,
				  `link_text` varchar(300) DEFAULT NULL,
				  `link_url` varchar(300) DEFAULT NULL,
				  `show_from` timestamp NOT NULL,
				  `show_to` timestamp NOT NULL,
				  `translations` json NOT NULL,
				  PRIMARY KEY (`id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
