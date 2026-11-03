package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration62 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `word_filters` (
				  `id` int unsigned NOT NULL AUTO_INCREMENT,
				  `owner_id` int unsigned NOT NULL,
				  `name` varchar(300) COLLATE utf8mb4_general_ci NOT NULL,
				  `words` json NOT NULL,
				  `contexts` bit(32) NOT NULL,
				  `expires_at` timestamp NULL DEFAULT NULL,
				  `action` tinyint unsigned NOT NULL,
				  PRIMARY KEY (`id`),
				  KEY `owner_id` (`owner_id`),
				  KEY `expires_at` (`expires_at`),
				  CONSTRAINT `word_filters_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
