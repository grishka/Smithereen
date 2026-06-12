package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration85 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `user_data_exports` (
				  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
				  `user_id` int unsigned NOT NULL,
				  `state` tinyint unsigned NOT NULL,
				  `size` bigint unsigned NOT NULL DEFAULT '0',
				  `file_id` bigint DEFAULT NULL,
				  `requested_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (`id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
