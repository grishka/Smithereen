package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration23 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE `polls` ADD `last_vote_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP()");
		conn.createStatement().execute("CREATE INDEX `poll_id` ON `wall_posts` (`poll_id`)");
	}
}
