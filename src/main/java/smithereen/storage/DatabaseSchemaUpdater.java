package smithereen.storage;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import smithereen.Config;
import smithereen.Utils;
import smithereen.activitypub.objects.Actor;
import smithereen.controllers.ObjectLinkResolver;
import smithereen.model.ObfuscatedObjectIDType;
import smithereen.model.UserRole;
import smithereen.model.media.ImageMetadata;
import smithereen.model.media.MediaFileReferenceType;
import smithereen.model.media.MediaFileType;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.DatabaseConnectionManager;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.text.TextProcessor;
import smithereen.util.JsonObjectBuilder;
import smithereen.util.Passwords;
import smithereen.util.XTEA;

public class DatabaseSchemaUpdater{
	public static final int SCHEMA_VERSION=70;
	private static final Logger LOG=LoggerFactory.getLogger(DatabaseSchemaUpdater.class);

	public static void maybeUpdate() throws SQLException{
		if(Config.dbSchemaVersion==0){
			Config.updateInDatabase("SchemaVersion", SCHEMA_VERSION+"");
			try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
				conn.createStatement().execute("""
						CREATE FUNCTION `bin_prefix`(p VARBINARY(1024)) RETURNS varbinary(2048) DETERMINISTIC
						RETURN CONCAT(REPLACE(REPLACE(REPLACE(p, '\\\\', '\\\\\\\\'), '%', '\\\\%'), '_', '\\\\_'), '%');""");
				createMediaRefCountTriggers(conn);
				createApIdIndexTriggers(conn);
				createApIdIndexTriggersForPhotos(conn);
				createApIdIndexTriggersForComments(conn);
				insertDefaultRoles(conn);
			}
		}else{
			for(int i=Config.dbSchemaVersion+1;i<=SCHEMA_VERSION;i++){
				try(DatabaseConnection conn=DatabaseConnectionManager.getConnection()){
					conn.createStatement().execute("START TRANSACTION");
					try{
						updateFromPrevious(conn, i);
						Config.updateInDatabase("SchemaVersion", String.valueOf(i));
						Config.dbSchemaVersion=i;
					}catch(Exception x){
						conn.createStatement().execute("ROLLBACK");
						throw new RuntimeException(x);
					}
					conn.createStatement().execute("COMMIT");
				}
			}
		}
	}

	private static void updateFromPrevious(DatabaseConnection conn, int target) throws SQLException{
		LOG.info("Updating database schema {} -> {}", Config.dbSchemaVersion, target);
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
					PreparedStatement stmt=conn.prepareStatement("INSERT INTO qsearch_index (string, user_id) VALUES (?, ?)");
					while(res.next()){
						int id=res.getInt("id");
						String fname=res.getString("fname");
						String lname=res.getString("lname");
						String mname=res.getString("middle_name");
						String mdname=res.getString("maiden_name");
						String uname=res.getString("username");
						String domain=res.getString("domain");
						StringBuilder sb=new StringBuilder(TextProcessor.transliterate(fname));
						if(lname!=null){
							sb.append(' ');
							sb.append(TextProcessor.transliterate(lname));
						}
						if(mname!=null){
							sb.append(' ');
							sb.append(TextProcessor.transliterate(mname));
						}
						if(mdname!=null){
							sb.append(' ');
							sb.append(TextProcessor.transliterate(mdname));
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
					PreparedStatement stmt=conn.prepareStatement("INSERT INTO qsearch_index (string, group_id) VALUES (?, ?)");
					while(res.next()){
						String s=TextProcessor.transliterate(res.getString("name"))+" "+res.getString("username");
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
			case 24 -> conn.createStatement().execute("ALTER TABLE `accounts` ADD `activation_info` json DEFAULT NULL");
			case 25 -> {
				conn.createStatement().execute("""
						CREATE TABLE `signup_requests` (
						  `id` int unsigned NOT NULL AUTO_INCREMENT,
						  `email` varchar(200) NOT NULL,
						  `first_name` varchar(100) NOT NULL,
						  `last_name` varchar(100) DEFAULT NULL,
						  `reason` text NOT NULL,
						  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  PRIMARY KEY (`id`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("ALTER TABLE `signup_invitations` ADD `email` varchar(200) DEFAULT NULL, ADD `extra` json DEFAULT NULL, ADD `id` int unsigned NOT NULL AUTO_INCREMENT, ADD UNIQUE (`id`), ADD UNIQUE INDEX (`email`)");
				conn.createStatement().execute("ALTER TABLE `accounts` ADD UNIQUE INDEX (`email`), ADD INDEX (`invited_by`)");
			}
			case 26 -> conn.createStatement().execute("ALTER TABLE `signup_requests` ADD UNIQUE INDEX (`email`)");
			case 27 -> {
				conn.createStatement().execute("""
						CREATE TABLE `reports` (
							`id` int unsigned NOT NULL AUTO_INCREMENT,
							`reporter_id` int unsigned NULL DEFAULT NULL,
							`target_type` tinyint unsigned NOT NULL,
							`content_type` tinyint unsigned DEFAULT NULL,
							`target_id` int unsigned NOT NULL,
							`content_id` int unsigned DEFAULT NULL,
							`comment` text NOT NULL,
							`moderator_id` int unsigned DEFAULT NULL,
							`time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
							`action_time` timestamp NULL DEFAULT NULL,
							`server_domain` varchar(100) NULL DEFAULT NULL,
							PRIMARY KEY (`id`),
							KEY `reporter_id` (`reporter_id`),
							KEY `moderator_id` (`moderator_id`),
							CONSTRAINT `reports_ibfk_1` FOREIGN KEY (`reporter_id`) REFERENCES `users` (`id`),
							CONSTRAINT `reports_ibfk_2` FOREIGN KEY (`moderator_id`) REFERENCES `users` (`id`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
			}
			case 28 -> {
				conn.createStatement().execute("DROP TABLE IF EXISTS `servers`");
				conn.createStatement().execute("""
						CREATE TABLE `servers` (
						   `id` int unsigned NOT NULL AUTO_INCREMENT,
						   `host` varchar(100) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
						   `software` varchar(100) DEFAULT NULL,
						   `version` varchar(30) DEFAULT NULL,
						   `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						   `last_error_day` date DEFAULT NULL,
						   `error_day_count` int NOT NULL DEFAULT '0',
						   `is_up` tinyint(1) unsigned NOT NULL DEFAULT '1',
						   `is_restricted` tinyint(1) unsigned NOT NULL DEFAULT '0',
						   `restriction` json DEFAULT NULL,
						   PRIMARY KEY (`id`),
						   UNIQUE KEY (`host`),
						   KEY `is_up` (`is_up`),
						   KEY `is_restricted` (`is_restricted`),
						   KEY `error_day_count` (`error_day_count`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				HashSet<String> servers=new HashSet<>();
				servers.addAll(new SQLQueryBuilder(conn).selectFrom("users").columns("domain").distinct().where("domain<>''").executeAsStream(rs->rs.getString(1).toLowerCase()).collect(Collectors.toSet()));
				servers.addAll(new SQLQueryBuilder(conn).selectFrom("groups").columns("domain").distinct().where("domain<>''").executeAsStream(rs->rs.getString(1).toLowerCase()).collect(Collectors.toSet()));
				PreparedStatement stmt=conn.prepareStatement("INSERT INTO `servers` (`host`) VALUES (?)");
				for(String domain:servers){
					stmt.setString(1, domain);
					stmt.execute();
				}
				conn.createStatement().execute("""
						CREATE TABLE `stats_daily` (
						  `day` date NOT NULL,
						  `type` int unsigned NOT NULL,
						  `object_id` int unsigned NOT NULL,
						  `count` int unsigned NOT NULL,
						  PRIMARY KEY (`day`,`type`,`object_id`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
			}
			case 29 -> conn.createStatement().execute("ALTER TABLE `users` ADD `privacy` json DEFAULT NULL");
			case 30 ->{
				conn.createStatement().execute("""
						CREATE TABLE `mail_messages` (
						  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
						  `owner_id` int unsigned NOT NULL,
						  `sender_id` int unsigned NOT NULL,
						  `to` varbinary(1024) NOT NULL,
						  `cc` varbinary(1024) DEFAULT NULL,
						  `text` text NOT NULL,
						  `subject` text NOT NULL,
						  `attachments` json DEFAULT NULL,
						  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `updated_at` timestamp NULL DEFAULT NULL,
						  `deleted_at` timestamp NULL DEFAULT NULL,
						  `read_receipts` varbinary(1024) DEFAULT NULL,
						  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
						  `reply_info` json DEFAULT NULL,
						  `related_message_ids` varbinary(1024) DEFAULT NULL,
						  PRIMARY KEY (`id`),
						  KEY `owner_id` (`owner_id`),
						  KEY `sender_id` (`sender_id`),
						  KEY `ap_id` (`ap_id`),
						  KEY `deleted_at` (`deleted_at`),
						  KEY `read_receipts` (`read_receipts`),
						  FULLTEXT KEY `text` (`text`,`subject`),
						  CONSTRAINT `mail_messages_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("""
						CREATE TABLE `mail_messages_peers` (
						  `owner_id` int unsigned NOT NULL,
						  `peer_id` int unsigned NOT NULL,
						  `message_id` bigint unsigned NOT NULL,
						  KEY `owner_id` (`owner_id`),
						  KEY `message_id` (`message_id`),
						  KEY `peer_id` (`peer_id`),
						  CONSTRAINT `mail_messages_peers_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
						  CONSTRAINT `mail_messages_peers_ibfk_2` FOREIGN KEY (`message_id`) REFERENCES `mail_messages` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("""
						CREATE TABLE `mail_privacy_grants` (
						  `owner_id` int unsigned NOT NULL,
						  `user_id` int unsigned NOT NULL,
						  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `messages_remain` int unsigned NOT NULL,
						  PRIMARY KEY (`owner_id`,`user_id`),
						  KEY `user_id` (`user_id`),
						  CONSTRAINT `mail_privacy_grants_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
						  CONSTRAINT `mail_privacy_grants_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
			}
			case 31 -> conn.createStatement().execute("ALTER TABLE `wall_posts` ADD `privacy` tinyint unsigned NOT NULL DEFAULT '0'");
			case 32 -> conn.createStatement().execute("ALTER TABLE reports CHANGE content_id content_id BIGINT UNSIGNED");
			case 33 ->{
				// Allow deleting users while keeping their IDs in these tables
				conn.createStatement().execute("ALTER TABLE reports DROP FOREIGN KEY reports_ibfk_1, DROP FOREIGN KEY reports_ibfk_2");
				conn.createStatement().execute("ALTER TABLE wall_posts DROP FOREIGN KEY wall_posts_ibfk_3");
			}
			case 34 ->{
				conn.createStatement().execute("""
						CREATE TABLE `user_roles` (
						  `id` int unsigned NOT NULL AUTO_INCREMENT,
						  `name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL,
						  `permissions` varbinary(255) NOT NULL,
						  PRIMARY KEY (`id`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				insertDefaultRoles(conn);
				conn.createStatement().execute("""
						ALTER TABLE accounts ADD `role` int unsigned DEFAULT NULL,
						ADD CONSTRAINT `accounts_ibfk_2` FOREIGN KEY (`role`) REFERENCES `user_roles` (`id`) ON DELETE SET NULL,
						ADD `promoted_by` int unsigned DEFAULT NULL,
						ADD CONSTRAINT `accounts_ibfk_3` FOREIGN KEY (`promoted_by`) REFERENCES `accounts` (`id`) ON DELETE SET NULL""");
				new SQLQueryBuilder(conn)
						.update("accounts")
						.where("access_level=2") // moderator -> new moderator role
						.value("role", 3)
						.executeNoResult();
				new SQLQueryBuilder(conn)
						.update("accounts")
						.where("access_level=3") // admin -> new admin role
						.value("role", 2)
						.executeNoResult();
				new SQLQueryBuilder(conn)
						.update("accounts")
						.where("access_level=3 AND id=1") // first admin -> new owner role
						.value("role", 1)
						.executeNoResult();
				conn.createStatement().execute("ALTER TABLE accounts DROP access_level");
			}
			case 35 -> {
				conn.createStatement().execute("""
						CREATE TABLE `audit_log` (
						  `id` int unsigned NOT NULL AUTO_INCREMENT,
						  `admin_id` int unsigned NOT NULL,
						  `action` int unsigned NOT NULL,
						  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `owner_id` int DEFAULT NULL,
						  `object_id` bigint DEFAULT NULL,
						  `object_type` int unsigned DEFAULT NULL,
						  `extra` json DEFAULT NULL,
						  PRIMARY KEY (`id`),
						  KEY `owner_id` (`owner_id`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
			}
			case 36 -> {
				conn.createStatement().execute("ALTER TABLE sessions DROP last_ip, ADD `ip` binary(16) NOT NULL, ADD `user_agent` bigint NOT NULL");
				conn.createStatement().execute("""
						CREATE TABLE `user_agents` (
						  `hash` bigint NOT NULL,
						  `user_agent` text COLLATE utf8mb4_general_ci NOT NULL,
						  PRIMARY KEY (`hash`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
			}
			case 37 -> {
				conn.createStatement().execute("ALTER TABLE accounts ADD email_domain varchar(150) NOT NULL DEFAULT '', ADD INDEX (email_domain), DROP ban_info, ADD last_ip binary(16) DEFAULT NULL, ADD INDEX (last_ip)");
				conn.createStatement().execute("UPDATE accounts SET email_domain=SUBSTR(email, LOCATE('@', email)+1)");
				conn.createStatement().execute("ALTER TABLE `users` ADD ban_status tinyint unsigned NOT NULL DEFAULT 0, ADD INDEX (ban_status), ADD ban_info json DEFAULT NULL");
			}
			case 38 -> {
				conn.createStatement().execute("""
						CREATE TABLE IF NOT EXISTS `media_files` (
						  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
						  `random_id` binary(18) NOT NULL,
						  `size` bigint unsigned NOT NULL,
						  `type` tinyint unsigned NOT NULL,
						  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `metadata` json NOT NULL,
						  `ref_count` int unsigned NOT NULL DEFAULT '0',
						  `original_owner_id` int NOT NULL,
						  PRIMARY KEY (`id`),
						  KEY `ref_count` (`ref_count`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("""
						CREATE TABLE IF NOT EXISTS `media_file_refs` (
						  `file_id` bigint unsigned NOT NULL,
						  `object_id` bigint NOT NULL,
						  `object_type` tinyint unsigned NOT NULL,
						  `owner_user_id` int unsigned DEFAULT NULL,
						  `owner_group_id` int unsigned DEFAULT NULL,
						  PRIMARY KEY (`object_id`,`object_type`,`file_id`),
						  KEY `file_id` (`file_id`),
						  KEY `owner_user_id` (`owner_user_id`),
						  KEY `owner_group_id` (`owner_group_id`),
						  CONSTRAINT `media_file_refs_ibfk_1` FOREIGN KEY (`file_id`) REFERENCES `media_files` (`id`),
						  CONSTRAINT `media_file_refs_ibfk_2` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
						  CONSTRAINT `media_file_refs_ibfk_3` FOREIGN KEY (`owner_group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("DROP TABLE IF EXISTS `draft_attachments`");
				createMediaRefCountTriggers(conn);
			}
			case 39 -> migrateMediaFiles(conn);
			case 40 -> {
				conn.createStatement().execute("ALTER TABLE `reports` ADD `content` json DEFAULT NULL, ADD `state` tinyint unsigned NOT NULL DEFAULT 0, ADD KEY `state` (`state`), CHANGE `target_id` `target_id` int NOT NULL, ADD KEY `target_id` (`target_id`)");
				conn.createStatement().execute("""
						CREATE TABLE `report_actions` (
						  `id` int unsigned NOT NULL AUTO_INCREMENT,
						  `report_id` int unsigned NOT NULL,
						  `user_id` int unsigned NOT NULL,
						  `action_type` tinyint unsigned NOT NULL,
						  `text` text COLLATE utf8mb4_general_ci,
						  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `extra` json DEFAULT NULL,
						  PRIMARY KEY (`id`),
						  KEY `report_id` (`report_id`),
						  CONSTRAINT `report_actions_ibfk_1` FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;""");
			}
			case 41 -> {
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
			case 42 -> {
				conn.createStatement().execute("""
						CREATE TABLE `blocks_email_domain` (
						  `domain` varchar(100) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
						  `action` tinyint unsigned NOT NULL,
						  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `note` text COLLATE utf8mb4_general_ci NOT NULL,
						  `creator_id` int unsigned NOT NULL,
						  PRIMARY KEY (`domain`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;""");
				conn.createStatement().execute("""
						CREATE TABLE `blocks_ip` (
						  `id` int unsigned NOT NULL AUTO_INCREMENT,
						  `address` binary(16) NOT NULL,
						  `prefix_length` tinyint unsigned NOT NULL,
						  `action` tinyint unsigned NOT NULL,
						  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `expires_at` timestamp NOT NULL,
						  `note` text COLLATE utf8mb4_general_ci NOT NULL,
						  `creator_id` int unsigned NOT NULL,
						  PRIMARY KEY (`id`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;""");
			}
			case 43 -> {
				conn.createStatement().execute("DROP FUNCTION `bin_prefix`");
				conn.createStatement().execute("""
						CREATE FUNCTION `bin_prefix`(p VARBINARY(1024)) RETURNS varbinary(2048) DETERMINISTIC
						RETURN CONCAT(REPLACE(REPLACE(REPLACE(p, '\\\\', '\\\\\\\\'), '%', '\\\\%'), '_', '\\\\_'), '%');""");
			}
			case 44 -> {
				conn.createStatement().execute("ALTER TABLE `wall_posts` ADD `flags` bigint unsigned NOT NULL DEFAULT 0, DROP FOREIGN KEY wall_posts_ibfk_2");
			}
			case 45 -> {
				conn.createStatement().execute("ALTER TABLE `users` CHANGE `username` `username` varchar(64) NOT NULL");
				conn.createStatement().execute("ALTER TABLE `groups` CHANGE `username` `username` varchar(64) NOT NULL");
			}
			case 46 -> conn.createStatement().execute("ALTER TABLE `config` CHANGE `value` `value` mediumtext NOT NULL");
			case 47 -> {
				conn.createStatement().execute("""
						CREATE TABLE `bookmarks_group` (
						  `id` int unsigned NOT NULL AUTO_INCREMENT,
						  `owner_id` int unsigned NOT NULL,
						  `group_id` int unsigned NOT NULL,
						  PRIMARY KEY (`id`),
						  UNIQUE KEY `owner_id_2` (`owner_id`,`group_id`),
						  KEY `group_id` (`group_id`),
						  CONSTRAINT `bookmarks_group_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;""");
				conn.createStatement().execute("""
						CREATE TABLE `bookmarks_user` (
						  `id` int unsigned NOT NULL AUTO_INCREMENT,
						  `owner_id` int unsigned NOT NULL,
						  `user_id` int unsigned NOT NULL,
						  PRIMARY KEY (`id`),
						  UNIQUE KEY `owner_id` (`owner_id`,`user_id`),
						  KEY `user_id` (`user_id`),
						  CONSTRAINT `bookmarks_user_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;""");
			}
			case 48 -> {
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
			case 49 -> createApIdIndexTriggers(conn);
			case 50 -> {
				conn.createStatement().execute("""
						CREATE TABLE `photo_albums` (
						  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
						  `owner_user_id` int unsigned DEFAULT NULL,
						  `owner_group_id` int unsigned DEFAULT NULL,
						  `title` varchar(200) NOT NULL,
						  `description` text NOT NULL,
						  `privacy` json NOT NULL,
						  `num_photos` int unsigned NOT NULL DEFAULT '0',
						  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `system_type` tinyint unsigned DEFAULT NULL,
						  `cover_id` bigint unsigned DEFAULT NULL,
						  `flags` bigint NOT NULL DEFAULT '0',
						  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
						  `ap_url` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
						  `display_order` int unsigned NOT NULL DEFAULT '0',
						  PRIMARY KEY (`id`),
						  UNIQUE KEY `ap_id` (`ap_id`),
						  KEY `owner_user_id` (`owner_user_id`),
						  KEY `owner_group_id` (`owner_group_id`),
						  KEY `display_order` (`display_order`),
						  CONSTRAINT `photo_albums_ibfk_1` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
						  CONSTRAINT `photo_albums_ibfk_2` FOREIGN KEY (`owner_group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("""
						CREATE TABLE `photos` (
						  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
						  `owner_id` int NOT NULL,
						  `author_id` int unsigned NOT NULL,
						  `album_id` bigint unsigned NOT NULL,
						  `local_file_id` bigint unsigned DEFAULT NULL,
						  `remote_src` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
						  `description` text NOT NULL,
						  `description_source` text,
						  `description_source_format` tinyint unsigned DEFAULT NULL,
						  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `metadata` json DEFAULT NULL,
						  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
						  `display_order` int unsigned NOT NULL,
						  PRIMARY KEY (`id`),
						  UNIQUE KEY `ap_id` (`ap_id`),
						  KEY `owner_id` (`owner_id`),
						  KEY `album_id` (`album_id`),
						  KEY `display_order` (`display_order`),
						  KEY `local_file_id` (`local_file_id`),
						  KEY `author_id` (`author_id`),
						  CONSTRAINT `photos_ibfk_1` FOREIGN KEY (`album_id`) REFERENCES `photo_albums` (`id`) ON DELETE CASCADE,
						  CONSTRAINT `photos_ibfk_2` FOREIGN KEY (`local_file_id`) REFERENCES `media_files` (`id`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
			}
			case 51 -> {
				conn.createStatement().execute("ALTER TABLE newsfeed CHANGE object_id `object_id` bigint unsigned DEFAULT NULL");
				conn.createStatement().execute("ALTER TABLE newsfeed_comments CHANGE object_id `object_id` bigint unsigned NOT NULL");
				conn.createStatement().execute("ALTER TABLE notifications CHANGE object_id `object_id` bigint unsigned DEFAULT NULL, CHANGE related_object_id `related_object_id` bigint unsigned DEFAULT NULL");
				conn.createStatement().execute("ALTER TABLE likes CHANGE object_id `object_id` bigint unsigned NOT NULL");
			}
			case 52 -> conn.createStatement().execute("ALTER TABLE servers ADD features bigint unsigned NOT NULL DEFAULT 0");
			case 53 -> migratePasswordsToSaltedHashes(conn);
			case 54 -> createApIdIndexTriggersForPhotos(conn);
			case 55 -> {
				conn.createStatement().execute("""
						CREATE TABLE `comments` (
						  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
						  `author_id` int unsigned DEFAULT NULL,
						  `owner_user_id` int unsigned DEFAULT NULL,
						  `owner_group_id` int unsigned DEFAULT NULL,
						  `parent_object_type` int unsigned NOT NULL,
						  `parent_object_id` bigint unsigned NOT NULL,
						  `text` text,
						  `attachments` json DEFAULT NULL,
						  `ap_url` varchar(300) DEFAULT NULL,
						  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
						  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `content_warning` text,
						  `updated_at` timestamp NULL DEFAULT NULL,
						  `reply_key` varbinary(2048) DEFAULT NULL,
						  `mentions` varbinary(1024) DEFAULT NULL,
						  `reply_count` int unsigned NOT NULL DEFAULT '0',
						  `ap_replies` varchar(300) DEFAULT NULL,
						  `federation_state` tinyint unsigned NOT NULL DEFAULT '0',
						  `source` text,
						  `source_format` tinyint unsigned DEFAULT NULL,
						  PRIMARY KEY (`id`),
						  UNIQUE KEY `ap_id` (`ap_id`),
						  KEY `owner_user_id` (`owner_user_id`),
						  KEY `author_id` (`author_id`),
						  KEY `reply_key` (`reply_key`),
						  KEY `owner_group_id` (`owner_group_id`),
						  KEY `parent_object_type` (`parent_object_type`,`parent_object_id`),
						  CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
						  CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`owner_group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				createApIdIndexTriggersForComments(conn);
			}
			case 56 -> conn.createStatement().execute("ALTER TABLE photo_albums ADD `ap_comments` varchar(300) CHARACTER SET ascii DEFAULT NULL");
			case 57 -> conn.createStatement().execute("ALTER TABLE wall_posts ADD action tinyint unsigned DEFAULT NULL");
			case 58 -> {
				conn.createStatement().execute("""
						CREATE TABLE `photo_tags` (
						  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
						  `photo_id` bigint unsigned NOT NULL,
						  `placer_id` int unsigned NOT NULL,
						  `user_id` int unsigned DEFAULT NULL,
						  `name` varchar(300) NOT NULL,
						  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `approved` tinyint unsigned NOT NULL DEFAULT '0',
						  `x1` float NOT NULL,
						  `y1` float NOT NULL,
						  `x2` float NOT NULL,
						  `y2` float NOT NULL,
						  PRIMARY KEY (`id`),
						  UNIQUE KEY `photo_id` (`photo_id`,`user_id`),
						  KEY `user_id` (`user_id`),
						  KEY `approved` (`approved`),
						  CONSTRAINT `photo_tags_ibfk_1` FOREIGN KEY (`photo_id`) REFERENCES `photos` (`id`) ON DELETE CASCADE,
						  CONSTRAINT `photo_tags_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
						) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4;""");
			}
			case 59 -> conn.createStatement().execute("ALTER TABLE photo_tags ADD ap_id varchar(300) CHARACTER SET ascii DEFAULT NULL, ADD UNIQUE KEY ap_id (ap_id)");
			case 60 -> {
				conn.createStatement().execute("""
						CREATE TABLE `newsfeed_groups` (
						  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
						  `type` int unsigned NOT NULL,
						  `object_id` bigint unsigned DEFAULT NULL,
						  `group_id` int unsigned NOT NULL,
						  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  PRIMARY KEY (`id`),
						  UNIQUE KEY `type` (`type`,`group_id`,`object_id`),
						  KEY `time` (`time`),
						  KEY `group_id` (`group_id`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
			}
			case 61 -> conn.createStatement().execute("ALTER TABLE followings ADD `muted` tinyint(1) NOT NULL DEFAULT '0', ADD `hints_rank` int unsigned NOT NULL DEFAULT '0'," +
					" ADD `lists` bit(64) NOT NULL DEFAULT b'0', ADD KEY `muted` (`muted`), ADD KEY `hints_rank` (`hints_rank`)");
			case 62 -> {
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
			case 63 -> {
				conn.createStatement().execute("ALTER TABLE users ADD `presence` json DEFAULT NULL, ADD `is_online` BOOL AS (IFNULL(CAST(presence->'$.isOnline' AS UNSIGNED), 0)) NOT NULL, " +
						"ADD KEY `is_online` (`is_online`), ADD `num_followers` bigint NOT NULL DEFAULT '0', ADD `num_following` bigint NOT NULL DEFAULT '0', ADD `num_friends` bigint NOT NULL DEFAULT '0'");
			}
			case 64 -> conn.createStatement().execute("ALTER TABLE group_memberships ADD `hints_rank` int unsigned NOT NULL DEFAULT '0', ADD KEY `hints_rank` (`hints_rank`)");
			case 65 -> {
				conn.createStatement().execute("""
						CREATE TABLE `user_action_log` (
						  `id` int unsigned NOT NULL AUTO_INCREMENT,
						  `action` int unsigned NOT NULL,
						  `user_id` int unsigned NOT NULL,
						  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  `info` json NOT NULL,
						  PRIMARY KEY (`id`),
						  KEY `user_id` (`user_id`),
						  KEY `action` (`action`),
						  CONSTRAINT `user_action_log_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
			}
			case 66 -> {
				conn.createStatement().execute("""
						CREATE TABLE `fasp_providers` (
						  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
						  `confirmed` tinyint(1) NOT NULL DEFAULT '0',
						  `name` varchar(300) NOT NULL,
						  `base_url` varchar(300) NOT NULL,
						  `sign_in_url` varchar(300) DEFAULT NULL,
						  `remote_id` varchar(64) NOT NULL,
						  `public_key` blob NOT NULL,
						  `private_key` blob NOT NULL,
						  `capabilities` json NOT NULL,
						  `enabled_capabilities` json NOT NULL,
						  `privacy_policy` json DEFAULT NULL,
						  `contact_email` varchar(300) DEFAULT NULL,
						  `actor_id` int DEFAULT NULL,
						  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  PRIMARY KEY (`id`),
						  UNIQUE KEY `base_url` (`base_url`),
						  KEY `confirmed` (`confirmed`)
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
				conn.createStatement().execute("""
						CREATE TABLE `fasp_debug_callbacks` (
						  `id` int unsigned NOT NULL AUTO_INCREMENT,
						  `provider_id` bigint unsigned NOT NULL,
						  `ip` binary(16) NOT NULL,
						  `body` text COLLATE utf8mb4_general_ci NOT NULL,
						  `received_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  PRIMARY KEY (`id`),
						  KEY `provider_id` (`provider_id`),
						  CONSTRAINT `fasp_debug_callbacks_ibfk_1` FOREIGN KEY (`provider_id`) REFERENCES `fasp_providers` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4;""");
			}
			case 67 -> {
				conn.createStatement().execute("UPDATE `users` SET num_followers=(SELECT COUNT(*) FROM followings WHERE followee_id=id)");
				conn.createStatement().execute("UPDATE `users` SET num_following=(SELECT COUNT(*) FROM followings WHERE follower_id=id)");
				conn.createStatement().execute("UPDATE `users` SET num_friends=(SELECT COUNT(*) FROM followings WHERE follower_id=id AND mutual=1)");
			}
			case 68 -> conn.createStatement().execute("ALTER TABLE `users` ADD KEY num_followers (num_followers)");
			case 69 -> conn.createStatement().execute("ALTER TABLE followings ADD `added_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP, ADD KEY `added_at` (`added_at`)");
			case 70 -> {
				conn.createStatement().execute("""
						CREATE TABLE `friend_lists` (
						  `id` tinyint unsigned NOT NULL,
						  `owner_id` int unsigned NOT NULL,
						  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
						  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
						  PRIMARY KEY (`id`,`owner_id`),
						  CONSTRAINT `friend_lists_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
						) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;""");
			}
		}
	}

	private static void migratePasswordsToSaltedHashes(DatabaseConnection conn) throws SQLException{
		LOG.info("Started migrating passwords to salted hashes");
		conn.createStatement().execute("ALTER TABLE `accounts` ADD `salt` binary(32) DEFAULT NULL AFTER `password`");
		try(ResultSet res=new SQLQueryBuilder(conn)
				.selectFrom("accounts")
				.columns("id", "password")
				.execute()){
			while(res.next()){
				int accountID=res.getInt(1);
				byte[] currentPassword=res.getBytes(2);
				byte[] salt=Passwords.randomSalt();
				byte[] newPassword=Passwords.saltedPassword(currentPassword, salt);
				new SQLQueryBuilder()
						.update("accounts")
						.value("password", newPassword)
						.value("salt", salt)
						.where("id=?", accountID)
						.executeUpdate();
			}
		}
	}

	private static void insertDefaultRoles(DatabaseConnection conn) throws SQLException{
		new SQLQueryBuilder(conn)
				.insertInto("user_roles")
				.value("name", "Owner")
				.value("permissions", Utils.serializeEnumSetToBytes(EnumSet.of(UserRole.Permission.SUPERUSER, UserRole.Permission.VISIBLE_IN_STAFF)))
				.executeNoResult();

		EnumSet<UserRole.Permission> adminPermissions=EnumSet.allOf(UserRole.Permission.class);
		adminPermissions.remove(UserRole.Permission.SUPERUSER);
		new SQLQueryBuilder(conn)
				.insertInto("user_roles")
				.value("name", "Admin")
				.value("permissions", Utils.serializeEnumSetToBytes(adminPermissions))
				.executeNoResult();

		EnumSet<UserRole.Permission> moderatorPermissions=EnumSet.of(
				UserRole.Permission.MANAGE_USERS,
				UserRole.Permission.MANAGE_REPORTS,
				UserRole.Permission.VIEW_SERVER_AUDIT_LOG,
				UserRole.Permission.MANAGE_GROUPS
		);
		new SQLQueryBuilder(conn)
				.insertInto("user_roles")
				.value("name", "Moderator")
				.value("permissions", Utils.serializeEnumSetToBytes(moderatorPermissions))
				.executeNoResult();
		Config.reloadRoles();
	}

	private static void createApIdIndexTriggers(DatabaseConnection conn) throws SQLException{
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_users_to_ap_ids AFTER INSERT ON `users` FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.USER.id).execute();
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_groups_to_ap_ids AFTER INSERT ON `groups` FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.GROUP.id).execute();
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_posts_to_ap_ids AFTER INSERT ON wall_posts FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.POST.id).execute();
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_messages_to_ap_ids AFTER INSERT ON mail_messages FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.MESSAGE.id).execute();

		conn.createStatement().execute("CREATE TRIGGER delete_foreign_users_from_ap_ids AFTER DELETE ON `users` FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF; END;");
		conn.createStatement().execute("CREATE TRIGGER delete_foreign_groups_from_ap_ids AFTER DELETE ON `groups` FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF; END;");
		conn.createStatement().execute("CREATE TRIGGER delete_foreign_posts_from_ap_ids AFTER DELETE ON wall_posts FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF; END;");

		conn.createStatement().execute("CREATE TRIGGER delete_foreign_user_posts_from_ap_ids BEFORE DELETE ON `users` FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id IN (SELECT ap_id FROM wall_posts WHERE owner_user_id=OLD.id AND ap_id IS NOT NULL); END IF; END;");
		conn.createStatement().execute("CREATE TRIGGER delete_foreign_group_posts_from_ap_ids BEFORE DELETE ON `groups` FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id IN (SELECT ap_id FROM wall_posts WHERE owner_group_id=OLD.id AND ap_id IS NOT NULL); END IF; END;");
	}

	private static void createApIdIndexTriggersForPhotos(DatabaseConnection conn) throws SQLException{
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_photo_albums_to_ap_ids AFTER INSERT ON photo_albums FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.PHOTO_ALBUM.id).execute();
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_photos_to_ap_ids AFTER INSERT ON photos FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.PHOTO.id).execute();

		conn.createStatement().execute("CREATE TRIGGER delete_foreign_photo_albums_from_ap_ids BEFORE DELETE ON photo_albums FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF;" +
				"DELETE FROM ap_id_index WHERE ap_id IN (SELECT ap_id FROM photos WHERE album_id=OLD.id AND ap_id IS NOT NULL);" +
				"END;");
		conn.createStatement().execute("CREATE TRIGGER delete_foreign_photos_from_ap_ids AFTER DELETE ON photos FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF; END;");
	}

	private static void createApIdIndexTriggersForComments(DatabaseConnection conn) throws SQLException{
		SQLQueryBuilder.prepareStatement(conn, "CREATE TRIGGER add_foreign_comments_to_ap_ids AFTER INSERT ON comments FOR EACH ROW BEGIN " +
				"IF NEW.ap_id IS NOT NULL THEN INSERT IGNORE INTO ap_id_index (ap_id, object_type, object_id) VALUES (NEW.ap_id, ?, NEW.id); END IF; END;", ObjectLinkResolver.ObjectType.COMMENT.id).execute();
		conn.createStatement().execute("CREATE TRIGGER delete_foreign_comments_from_ap_ids AFTER DELETE ON comments FOR EACH ROW BEGIN " +
				"IF OLD.ap_id IS NOT NULL THEN DELETE FROM ap_id_index WHERE ap_id=OLD.ap_id; END IF; END;");
	}

	private static void createMediaRefCountTriggers(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("CREATE TRIGGER inc_count_on_insert AFTER INSERT ON media_file_refs FOR EACH ROW UPDATE media_files SET ref_count=ref_count+1 WHERE id=NEW.file_id");
		conn.createStatement().execute("CREATE TRIGGER dec_count_on_delete AFTER DELETE ON media_file_refs FOR EACH ROW UPDATE media_files SET ref_count=ref_count-1 WHERE id=OLD.file_id");
	}

	private static void migrateMediaFiles(DatabaseConnection conn) throws SQLException{
		LOG.info("Started migrating user avatars");
		try(ResultSet res=new SQLQueryBuilder(conn)
				.selectFrom("users")
				.columns("id", "avatar")
				.where("domain='' AND avatar IS NOT NULL")
				.execute()){
			while(res.next()){
				int id=res.getInt(1);
				JsonObject avaObj=JsonParser.parseString(res.getString(2)).getAsJsonObject();
				if(!avaObj.has("_lid"))
					continue;
				long newID=migrateOneAvatar(conn, avaObj, id);
				if(newID==0)
					continue;
				new SQLQueryBuilder(conn)
						.insertInto("media_file_refs")
						.value("file_id", newID)
						.value("object_id", id)
						.value("object_type", MediaFileReferenceType.USER_AVATAR)
						.value("owner_user_id", id)
						.executeNoResult();
				new SQLQueryBuilder(conn)
						.update("users")
						.value("avatar", new JsonObjectBuilder().add("type", "_LocalImage").add("_fileID", newID).build().toString())
						.where("id=?", id)
						.executeNoResult();
			}
		}
		LOG.info("Started migrating group avatars");
		try(ResultSet res=new SQLQueryBuilder(conn)
				.selectFrom("groups")
				.columns("id", "avatar")
				.where("domain='' AND avatar IS NOT NULL")
				.execute()){
			while(res.next()){
				int id=res.getInt(1);
				JsonObject avaObj=JsonParser.parseString(res.getString(2)).getAsJsonObject();
				if(!avaObj.has("_lid"))
					continue;
				long newID=migrateOneAvatar(conn, avaObj, -id);
				if(newID==0)
					continue;
				new SQLQueryBuilder(conn)
						.insertInto("media_file_refs")
						.value("file_id", newID)
						.value("object_id", id)
						.value("object_type", MediaFileReferenceType.GROUP_AVATAR)
						.value("owner_group_id", id)
						.executeNoResult();
				new SQLQueryBuilder(conn)
						.update("groups")
						.value("avatar", new JsonObjectBuilder().add("type", "_LocalImage").add("_fileID", newID).build().toString())
						.where("id=?", id)
						.executeNoResult();
			}
		}
		LOG.info("Started migrating wall attachments");
		try(ResultSet res=new SQLQueryBuilder(conn)
				.selectFrom("wall_posts")
				.columns("id", "owner_user_id", "owner_group_id", "attachments")
				.where("ap_id IS NULL AND attachments IS NOT NULL")
				.execute()){
			while(res.next()){
				int id=res.getInt(1);
				int ownerID=res.getInt(2);
				if(res.wasNull())
					ownerID=-res.getInt(3);
				JsonElement _attachments=JsonParser.parseString(res.getString(4));
				List<JsonObject> attachments;
				if(_attachments instanceof JsonObject jo){
					attachments=List.of(jo);
				}else if(_attachments instanceof JsonArray ja){
					attachments=new ArrayList<>(ja.size());
					for(JsonElement el:ja)
						attachments.add(el.getAsJsonObject());
				}else{
					throw new IllegalStateException();
				}
				if(!attachments.getFirst().has("_p"))
					continue;
				long[] attachmentIDs=migrateMediaAttachments(conn, attachments, ownerID);
				JsonArray newAttachments=new JsonArray();
				for(long attID:attachmentIDs){
					newAttachments.add(new JsonObjectBuilder()
							.add("type", "_LocalImage")
							.add("_fileID", attID)
							.build());
					if(attID==0)
						continue;
					new SQLQueryBuilder(conn)
							.insertInto("media_file_refs")
							.value("file_id", attID)
							.value("object_id", id)
							.value("object_type", MediaFileReferenceType.WALL_ATTACHMENT)
							.value(ownerID>0 ? "owner_user_id" : "owner_group_id", Math.abs(ownerID))
							.executeNoResult();
				}
				new SQLQueryBuilder(conn)
						.update("wall_posts")
						.value("attachments", (newAttachments.size()==1 ? newAttachments.get(0) : newAttachments).toString())
						.where("id=?", id)
						.executeNoResult();
			}
		}
		LOG.info("Started migrating mail attachments");
		try(ResultSet res=new SQLQueryBuilder(conn)
				.selectFrom("mail_messages")
				.columns("id", "owner_id", "attachments")
				.where("ap_id IS NULL AND attachments IS NOT NULL")
				.execute()){
			while(res.next()){
				long id=res.getInt(1);
				int ownerID=res.getInt(2);
				JsonElement _attachments=JsonParser.parseString(res.getString(3));
				List<JsonObject> attachments;
				if(_attachments instanceof JsonObject jo){
					attachments=List.of(jo);
				}else if(_attachments instanceof JsonArray ja){
					attachments=new ArrayList<>(ja.size());
					for(JsonElement el:ja)
						attachments.add(el.getAsJsonObject());
				}else{
					throw new IllegalStateException();
				}
				if(!attachments.getFirst().has("_p"))
					continue;
				long[] attachmentIDs=migrateMediaAttachments(conn, attachments, ownerID);
				JsonArray newAttachments=new JsonArray();
				for(long attID:attachmentIDs){
					newAttachments.add(new JsonObjectBuilder()
							.add("type", "_LocalImage")
							.add("_fileID", attID)
							.build());
					if(attID==0)
						continue;
					new SQLQueryBuilder(conn)
							.insertInto("media_file_refs")
							.value("file_id", attID)
							.value("object_id", id)
							.value("object_type", MediaFileReferenceType.MAIL_ATTACHMENT)
							.value("owner_user_id", ownerID)
							.executeNoResult();
				}
				new SQLQueryBuilder(conn)
						.update("mail_messages")
						.value("attachments", (newAttachments.size()==1 ? newAttachments.get(0) : newAttachments).toString())
						.where("id=?", id)
						.executeNoResult();
			}
		}
		LOG.info("Media file migration done");
	}

	private static long migrateOneAvatar(DatabaseConnection conn, JsonObject avaObj, int id) throws SQLException{
		String fileID=avaObj.get("_lid").getAsString();
		String dirName=avaObj.get("_p").getAsString();

		File actualFile=new File(Config.uploadPath, dirName+"/"+fileID+".webp");
		if(!actualFile.exists()){
			LOG.debug("Skipping file {} because it does not exist on disk", actualFile.getAbsolutePath());
			return 0;
		}

		int width=avaObj.getAsJsonArray("_sz").get(0).getAsInt();
		int height=avaObj.getAsJsonArray("_sz").get(1).getAsInt();
		JsonArray _cropRegion=avaObj.getAsJsonArray("cropRegion");
		float[] cropRegion=new float[4];
		for(int i=0;i<4;i++)
			cropRegion[i]=_cropRegion.get(i).getAsFloat();
		ImageMetadata meta=new ImageMetadata(width, height, null, cropRegion);
		byte[] randomID=Utils.randomBytes(18);
		long newID=new SQLQueryBuilder(conn)
				.insertInto("media_files")
				.value("random_id", randomID)
				.value("size", actualFile.length())
				.value("type", MediaFileType.IMAGE_AVATAR)
				.value("metadata", Utils.gson.toJson(meta))
				.value("original_owner_id", id)
				.executeAndGetIDLong();
		int oid=Math.abs(id);
		File newFileDir=new File(Config.uploadPath, String.format(Locale.US, "%02d/%02d/%02d", oid%100, oid/100%100, oid/100_00%100));
		if(!newFileDir.exists() && !newFileDir.mkdirs())
			throw new RuntimeException("mkdirs failed");
		File newFile=new File(newFileDir, Base64.getUrlEncoder().withoutPadding().encodeToString(randomID)+"_"
				+Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.packLong(XTEA.obfuscateObjectID(newID, ObfuscatedObjectIDType.MEDIA_FILE)))+".webp");
		try{
			LOG.debug("Copying: {} -> {}", actualFile.getAbsolutePath(), newFile.getAbsolutePath());
			Files.copy(actualFile.toPath(), newFile.toPath());
		}catch(IOException x){
			throw new RuntimeException("failed to copy file", x);
		}
		return newID;
	}

	private static long[] migrateMediaAttachments(DatabaseConnection conn, List<JsonObject> attachments, int ownerID) throws SQLException{
		long[] ids=new long[attachments.size()];
		int i=0;
		for(JsonObject obj:attachments){
			String fileID=obj.get("_lid").getAsString();
			String dirName=obj.get("_p").getAsString();

			File actualFile=new File(Config.uploadPath, dirName+"/"+fileID+".webp");
			if(!actualFile.exists()){
				LOG.debug("Skipping file {} because it does not exist on disk", actualFile.getAbsolutePath());
				i++;
				continue;
			}
			int width=obj.getAsJsonArray("_sz").get(0).getAsInt();
			int height=obj.getAsJsonArray("_sz").get(1).getAsInt();
			String blurhash=obj.has("blurhash") ? obj.get("blurhash").getAsString() : null;
			ImageMetadata meta=new ImageMetadata(width, height, blurhash, null);
			byte[] randomID=Utils.randomBytes(18);
			boolean isGraffiti=obj.has("graffiti") && obj.get("graffiti").getAsBoolean();
			long newID=new SQLQueryBuilder(conn)
					.insertInto("media_files")
					.value("random_id", randomID)
					.value("size", actualFile.length())
					.value("type", isGraffiti ? MediaFileType.IMAGE_GRAFFITI : MediaFileType.IMAGE_PHOTO)
					.value("metadata", Utils.gson.toJson(meta))
					.value("original_owner_id", ownerID)
					.executeAndGetIDLong();
			int oid=Math.abs(ownerID);
			File newFileDir=new File(Config.uploadPath, String.format(Locale.US, "%02d/%02d/%02d", oid%100, oid/100%100, oid/100_00%100));
			if(!newFileDir.exists() && !newFileDir.mkdirs())
				throw new RuntimeException("mkdirs failed");
			File newFile=new File(newFileDir, Base64.getUrlEncoder().withoutPadding().encodeToString(randomID)+"_"
					+Base64.getUrlEncoder().withoutPadding().encodeToString(Utils.packLong(XTEA.obfuscateObjectID(newID, ObfuscatedObjectIDType.MEDIA_FILE)))+".webp");
			try{
				LOG.debug("Copying: {} -> {}", actualFile.getAbsolutePath(), newFile.getAbsolutePath());
				Files.copy(actualFile.toPath(), newFile.toPath());
			}catch(IOException x){
				throw new RuntimeException("failed to copy file", x);
			}
			ids[i]=newID;
			i++;
		}

		return ids;
	}
}
