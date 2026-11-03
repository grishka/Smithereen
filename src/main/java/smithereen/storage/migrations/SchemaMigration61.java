package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration61 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE followings ADD `muted` tinyint(1) NOT NULL DEFAULT '0', ADD `hints_rank` int unsigned NOT NULL DEFAULT '0',"+
				" ADD `lists` bit(64) NOT NULL DEFAULT b'0', ADD KEY `muted` (`muted`), ADD KEY `hints_rank` (`hints_rank`)");
	}
}
