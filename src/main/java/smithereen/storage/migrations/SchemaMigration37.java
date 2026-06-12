package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration37 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE accounts ADD email_domain varchar(150) NOT NULL DEFAULT '', ADD INDEX (email_domain), DROP ban_info, ADD last_ip binary(16) DEFAULT NULL, ADD INDEX (last_ip)");
		conn.createStatement().execute("UPDATE accounts SET email_domain=SUBSTR(email, LOCATE('@', email)+1)");
		conn.createStatement().execute("ALTER TABLE `users` ADD ban_status tinyint unsigned NOT NULL DEFAULT 0, ADD INDEX (ban_status), ADD ban_info json DEFAULT NULL");
	}
}
