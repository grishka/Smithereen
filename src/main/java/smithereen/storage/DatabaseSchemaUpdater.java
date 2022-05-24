package smithereen.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;

public class DatabaseSchemaUpdater{
	public static final int SCHEMA_VERSION=23;
	private static final Logger LOG=LoggerFactory.getLogger(DatabaseSchemaUpdater.class);

	public static void maybeUpdate() throws SQLException{
		if(Config.dbSchemaVersion==0){
			Config.updateInDatabase("SchemaVersion", SCHEMA_VERSION+"");
			Connection conn=DatabaseConnectionManager.getConnection();
			conn.createStatement().execute("""
					CREATE FUNCTION `bin_prefix`(p VARBINARY(1024)) RETURNS varbinary(2048) DETERMINISTIC
					RETURN CONCAT(REPLACE(REPLACE(REPLACE(p, BINARY(0xFF), BINARY(0xFFFF)), '%', BINARY(0xFF25)), '_', BINARY(0xFF5F)), '%');""");
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
		LOG.info("Updating database schema {} -> {}", Config.dbSchemaVersion, target);
		Connection conn=DatabaseConnectionManager.getConnection();
		switch(target){
			case 2 -> conn.createStatement().execute("ALTER TABLE wall_posts ADD (reply_count INTEGER UNSIGNED NOT NULL DEFAULT 0)");
			case 3 -> conn.createStatement().execute("ALTER TABLE users ADD middle_name VARCHAR(100) DEFAULT NULL AFTER lname, ADD maiden_name VARCHAR(100) DEFAULT NULL AFTER middle_name");
			case 4 -> {
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
			case 5 -> {
				conn.createStatement().execute("""
						CREATE TABLE `blocks_group_domain` (
						  `owner_id` int(10) unsigned NOT NULL,
						  `domain` varchar(100) CHARACTER SET ascii NOT NULL,
						  UNIQUE KEY `owner_id` (`owner_id`,`domain`),
						  KEY `domain` (`domain`),
						  CONSTRAINT `blocks_group_domain_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("""
						CREATE TABLE `blocks_group_user` (
						  `owner_id` int(10) unsigned NOT NULL,
						  `user_id` int(10) unsigned NOT NULL,
						  UNIQUE KEY `owner_id` (`owner_id`,`user_id`),
						  KEY `user_id` (`user_id`),
						  CONSTRAINT `blocks_group_user_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
						  CONSTRAINT `blocks_group_user_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("""
						CREATE TABLE `blocks_user_domain` (
						  `owner_id` int(10) unsigned NOT NULL,
						  `domain` varchar(100) CHARACTER SET ascii NOT NULL,
						  UNIQUE KEY `owner_id` (`owner_id`,`domain`),
						  KEY `domain` (`domain`),
						  CONSTRAINT `blocks_user_domain_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("""
						CREATE TABLE `blocks_user_user` (
						  `owner_id` int(10) unsigned NOT NULL,
						  `user_id` int(10) unsigned NOT NULL,
						  UNIQUE KEY `owner_id` (`owner_id`,`user_id`),
						  KEY `user_id` (`user_id`),
						  CONSTRAINT `blocks_user_user_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
						  CONSTRAINT `blocks_user_user_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
			}
			case 6 -> conn.createStatement().execute("""
					CREATE TABLE `email_codes` (
					  `code` binary(64) NOT NULL,
					  `account_id` int(10) unsigned DEFAULT NULL,
					  `type` int(11) NOT NULL,
					  `extra` text,
					  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
					  PRIMARY KEY (`code`),
					  KEY `account_id` (`account_id`),
					  CONSTRAINT `email_codes_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
					) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
			case 7 -> conn.createStatement().execute("ALTER TABLE accounts ADD `last_active` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP");
			case 8 -> conn.createStatement().execute("ALTER TABLE accounts ADD `ban_info` text DEFAULT NULL");
			case 9 -> conn.createStatement().execute("ALTER TABLE users ADD `ap_friends` varchar(300) DEFAULT NULL, ADD `ap_groups` varchar(300) DEFAULT NULL");
			case 10 -> {
				conn.createStatement().execute("ALTER TABLE likes ADD `ap_id` varchar(300) DEFAULT NULL");
				conn.createStatement().execute("UPDATE likes SET object_type=0");
			}
			case 11 -> {
				conn.createStatement().execute("""
						CREATE TABLE `qsearch_index` (
						  `string` text NOT NULL,
						  `user_id` int(10) unsigned DEFAULT NULL,
						  `group_id` int(10) unsigned DEFAULT NULL,
						  KEY `user_id` (`user_id`),
						  KEY `group_id` (`group_id`),
						  FULLTEXT KEY `string` (`string`),
						  CONSTRAINT `qsearch_index_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
						  CONSTRAINT `qsearch_index_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=ascii;""");
				try(ResultSet res=conn.createStatement().executeQuery("SELECT id, fname, lname, middle_name, maiden_name, username, domain FROM users")){
					res.beforeFirst();
					PreparedStatement stmt=conn.prepareStatement("INSERT INTO qsearch_index (string, user_id) VALUES (?, ?)");
					while(res.next()){
						int id=res.getInt("id");
						String fname=res.getString("fname");
						String lname=res.getString("lname");
						String mname=res.getString("middle_name");
						String mdname=res.getString("maiden_name");
						String uname=res.getString("username");
						String domain=res.getString("domain");
						StringBuilder sb=new StringBuilder(Utils.transliterate(fname));
						if(lname!=null){
							sb.append(' ');
							sb.append(Utils.transliterate(lname));
						}
						if(mname!=null){
							sb.append(' ');
							sb.append(Utils.transliterate(mname));
						}
						if(mdname!=null){
							sb.append(' ');
							sb.append(Utils.transliterate(mdname));
						}
						sb.append(' ');
						sb.append(uname);
						if(domain!=null){
							sb.append(' ');
							sb.append(domain);
						}
						stmt.setString(1, sb.toString());
						stmt.setInt(2, id);
						stmt.execute();
					}
				}
				try(ResultSet res=conn.createStatement().executeQuery("SELECT id, name, username, domain FROM groups")){
					res.beforeFirst();
					PreparedStatement stmt=conn.prepareStatement("INSERT INTO qsearch_index (string, group_id) VALUES (?, ?)");
					while(res.next()){
						String s=Utils.transliterate(res.getString("name"))+" "+res.getString("username");
						String domain=res.getString("domain");
						if(domain!=null)
							s+=" "+domain;
						stmt.setString(1, s);
						stmt.setInt(2, res.getInt("id"));
						stmt.execute();
					}
				}
			}
			case 12 -> conn.createStatement().execute("ALTER TABLE wall_posts ADD `ap_replies` varchar(300) DEFAULT NULL");
			case 13 -> {
				conn.createStatement().execute("""
						CREATE TABLE `polls` (
						  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
						  `owner_id` int(10) unsigned NOT NULL,
						  `ap_id` varchar(300) CHARACTER SET ascii DEFAULT NULL,
						  `question` text,
						  `is_anonymous` tinyint(1) NOT NULL DEFAULT '0',
						  `is_multi_choice` tinyint(1) NOT NULL DEFAULT '0',
						  `end_time` timestamp NULL DEFAULT NULL,
						  `num_voted_users` int(10) unsigned NOT NULL DEFAULT '0',
						  PRIMARY KEY (`id`),
						  UNIQUE KEY `ap_id` (`ap_id`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("""
						CREATE TABLE `poll_options` (
						  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
						  `poll_id` int(10) unsigned NOT NULL,
						  `ap_id` varchar(300) CHARACTER SET ascii DEFAULT NULL,
						  `text` text NOT NULL,
						  `num_votes` int(10) unsigned NOT NULL DEFAULT '0',
						  PRIMARY KEY (`id`),
						  UNIQUE KEY `ap_id` (`ap_id`),
						  KEY `poll_id` (`poll_id`),
						  CONSTRAINT `poll_options_ibfk_1` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("""
						CREATE TABLE `poll_votes` (
						  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
						  `user_id` int(11) unsigned NOT NULL,
						  `poll_id` int(10) unsigned NOT NULL,
						  `option_id` int(10) unsigned NOT NULL,
						  `ap_id` varchar(300) CHARACTER SET ascii DEFAULT NULL,
						  PRIMARY KEY (`id`),
						  UNIQUE KEY `ap_id` (`ap_id`),
						  KEY `user_id` (`user_id`),
						  KEY `poll_id` (`poll_id`),
						  KEY `option_id` (`option_id`),
						  CONSTRAINT `poll_votes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
						  CONSTRAINT `poll_votes_ibfk_2` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE,
						  CONSTRAINT `poll_votes_ibfk_3` FOREIGN KEY (`option_id`) REFERENCES `poll_options` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("ALTER TABLE wall_posts ADD `poll_id` int(10) unsigned DEFAULT NULL");
			}
			case 14 -> conn.createStatement().execute("""
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
			case 15 -> conn.createStatement().execute("ALTER TABLE `wall_posts` ADD `federation_state` tinyint unsigned NOT NULL DEFAULT 0");
			case 16 -> conn.createStatement().execute("ALTER TABLE `wall_posts` ADD `source` text DEFAULT NULL, ADD `source_format` tinyint unsigned DEFAULT NULL");
			case 17 -> {
				conn.createStatement().execute("ALTER TABLE `users` ADD `about_source` TEXT NULL AFTER `about`");
				conn.createStatement().execute("ALTER TABLE `groups` ADD `about_source` TEXT NULL AFTER `about`");
				conn.createStatement().execute("ALTER TABLE `friend_requests` ADD `id` INT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST");
			}
			case 18 -> conn.createStatement().execute("ALTER TABLE `groups` ADD `flags` BIGINT UNSIGNED NOT NULL DEFAULT 0");
			case 19 -> conn.createStatement().execute("""
					CREATE TABLE `group_invites` (
					`id` int unsigned NOT NULL AUTO_INCREMENT,
							`inviter_id` int unsigned NOT NULL,
							`invitee_id` int unsigned NOT NULL,
							`group_id` int unsigned NOT NULL,
							`time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
							`is_event` tinyint(1) NOT NULL,
							`ap_id` varchar(300) CHARACTER SET ascii DEFAULT NULL,
							PRIMARY KEY (`id`),
							UNIQUE KEY `ap_id` (`ap_id`),
							KEY `inviter_id` (`inviter_id`),
							KEY `invitee_id` (`invitee_id`),
							KEY `group_id` (`group_id`),
							KEY `is_event` (`is_event`),
							CONSTRAINT `group_invites_ibfk_1` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
							CONSTRAINT `group_invites_ibfk_2` FOREIGN KEY (`invitee_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
							CONSTRAINT `group_invites_ibfk_3` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
					) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
			case 20 -> {
				// Make room for a new column
				conn.createStatement().execute("ALTER TABLE `users` DROP KEY `ap_outbox`");
				conn.createStatement().execute("ALTER TABLE `users` CHANGE `ap_outbox` `ap_outbox` TEXT");
				conn.createStatement().execute("ALTER TABLE `groups` CHANGE `ap_outbox` `ap_outbox` TEXT");
				// Then add the new column
				conn.createStatement().execute("ALTER TABLE `users` ADD `endpoints` json DEFAULT NULL");
				conn.createStatement().execute("ALTER TABLE `groups` ADD `endpoints` json DEFAULT NULL");
				PreparedStatement stmt=conn.prepareStatement("UPDATE `users` SET `endpoints`=? WHERE `id`=?");
				try(ResultSet res=conn.createStatement().executeQuery("SELECT `id`,`ap_outbox`,`ap_followers`,`ap_following`,`ap_wall`,`ap_friends`,`ap_groups` FROM `users` WHERE `ap_id` IS NOT NULL")){
					res.beforeFirst();
					while(res.next()){
						int id=res.getInt(1);
						Actor.EndpointsStorageWrapper ep=new Actor.EndpointsStorageWrapper();
						ep.outbox=res.getString(2);
						ep.followers=res.getString(3);
						ep.following=res.getString(4);
						ep.wall=res.getString(5);
						ep.friends=res.getString(6);
						ep.groups=res.getString(7);
						stmt.setString(1, Utils.gson.toJson(ep));
						stmt.setInt(2, id);
						stmt.execute();
					}
				}
				stmt=conn.prepareStatement("UPDATE `groups` SET `endpoints`=? WHERE `id`=?");
				try(ResultSet res=conn.createStatement().executeQuery("SELECT `id`,`ap_outbox`,`ap_followers`,`ap_wall` FROM `groups` WHERE `ap_id` IS NOT NULL")){
					res.beforeFirst();
					while(res.next()){
						int id=res.getInt(1);
						Actor.EndpointsStorageWrapper ep=new Actor.EndpointsStorageWrapper();
						ep.outbox=res.getString(2);
						ep.followers=res.getString(3);
						ep.wall=res.getString(4);
						stmt.setString(1, Utils.gson.toJson(ep));
						stmt.setInt(2, id);
						stmt.execute();
					}
				}
				conn.createStatement().execute("ALTER TABLE `users` DROP `ap_outbox`, DROP `ap_followers`, DROP `ap_following`, DROP `ap_wall`, DROP `ap_friends`, DROP `ap_groups`");
				conn.createStatement().execute("ALTER TABLE `groups` DROP `ap_outbox`, DROP `ap_followers`, DROP `ap_wall`");
				conn.createStatement().execute("ALTER TABLE `groups` ADD `access_type` tinyint NOT NULL DEFAULT '0'");
			}
			case 21 -> conn.createStatement().execute("ALTER TABLE `group_memberships` ADD `time` timestamp DEFAULT CURRENT_TIMESTAMP()");
			case 22 -> conn.createStatement().execute("ALTER TABLE `polls` CHANGE `owner_id` `owner_id` int NOT NULL");
			case 23 -> {
				conn.createStatement().execute("ALTER TABLE `polls` ADD `last_vote_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP()");
				conn.createStatement().execute("CREATE INDEX `poll_id` ON `wall_posts` (`poll_id`)");
			}
		}
	}
}
