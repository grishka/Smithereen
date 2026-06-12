package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration84 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `wall_pinned_posts` (
				  `owner_user_id` int unsigned NOT NULL,
				  `post_id` int unsigned NOT NULL,
				  `display_order` int unsigned NOT NULL,
				  PRIMARY KEY (`owner_user_id`,`post_id`),
				  KEY `post_id` (`post_id`),
				  CONSTRAINT `wall_pinned_posts_ibfk_1` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
				  CONSTRAINT `wall_pinned_posts_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `wall_posts` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
