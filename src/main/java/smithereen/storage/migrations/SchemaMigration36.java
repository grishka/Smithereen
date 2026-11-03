package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration36 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE sessions DROP last_ip, ADD `ip` binary(16) NOT NULL, ADD `user_agent` bigint NOT NULL");
		conn.createStatement().execute("""
				CREATE TABLE `user_agents` (
				  `hash` bigint NOT NULL,
				  `user_agent` text COLLATE utf8mb4_general_ci NOT NULL,
				  PRIMARY KEY (`hash`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
