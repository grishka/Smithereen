package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration72 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `deleted_user_bans` (
				  `user_id` int unsigned NOT NULL,
				  `domain` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
				  `ban_status` tinyint unsigned NOT NULL DEFAULT '0',
				  `ban_info` json NOT NULL,
				  PRIMARY KEY (`user_id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
