package smithereen.storage.migrations;

import java.sql.SQLException;

import smithereen.controllers.ObjectLinkResolver;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.SQLQueryBuilder;

class SchemaMigration48 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
				CREATE TABLE IF NOT EXISTS `ap_id_index` (
				  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
				  `object_type` int unsigned NOT NULL,
				  `object_id` bigint unsigned NOT NULL,
				  PRIMARY KEY (`ap_id`),
				  UNIQUE KEY `object_type` (`object_type`,`object_id`)
				) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
		SQLQueryBuilder.prepareStatement(conn,
				"INSERT IGNORE INTO `ap_id_index` (ap_id, object_type, object_id) SELECT ap_id, ?, id FROM `users` WHERE `domain` IS NOT NULL",
				ObjectLinkResolver.ObjectType.USER.id).execute();
		SQLQueryBuilder.prepareStatement(conn,
				"INSERT IGNORE INTO `ap_id_index` (ap_id, object_type, object_id) SELECT ap_id, ?, id FROM `groups` WHERE `domain` IS NOT NULL",
				ObjectLinkResolver.ObjectType.GROUP.id).execute();
		SQLQueryBuilder.prepareStatement(conn,
				"INSERT IGNORE INTO `ap_id_index` (ap_id, object_type, object_id) SELECT ap_id, ?, id FROM `wall_posts` WHERE `ap_id` IS NOT NULL",
				ObjectLinkResolver.ObjectType.POST.id).execute();
		SQLQueryBuilder.prepareStatement(conn,
				"INSERT IGNORE INTO `ap_id_index` (ap_id, object_type, object_id) SELECT ap_id, ?, id FROM `mail_messages` WHERE `ap_id` IS NOT NULL",
				ObjectLinkResolver.ObjectType.MESSAGE.id).execute();
	}
}
