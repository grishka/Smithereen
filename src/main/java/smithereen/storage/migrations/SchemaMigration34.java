package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.DatabaseSchemaUpdater;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.SQLQueryBuilder;

class SchemaMigration34 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `user_roles` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
				  `permissions` varbinary(255) NOT NULL,
				  PRIMARY KEY (`id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		DatabaseSchemaUpdater.insertDefaultRoles(conn);
		conn.createStatement().execute("""
				ALTER TABLE accounts ADD `role` int unsigned DEFAULT NULL,
				ADD CONSTRAINT `accounts_ibfk_2` FOREIGN KEY (`role`) REFERENCES `user_roles` (`id`) ON DELETE SET NULL,
				ADD `promoted_by` int unsigned DEFAULT NULL,
				ADD CONSTRAINT `accounts_ibfk_3` FOREIGN KEY (`promoted_by`) REFERENCES `accounts` (`id`) ON DELETE SET NULL""");
		new SQLQueryBuilder(conn)
				.update("accounts")
				.where("access_level=2") // moderator -> new moderator role
				.value("role", 3)
				.executeNoResult();
		new SQLQueryBuilder(conn)
				.update("accounts")
				.where("access_level=3") // admin -> new admin role
				.value("role", 2)
				.executeNoResult();
		new SQLQueryBuilder(conn)
				.update("accounts")
				.where("access_level=3 AND id=1") // first admin -> new owner role
				.value("role", 1)
				.executeNoResult();
		conn.createStatement().execute("ALTER TABLE accounts DROP access_level");
	}
}
