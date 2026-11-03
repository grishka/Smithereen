package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

class SchemaMigration79 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE `group_links` (
				  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
				  `group_id` int unsigned NOT NULL,
				  `url` varchar(300) NOT NULL,
				  `title` varchar(300) NOT NULL,
				  `object_type` int unsigned DEFAULT NULL,
				  `object_id` bigint unsigned DEFAULT NULL,
				  `image_id` bigint DEFAULT NULL,
				  `ap_image_url` varchar(300) DEFAULT NULL,
				  `display_order` int unsigned NOT NULL DEFAULT '0',
				  `ap_id` varchar(300) DEFAULT NULL,
				  PRIMARY KEY (`id`),
				  UNIQUE KEY `ap_id` (`ap_id`),
				  KEY `group_id` (`group_id`),
				  CONSTRAINT `group_links_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
	}
}
