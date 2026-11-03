package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

public class SchemaMigration92 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("ALTER TABLE `mail_messages` ADD `searchable_text` TEXT GENERATED ALWAYS AS (regexp_replace(`text`, '(<[^>]*>|&[^;]*;)', '')) STORED, " +
				"DROP KEY `text`, ADD FULLTEXT KEY `text` (`searchable_text`, `subject`)");
		conn.createStatement().execute("ALTER TABLE `wall_posts` ADD `searchable_text` TEXT GENERATED ALWAYS AS (regexp_replace(`text`, '(<[^>]*>|&[^;]*;)', '')) STORED, " +
				"ADD FULLTEXT KEY `text` (`searchable_text`)");
		conn.createStatement().execute("ALTER TABLE `comments` ADD `searchable_text` TEXT GENERATED ALWAYS AS (regexp_replace(`text`, '(<[^>]*>|&[^;]*;)', '')) STORED, " +
				"ADD FULLTEXT KEY `text` (`searchable_text`)");
	}
}
