package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration67 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("UPDATE `users` SET num_followers=(SELECT COUNT(*) FROM followings WHERE followee_id=id)");
		conn.createStatement().execute("UPDATE `users` SET num_following=(SELECT COUNT(*) FROM followings WHERE follower_id=id)");
		conn.createStatement().execute("UPDATE `users` SET num_friends=(SELECT COUNT(*) FROM followings WHERE follower_id=id AND mutual=1)");
	}
}
