package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration14 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `newsfeed_comments` (
				  `user_id` int(10) unsigned NOT NULL,
				  `object_type` int(10) unsigned NOT NULL,
				  `object_id` int(10) unsigned NOT NULL,
				  `last_comment_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (`object_type`,`object_id`,`user_id`),
				  KEY `user_id` (`user_id`),
				  KEY `last_comment_time` (`last_comment_time`),
				  CONSTRAINT `newsfeed_comments_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
