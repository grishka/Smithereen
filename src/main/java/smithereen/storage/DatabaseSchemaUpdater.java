package smithereen.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import smithereen.Config;

public class DatabaseSchemaUpdater{
	public static final int SCHEMA_VERSION=4;

	public static void maybeUpdate() throws SQLException{
		if(Config.dbSchemaVersion==0){
			Config.updateInDatabase("SchemaVersion", SCHEMA_VERSION+"");
		}else{
			for(int i=Config.dbSchemaVersion+1;i<=SCHEMA_VERSION;i++){
				Connection conn=DatabaseConnectionManager.getConnection();
				conn.createStatement().execute("START TRANSACTION");
				try{
					updateFromPrevious(i);
					Config.updateInDatabase("SchemaVersion", i+"");
					Config.dbSchemaVersion=i;
				}catch(Exception x){
					conn.createStatement().execute("ROLLBACK");
					throw new RuntimeException(x);
				}
				conn.createStatement().execute("COMMIT");
			}
		}
	}

	private static void updateFromPrevious(int target) throws SQLException{
		System.out.println("Updating database schema "+Config.dbSchemaVersion+" -> "+target);
		Connection conn=DatabaseConnectionManager.getConnection();
		if(target==2){
			conn.createStatement().execute("ALTER TABLE wall_posts ADD (reply_count INTEGER UNSIGNED NOT NULL DEFAULT 0)");
		}else if(target==3){
			conn.createStatement().execute("ALTER TABLE users ADD middle_name VARCHAR(100) DEFAULT NULL AFTER lname, ADD maiden_name VARCHAR(100) DEFAULT NULL AFTER middle_name");
		}else if(target==4){
			conn.createStatement().execute("""
					CREATE TABLE `groups` (
					  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
					  `name` varchar(200) NOT NULL DEFAULT '',
					  `username` varchar(50) NOT NULL DEFAULT '',
					  `domain` varchar(100) NOT NULL DEFAULT '',
					  `ap_id` varchar(300) CHARACTER SET ascii DEFAULT NULL,
					  `ap_url` varchar(300) DEFAULT NULL,
					  `ap_inbox` varchar(300) DEFAULT NULL,
					  `ap_shared_inbox` varchar(300) DEFAULT NULL,
					  `ap_outbox` varchar(300) DEFAULT NULL,
					  `public_key` blob NOT NULL,
					  `private_key` blob,
					  `avatar` text,
					  `about` text,
					  `profile_fields` text,
					  `event_start_time` timestamp NULL DEFAULT NULL,
					  `event_end_time` timestamp NULL DEFAULT NULL,
					  `type` tinyint(3) unsigned NOT NULL DEFAULT '0',
					  `member_count` int(10) unsigned NOT NULL DEFAULT '0',
					  `tentative_member_count` int(10) unsigned NOT NULL DEFAULT '0',
					  `ap_followers` varchar(300) DEFAULT NULL,
					  `ap_wall` varchar(300) DEFAULT NULL,
					  `last_updated` timestamp NULL DEFAULT NULL,
					  PRIMARY KEY (`id`),
					  UNIQUE KEY `username` (`username`,`domain`),
					  UNIQUE KEY `ap_id` (`ap_id`),
					  KEY `type` (`type`)
					) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");

			conn.createStatement().execute("""
					CREATE TABLE `group_admins` (
					  `user_id` int(11) unsigned NOT NULL,
					  `group_id` int(11) unsigned NOT NULL,
					  `level` int(11) unsigned NOT NULL,
					  `title` varchar(300) DEFAULT NULL,
					  `display_order` int(10) unsigned NOT NULL DEFAULT '0',
					  KEY `user_id` (`user_id`),
					  KEY `group_id` (`group_id`),
					  CONSTRAINT `group_admins_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
					  CONSTRAINT `group_admins_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
					) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");

			conn.createStatement().execute("""
					CREATE TABLE `group_memberships` (
					  `user_id` int(11) unsigned NOT NULL,
					  `group_id` int(11) unsigned NOT NULL,
					  `post_feed_visibility` tinyint(4) unsigned NOT NULL DEFAULT '0',
					  `tentative` tinyint(1) NOT NULL DEFAULT '0',
					  `accepted` tinyint(1) NOT NULL DEFAULT '1',
					  UNIQUE KEY `user_id` (`user_id`,`group_id`),
					  KEY `group_id` (`group_id`),
					  CONSTRAINT `group_memberships_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
					  CONSTRAINT `group_memberships_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
					) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");

			conn.createStatement().execute("ALTER TABLE users ADD `ap_wall` varchar(300) DEFAULT NULL");
		}
	}
}
