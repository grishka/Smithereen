package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.SQLQueryBuilder;

class SchemaMigration91 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		new SQLQueryBuilder(conn)
				.deleteFrom("newsfeed")
				.where("author_id NOT IN (SELECT id FROM `users`)")
				.executeNoResult();
		new SQLQueryBuilder(conn)
				.deleteFrom("newsfeed_groups")
				.where("group_id NOT IN (SELECT id FROM `groups`)")
				.executeNoResult();
		conn.createStatement().execute("ALTER TABLE newsfeed CHANGE author_id `author_id` int unsigned NOT NULL, ADD CONSTRAINT `newsfeed_users_fk` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE");
		conn.createStatement().execute("ALTER TABLE newsfeed_groups ADD CONSTRAINT `newsfeed_groups_groups_fk` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE");
	}
}
