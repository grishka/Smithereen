package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration51 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE newsfeed CHANGE object_id `object_id` bigint unsigned DEFAULT NULL");
		conn.createStatement().execute("ALTER TABLE newsfeed_comments CHANGE object_id `object_id` bigint unsigned NOT NULL");
		conn.createStatement().execute("ALTER TABLE notifications CHANGE object_id `object_id` bigint unsigned DEFAULT NULL, CHANGE related_object_id `related_object_id` bigint unsigned DEFAULT NULL");
		conn.createStatement().execute("ALTER TABLE likes CHANGE object_id `object_id` bigint unsigned NOT NULL");
	}
}
