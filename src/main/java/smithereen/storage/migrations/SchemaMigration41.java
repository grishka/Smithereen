package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration41 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `user_staff_notes` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `target_id` int unsigned NOT NULL,
				  `author_id` int unsigned NOT NULL,
				  `text` text COLLATE utf8mb4_general_ci NOT NULL,
				  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
				  PRIMARY KEY (`id`),
				  KEY `target_id` (`target_id`),
				  CONSTRAINT `user_staff_notes_ibfk_1` FOREIGN KEY (`target_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;""");
	}
}
