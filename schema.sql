# ************************************************************
# Sequel Pro SQL dump
# Version 5446
#
# https://www.sequelpro.com/
# https://github.com/sequelpro/sequelpro
#
# Host: localhost (MySQL 5.7.9)
# Database: smithereen
# Generation Time: 2021-09-28 08:49:15 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
SET NAMES utf8mb4;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table accounts
# ------------------------------------------------------------

CREATE TABLE `accounts` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int(11) unsigned NOT NULL,
  `email` varchar(200) NOT NULL DEFAULT '',
  `password` binary(32) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `invited_by` int(11) unsigned DEFAULT NULL,
  `access_level` tinyint(3) unsigned NOT NULL DEFAULT '1',
  `preferences` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
  `last_active` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ban_info` text,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `accounts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table blocks_group_domain
# ------------------------------------------------------------

CREATE TABLE `blocks_group_domain` (
  `owner_id` int(10) unsigned NOT NULL,
  `domain` varchar(100) CHARACTER SET ascii NOT NULL,
  UNIQUE KEY `owner_id` (`owner_id`,`domain`),
  KEY `domain` (`domain`),
  CONSTRAINT `blocks_group_domain_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table blocks_group_user
# ------------------------------------------------------------

CREATE TABLE `blocks_group_user` (
  `owner_id` int(10) unsigned NOT NULL,
  `user_id` int(10) unsigned NOT NULL,
  UNIQUE KEY `owner_id` (`owner_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `blocks_group_user_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blocks_group_user_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table blocks_user_domain
# ------------------------------------------------------------

CREATE TABLE `blocks_user_domain` (
  `owner_id` int(10) unsigned NOT NULL,
  `domain` varchar(100) CHARACTER SET ascii NOT NULL,
  UNIQUE KEY `owner_id` (`owner_id`,`domain`),
  KEY `domain` (`domain`),
  CONSTRAINT `blocks_user_domain_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table blocks_user_user
# ------------------------------------------------------------

CREATE TABLE `blocks_user_user` (
  `owner_id` int(10) unsigned NOT NULL,
  `user_id` int(10) unsigned NOT NULL,
  UNIQUE KEY `owner_id` (`owner_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `blocks_user_user_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blocks_user_user_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table config
# ------------------------------------------------------------

CREATE TABLE `config` (
  `key` varchar(255) CHARACTER SET ascii NOT NULL DEFAULT '',
  `value` text NOT NULL,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table draft_attachments
# ------------------------------------------------------------

CREATE TABLE `draft_attachments` (
  `id` binary(16) NOT NULL,
  `owner_account_id` int(10) unsigned NOT NULL,
  `info` text NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `owner_account_id` (`owner_account_id`),
  CONSTRAINT `draft_attachments_ibfk_1` FOREIGN KEY (`owner_account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table email_codes
# ------------------------------------------------------------

CREATE TABLE `email_codes` (
  `code` binary(64) NOT NULL,
  `account_id` int(10) unsigned DEFAULT NULL,
  `type` int(11) NOT NULL,
  `extra` text,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`code`),
  KEY `account_id` (`account_id`),
  CONSTRAINT `email_codes_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table followings
# ------------------------------------------------------------

CREATE TABLE `followings` (
  `follower_id` int(11) unsigned NOT NULL,
  `followee_id` int(11) unsigned NOT NULL,
  `mutual` tinyint(1) NOT NULL DEFAULT '0',
  `accepted` tinyint(1) NOT NULL DEFAULT '1',
  KEY `follower_id` (`follower_id`),
  KEY `followee_id` (`followee_id`),
  KEY `mutual` (`mutual`),
  KEY `accepted` (`accepted`),
  CONSTRAINT `followings_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `followings_ibfk_2` FOREIGN KEY (`followee_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table friend_requests
# ------------------------------------------------------------

CREATE TABLE `friend_requests` (
  `from_user_id` int(11) unsigned NOT NULL,
  `to_user_id` int(11) unsigned NOT NULL,
  `message` text,
  UNIQUE KEY `from_user_id` (`from_user_id`,`to_user_id`),
  KEY `to_user_id` (`to_user_id`),
  CONSTRAINT `friend_requests_ibfk_1` FOREIGN KEY (`from_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `friend_requests_ibfk_2` FOREIGN KEY (`to_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table group_admins
# ------------------------------------------------------------

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table group_memberships
# ------------------------------------------------------------

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table groups
# ------------------------------------------------------------

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table likes
# ------------------------------------------------------------

CREATE TABLE `likes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) unsigned NOT NULL,
  `object_id` int(11) unsigned NOT NULL,
  `object_type` int(11) unsigned NOT NULL,
  `ap_id` varchar(300) DEFAULT NULL,
  UNIQUE KEY `user_id` (`user_id`,`object_id`,`object_type`),
  UNIQUE KEY `id` (`id`),
  KEY `object_type` (`object_type`,`object_id`),
  CONSTRAINT `likes_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table media_cache
# ------------------------------------------------------------

CREATE TABLE `media_cache` (
  `url_hash` binary(16) NOT NULL,
  `size` int(11) unsigned NOT NULL,
  `last_access` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `info` blob,
  `type` tinyint(4) NOT NULL,
  PRIMARY KEY (`url_hash`),
  KEY `last_access` (`last_access`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table newsfeed
# ------------------------------------------------------------

CREATE TABLE `newsfeed` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `type` int(11) NOT NULL,
  `author_id` int(11) NOT NULL,
  `object_id` int(11) DEFAULT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `type` (`type`,`object_id`,`author_id`),
  KEY `time` (`time`),
  KEY `author_id` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table newsfeed_comments
# ------------------------------------------------------------

CREATE TABLE `newsfeed_comments` (
  `user_id` int(10) unsigned NOT NULL,
  `object_type` int(10) unsigned NOT NULL,
  `object_id` int(10) unsigned NOT NULL,
  `last_comment_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`object_type`,`object_id`,`user_id`),
  KEY `user_id` (`user_id`),
  KEY `last_comment_time` (`last_comment_time`),
  CONSTRAINT `newsfeed_comments_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table notifications
# ------------------------------------------------------------

CREATE TABLE `notifications` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `owner_id` int(11) unsigned NOT NULL,
  `type` smallint(5) unsigned NOT NULL,
  `object_id` int(11) unsigned DEFAULT NULL,
  `object_type` smallint(5) unsigned DEFAULT NULL,
  `related_object_id` int(11) unsigned DEFAULT NULL,
  `related_object_type` smallint(5) unsigned DEFAULT NULL,
  `actor_id` int(11) DEFAULT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `owner_id` (`owner_id`),
  KEY `time` (`time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table poll_options
# ------------------------------------------------------------

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table poll_votes
# ------------------------------------------------------------

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table polls
# ------------------------------------------------------------

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table qsearch_index
# ------------------------------------------------------------

CREATE TABLE `qsearch_index` (
  `string` text NOT NULL,
  `user_id` int(10) unsigned DEFAULT NULL,
  `group_id` int(10) unsigned DEFAULT NULL,
  KEY `user_id` (`user_id`),
  KEY `group_id` (`group_id`),
  FULLTEXT KEY `string` (`string`),
  CONSTRAINT `qsearch_index_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `qsearch_index_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=ascii;



# Dump of table servers
# ------------------------------------------------------------

CREATE TABLE `servers` (
  `host` varchar(100) NOT NULL DEFAULT '',
  `software` varchar(100) DEFAULT NULL,
  `version` varchar(30) DEFAULT NULL,
  `capabilities` bigint(20) unsigned NOT NULL DEFAULT '0',
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`host`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table sessions
# ------------------------------------------------------------

CREATE TABLE `sessions` (
  `id` binary(64) NOT NULL,
  `account_id` int(11) unsigned NOT NULL,
  `last_active` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_ip` varbinary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `account_id` (`account_id`),
  CONSTRAINT `sessions_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table signup_invitations
# ------------------------------------------------------------

CREATE TABLE `signup_invitations` (
  `code` binary(16) NOT NULL DEFAULT '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0',
  `owner_id` int(11) unsigned DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `signups_remaining` int(11) unsigned NOT NULL,
  PRIMARY KEY (`code`),
  KEY `owner_id` (`owner_id`),
  CONSTRAINT `signup_invitations_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table users
# ------------------------------------------------------------

CREATE TABLE `users` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `fname` varchar(100) NOT NULL DEFAULT '',
  `lname` varchar(100) DEFAULT NULL,
  `middle_name` varchar(100) DEFAULT NULL,
  `maiden_name` varchar(100) DEFAULT NULL,
  `bdate` date DEFAULT NULL,
  `username` varchar(50) NOT NULL DEFAULT '',
  `domain` varchar(100) NOT NULL DEFAULT '',
  `public_key` blob NOT NULL,
  `private_key` blob,
  `ap_url` varchar(300) DEFAULT NULL,
  `ap_inbox` varchar(300) DEFAULT NULL,
  `ap_outbox` varchar(300) DEFAULT NULL,
  `ap_shared_inbox` varchar(300) DEFAULT NULL,
  `about` text,
  `gender` tinyint(4) unsigned NOT NULL DEFAULT '0',
  `profile_fields` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
  `avatar` text,
  `ap_id` varchar(300) CHARACTER SET ascii DEFAULT NULL,
  `ap_followers` varchar(300) DEFAULT NULL,
  `ap_following` varchar(300) DEFAULT NULL,
  `last_updated` timestamp NULL DEFAULT NULL,
  `flags` bigint(20) unsigned NOT NULL,
  `ap_wall` varchar(300) DEFAULT NULL,
  `ap_friends` varchar(300) DEFAULT NULL,
  `ap_groups` varchar(300) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`,`domain`),
  UNIQUE KEY `ap_id` (`ap_id`),
  KEY `ap_outbox` (`ap_outbox`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;



# Dump of table wall_posts
# ------------------------------------------------------------

CREATE TABLE `wall_posts` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `author_id` int(11) unsigned DEFAULT NULL,
  `owner_user_id` int(11) unsigned DEFAULT NULL,
  `owner_group_id` int(11) unsigned DEFAULT NULL,
  `text` text,
  `attachments` text CHARACTER SET utf8mb4 COLLATE utf8mb4_bin,
  `repost_of` int(11) unsigned DEFAULT NULL,
  `ap_url` varchar(300) DEFAULT NULL,
  `ap_id` varchar(300) CHARACTER SET ascii DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `content_warning` text,
  `updated_at` timestamp NULL DEFAULT NULL,
  `reply_key` varbinary(1024) DEFAULT NULL,
  `mentions` varbinary(1024) DEFAULT NULL,
  `reply_count` int(10) unsigned NOT NULL DEFAULT '0',
  `ap_replies` varchar(300) DEFAULT NULL,
  `poll_id` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ap_id` (`ap_id`),
  KEY `owner_user_id` (`owner_user_id`),
  KEY `repost_of` (`repost_of`),
  KEY `author_id` (`author_id`),
  KEY `reply_key` (`reply_key`),
  KEY `owner_group_id` (`owner_group_id`),
  CONSTRAINT `wall_posts_ibfk_1` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `wall_posts_ibfk_2` FOREIGN KEY (`repost_of`) REFERENCES `wall_posts` (`id`) ON DELETE SET NULL ON UPDATE NO ACTION,
  CONSTRAINT `wall_posts_ibfk_3` FOREIGN KEY (`author_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `wall_posts_ibfk_4` FOREIGN KEY (`owner_group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;




--
-- Dumping routines (FUNCTION) for database 'smithereen'
--
DELIMITER ;;

# Dump of FUNCTION bin_prefix
# ------------------------------------------------------------

/*!50003 SET SESSION SQL_MODE="ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION"*/;;
/*!50003 CREATE*/ /*!50020 DEFINER=`root`@`localhost`*/ /*!50003 FUNCTION `bin_prefix`(p VARBINARY(1024)) RETURNS varbinary(2048)
    DETERMINISTIC
BEGIN
RETURN CONCAT(REPLACE(REPLACE(REPLACE(p, BINARY(0xFF), BINARY(0xFFFF)), '%', BINARY(0xFF25)), '_', BINARY(0xFF5F)), '%');
END */;;

/*!50003 SET SESSION SQL_MODE=@OLD_SQL_MODE */;;
DELIMITER ;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
