package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration86 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE notifications ADD KEY `object_type` (`object_type`,`object_id`), ADD KEY `type` (`type`), ADD KEY `related_object_type` (`related_object_type`,`related_object_id`), ADD KEY `actor_id` (`actor_id`)");
	}
}
