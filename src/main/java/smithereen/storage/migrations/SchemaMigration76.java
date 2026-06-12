package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration76 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `rules` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `title` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
				  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
				  `translations` json NOT NULL,
				  `priority` int NOT NULL DEFAULT '0',
				  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
				  PRIMARY KEY (`id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
