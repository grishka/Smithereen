-- MySQL dump 10.13  Distrib 8.0.32, for macos13.0 (arm64)
--
-- Host: localhost    Database: smithereen
-- ------------------------------------------------------
-- Server version	8.0.32
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;

--
-- Table structure for table `accounts`
--

CREATE TABLE `accounts` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `email` varchar(200) NOT NULL DEFAULT '',
  `password` binary(32) DEFAULT NULL,
  `salt` binary(32) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `invited_by` int unsigned DEFAULT NULL,
  `preferences` text,
  `last_active` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `activation_info` json DEFAULT NULL,
  `role` int unsigned DEFAULT NULL,
  `promoted_by` int unsigned DEFAULT NULL,
  `email_domain` varchar(150) NOT NULL DEFAULT '',
  `last_ip` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `user_id` (`user_id`),
  KEY `invited_by` (`invited_by`),
  KEY `role` (`role`),
  KEY `promoted_by` (`promoted_by`),
  KEY `email_domain` (`email_domain`),
  KEY `last_ip` (`last_ip`),
  CONSTRAINT `accounts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `accounts_ibfk_2` FOREIGN KEY (`role`) REFERENCES `user_roles` (`id`) ON DELETE SET NULL,
  CONSTRAINT `accounts_ibfk_3` FOREIGN KEY (`promoted_by`) REFERENCES `accounts` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `announcements`
--

CREATE TABLE `announcements` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(300) DEFAULT NULL,
  `description` text NOT NULL,
  `link_text` varchar(300) DEFAULT NULL,
  `link_url` varchar(300) DEFAULT NULL,
  `show_from` timestamp NOT NULL,
  `show_to` timestamp NOT NULL,
  `translations` json NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `ap_id_index`
--

CREATE TABLE `ap_id_index` (
  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `object_type` int unsigned NOT NULL,
  `object_id` bigint unsigned NOT NULL,
  PRIMARY KEY (`ap_id`),
  UNIQUE KEY `object_type` (`object_type`,`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `audit_log`
--

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `blocks_email_domain`
--

CREATE TABLE `blocks_email_domain` (
  `domain` varchar(100) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `action` tinyint unsigned NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `note` text NOT NULL,
  `creator_id` int unsigned NOT NULL,
  PRIMARY KEY (`domain`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `blocks_group_domain`
--

CREATE TABLE `blocks_group_domain` (
  `owner_id` int unsigned NOT NULL,
  `domain` varchar(100) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  UNIQUE KEY `owner_id` (`owner_id`,`domain`),
  KEY `domain` (`domain`),
  CONSTRAINT `blocks_group_domain_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `blocks_group_user`
--

CREATE TABLE `blocks_group_user` (
  `owner_id` int unsigned NOT NULL,
  `user_id` int unsigned NOT NULL,
  UNIQUE KEY `owner_id` (`owner_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `blocks_group_user_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blocks_group_user_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `blocks_ip`
--

CREATE TABLE `blocks_ip` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `address` binary(16) NOT NULL,
  `prefix_length` tinyint unsigned NOT NULL,
  `action` tinyint unsigned NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` timestamp NOT NULL,
  `note` text NOT NULL,
  `creator_id` int unsigned NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `blocks_user_domain`
--

CREATE TABLE `blocks_user_domain` (
  `owner_id` int unsigned NOT NULL,
  `domain` varchar(100) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  UNIQUE KEY `owner_id` (`owner_id`,`domain`),
  KEY `domain` (`domain`),
  CONSTRAINT `blocks_user_domain_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `blocks_user_user`
--

CREATE TABLE `blocks_user_user` (
  `owner_id` int unsigned NOT NULL,
  `user_id` int unsigned NOT NULL,
  UNIQUE KEY `owner_id` (`owner_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `blocks_user_user_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `blocks_user_user_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `board_topics`
--

CREATE TABLE `board_topics` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `title` tinytext NOT NULL,
  `author_id` int unsigned NOT NULL,
  `group_id` int unsigned NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `pinned_at` timestamp NULL DEFAULT NULL,
  `num_comments` int unsigned NOT NULL DEFAULT '0',
  `last_comment_author_id` int unsigned NOT NULL,
  `is_closed` tinyint(1) NOT NULL DEFAULT '0',
  `ap_url` varchar(300) DEFAULT NULL,
  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
  `first_comment_id` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ap_id` (`ap_id`),
  KEY `group_id` (`group_id`),
  KEY `updated_at` (`updated_at`),
  KEY `created_at` (`created_at`),
  KEY `pinned_at` (`pinned_at`),
  CONSTRAINT `board_topics_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `bookmarks_group`
--

CREATE TABLE `bookmarks_group` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `owner_id` int unsigned NOT NULL,
  `group_id` int unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `owner_id_2` (`owner_id`,`group_id`),
  KEY `group_id` (`group_id`),
  CONSTRAINT `bookmarks_group_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `bookmarks_user`
--

CREATE TABLE `bookmarks_user` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `owner_id` int unsigned NOT NULL,
  `user_id` int unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `owner_id` (`owner_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `bookmarks_user_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `comments`
--

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `config`
--

CREATE TABLE `config` (
  `key` varchar(255) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL DEFAULT '',
  `value` mediumtext NOT NULL,
  PRIMARY KEY (`key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `deleted_user_bans`
--

CREATE TABLE `deleted_user_bans` (
  `user_id` int unsigned NOT NULL,
  `domain` varchar(100) DEFAULT NULL,
  `ban_status` tinyint unsigned NOT NULL DEFAULT '0',
  `ban_info` json NOT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `draft_attachments`
--

CREATE TABLE `draft_attachments` (
  `id` binary(16) NOT NULL,
  `owner_account_id` int unsigned NOT NULL,
  `info` text NOT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `owner_account_id` (`owner_account_id`),
  CONSTRAINT `draft_attachments_ibfk_1` FOREIGN KEY (`owner_account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `email_codes`
--

CREATE TABLE `email_codes` (
  `code` binary(64) NOT NULL,
  `account_id` int unsigned DEFAULT NULL,
  `type` int NOT NULL,
  `extra` text,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`code`),
  KEY `account_id` (`account_id`),
  CONSTRAINT `email_codes_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `fasp_debug_callbacks`
--

CREATE TABLE `fasp_debug_callbacks` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `provider_id` bigint unsigned NOT NULL,
  `ip` binary(16) NOT NULL,
  `body` text NOT NULL,
  `received_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `provider_id` (`provider_id`),
  CONSTRAINT `fasp_debug_callbacks_ibfk_1` FOREIGN KEY (`provider_id`) REFERENCES `fasp_providers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `fasp_providers`
--

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `followings`
--

CREATE TABLE `followings` (
  `follower_id` int unsigned NOT NULL,
  `followee_id` int unsigned NOT NULL,
  `mutual` tinyint(1) NOT NULL DEFAULT '0',
  `accepted` tinyint(1) NOT NULL DEFAULT '1',
  `muted` tinyint(1) NOT NULL DEFAULT '0',
  `hints_rank` int unsigned NOT NULL DEFAULT '0',
  `lists` bit(64) NOT NULL DEFAULT b'0',
  `added_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `follower_id` (`follower_id`),
  KEY `followee_id` (`followee_id`),
  KEY `mutual` (`mutual`),
  KEY `accepted` (`accepted`),
  KEY `muted` (`muted`),
  KEY `hints_rank` (`hints_rank`),
  KEY `added_at` (`added_at`),
  CONSTRAINT `followings_ibfk_1` FOREIGN KEY (`follower_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `followings_ibfk_2` FOREIGN KEY (`followee_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `friend_lists`
--

CREATE TABLE `friend_lists` (
  `id` tinyint unsigned NOT NULL,
  `owner_id` int unsigned NOT NULL,
  `name` varchar(128) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`,`owner_id`),
  KEY `owner_id` (`owner_id`),
  CONSTRAINT `friend_lists_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `friend_requests`
--

CREATE TABLE `friend_requests` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `from_user_id` int unsigned NOT NULL,
  `to_user_id` int unsigned NOT NULL,
  `message` text,
  PRIMARY KEY (`id`),
  UNIQUE KEY `from_user_id` (`from_user_id`,`to_user_id`),
  KEY `to_user_id` (`to_user_id`),
  CONSTRAINT `friend_requests_ibfk_1` FOREIGN KEY (`from_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `friend_requests_ibfk_2` FOREIGN KEY (`to_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `group_action_log`
--

CREATE TABLE `group_action_log` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `action` int unsigned NOT NULL,
  `group_id` int unsigned NOT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `admin_id` int unsigned DEFAULT NULL,
  `info` json NOT NULL,
  PRIMARY KEY (`id`),
  KEY `group_id` (`group_id`),
  CONSTRAINT `group_action_log_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `group_admins`
--

CREATE TABLE `group_admins` (
  `user_id` int unsigned NOT NULL,
  `group_id` int unsigned NOT NULL,
  `level` int unsigned NOT NULL,
  `title` varchar(300) DEFAULT NULL,
  `display_order` int unsigned NOT NULL DEFAULT '0',
  KEY `user_id` (`user_id`),
  KEY `group_id` (`group_id`),
  CONSTRAINT `group_admins_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `group_admins_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `group_invites`
--

CREATE TABLE `group_invites` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `inviter_id` int unsigned NOT NULL,
  `invitee_id` int unsigned NOT NULL,
  `group_id` int unsigned NOT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_event` tinyint(1) NOT NULL,
  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ap_id` (`ap_id`),
  KEY `inviter_id` (`inviter_id`),
  KEY `invitee_id` (`invitee_id`),
  KEY `group_id` (`group_id`),
  KEY `is_event` (`is_event`),
  CONSTRAINT `group_invites_ibfk_1` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `group_invites_ibfk_2` FOREIGN KEY (`invitee_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `group_invites_ibfk_3` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `group_links`
--

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
  `is_unresolved_ap_object` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ap_id` (`ap_id`),
  KEY `group_id` (`group_id`),
  CONSTRAINT `group_links_ibfk_1` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `group_memberships`
--

CREATE TABLE `group_memberships` (
  `user_id` int unsigned NOT NULL,
  `group_id` int unsigned NOT NULL,
  `post_feed_visibility` tinyint unsigned NOT NULL DEFAULT '0',
  `tentative` tinyint(1) NOT NULL DEFAULT '0',
  `accepted` tinyint(1) NOT NULL DEFAULT '1',
  `time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `hints_rank` int unsigned NOT NULL DEFAULT '0',
  UNIQUE KEY `user_id` (`user_id`,`group_id`),
  KEY `group_id` (`group_id`),
  KEY `hints_rank` (`hints_rank`),
  CONSTRAINT `group_memberships_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `group_memberships_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `group_staff_notes`
--

CREATE TABLE `group_staff_notes` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `target_id` int unsigned NOT NULL,
  `author_id` int unsigned NOT NULL,
  `text` text NOT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `target_id` (`target_id`),
  CONSTRAINT `group_staff_notes_ibfk_1` FOREIGN KEY (`target_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `groups`
--

CREATE TABLE `groups` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL DEFAULT '',
  `username` varchar(64) NOT NULL,
  `domain` varchar(100) NOT NULL DEFAULT '',
  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
  `ap_url` varchar(300) DEFAULT NULL,
  `ap_inbox` varchar(300) DEFAULT NULL,
  `ap_shared_inbox` varchar(300) DEFAULT NULL,
  `public_key` blob NOT NULL,
  `private_key` blob,
  `avatar` json DEFAULT NULL,
  `about` text,
  `about_source` text,
  `profile_fields` json DEFAULT NULL,
  `event_start_time` timestamp NULL DEFAULT NULL,
  `event_end_time` timestamp NULL DEFAULT NULL,
  `type` tinyint unsigned NOT NULL DEFAULT '0',
  `member_count` int unsigned NOT NULL DEFAULT '0',
  `tentative_member_count` int unsigned NOT NULL DEFAULT '0',
  `last_updated` timestamp NULL DEFAULT NULL,
  `flags` bigint unsigned NOT NULL DEFAULT '0',
  `endpoints` json DEFAULT NULL,
  `access_type` tinyint NOT NULL DEFAULT '0',
  `ban_status` tinyint unsigned NOT NULL DEFAULT '0',
  `ban_info` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`,`domain`),
  UNIQUE KEY `ap_id` (`ap_id`),
  KEY `type` (`type`),
  KEY `ban_status` (`ban_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `likes`
--

CREATE TABLE `likes` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `object_id` bigint unsigned NOT NULL,
  `object_type` int unsigned NOT NULL,
  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
  UNIQUE KEY `user_id` (`user_id`,`object_id`,`object_type`),
  UNIQUE KEY `id` (`id`),
  KEY `object_type` (`object_type`,`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `mail_messages`
--

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `mail_messages_peers`
--

CREATE TABLE `mail_messages_peers` (
  `owner_id` int unsigned NOT NULL,
  `peer_id` int unsigned NOT NULL,
  `message_id` bigint unsigned NOT NULL,
  KEY `owner_id` (`owner_id`),
  KEY `message_id` (`message_id`),
  KEY `peer_id` (`peer_id`),
  CONSTRAINT `mail_messages_peers_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `mail_messages_peers_ibfk_2` FOREIGN KEY (`message_id`) REFERENCES `mail_messages` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `mail_privacy_grants`
--

CREATE TABLE `mail_privacy_grants` (
  `owner_id` int unsigned NOT NULL,
  `user_id` int unsigned NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `messages_remain` int unsigned NOT NULL,
  PRIMARY KEY (`owner_id`,`user_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `mail_privacy_grants_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `mail_privacy_grants_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `media_cache`
--

CREATE TABLE `media_cache` (
  `url_hash` binary(16) NOT NULL,
  `size` int unsigned NOT NULL,
  `last_access` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `info` blob,
  `type` tinyint NOT NULL,
  PRIMARY KEY (`url_hash`),
  KEY `last_access` (`last_access`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `media_file_refs`
--

CREATE TABLE `media_file_refs` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `media_files`
--

CREATE TABLE `media_files` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `newsfeed`
--

CREATE TABLE `newsfeed` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `type` int unsigned NOT NULL,
  `author_id` int NOT NULL,
  `object_id` bigint unsigned DEFAULT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `type` (`type`,`object_id`,`author_id`),
  KEY `time` (`time`),
  KEY `author_id` (`author_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `newsfeed_comments`
--

CREATE TABLE `newsfeed_comments` (
  `user_id` int unsigned NOT NULL,
  `object_type` int unsigned NOT NULL,
  `object_id` bigint unsigned NOT NULL,
  `last_comment_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`object_type`,`object_id`,`user_id`),
  KEY `user_id` (`user_id`),
  KEY `last_comment_time` (`last_comment_time`),
  CONSTRAINT `newsfeed_comments_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `newsfeed_groups`
--

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `notifications`
--

CREATE TABLE `notifications` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `owner_id` int unsigned NOT NULL,
  `type` smallint unsigned NOT NULL,
  `object_id` bigint unsigned DEFAULT NULL,
  `object_type` smallint unsigned DEFAULT NULL,
  `related_object_id` bigint unsigned DEFAULT NULL,
  `related_object_type` smallint unsigned DEFAULT NULL,
  `actor_id` int DEFAULT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `owner_id` (`owner_id`),
  KEY `time` (`time`),
  KEY `object_type` (`object_type`,`object_id`),
  KEY `type` (`type`),
  KEY `related_object_type` (`related_object_type`,`related_object_id`),
  KEY `actor_id` (`actor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `photo_albums`
--

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
  `ap_comments` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ap_id` (`ap_id`),
  KEY `owner_user_id` (`owner_user_id`),
  KEY `owner_group_id` (`owner_group_id`),
  KEY `display_order` (`display_order`),
  CONSTRAINT `photo_albums_ibfk_1` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `photo_albums_ibfk_2` FOREIGN KEY (`owner_group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `photo_tags`
--

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
  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `photo_id` (`photo_id`,`user_id`),
  UNIQUE KEY `ap_id` (`ap_id`),
  KEY `user_id` (`user_id`),
  KEY `approved` (`approved`),
  CONSTRAINT `photo_tags_ibfk_1` FOREIGN KEY (`photo_id`) REFERENCES `photos` (`id`) ON DELETE CASCADE,
  CONSTRAINT `photo_tags_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `photos`
--

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `poll_options`
--

CREATE TABLE `poll_options` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `poll_id` int unsigned NOT NULL,
  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
  `text` text NOT NULL,
  `num_votes` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `ap_id` (`ap_id`),
  KEY `poll_id` (`poll_id`),
  CONSTRAINT `poll_options_ibfk_1` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `poll_votes`
--

CREATE TABLE `poll_votes` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `poll_id` int unsigned NOT NULL,
  `option_id` int unsigned NOT NULL,
  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ap_id` (`ap_id`),
  KEY `user_id` (`user_id`),
  KEY `poll_id` (`poll_id`),
  KEY `option_id` (`option_id`),
  CONSTRAINT `poll_votes_ibfk_2` FOREIGN KEY (`poll_id`) REFERENCES `polls` (`id`) ON DELETE CASCADE,
  CONSTRAINT `poll_votes_ibfk_3` FOREIGN KEY (`option_id`) REFERENCES `poll_options` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `polls`
--

CREATE TABLE `polls` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `owner_id` int NOT NULL,
  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
  `question` text,
  `is_anonymous` tinyint(1) NOT NULL DEFAULT '0',
  `is_multi_choice` tinyint(1) NOT NULL DEFAULT '0',
  `end_time` timestamp NULL DEFAULT NULL,
  `num_voted_users` int unsigned NOT NULL DEFAULT '0',
  `last_vote_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ap_id` (`ap_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `qsearch_index`
--

CREATE TABLE `qsearch_index` (
  `string` text NOT NULL,
  `user_id` int unsigned DEFAULT NULL,
  `group_id` int unsigned DEFAULT NULL,
  KEY `user_id` (`user_id`),
  KEY `group_id` (`group_id`),
  FULLTEXT KEY `string` (`string`),
  CONSTRAINT `qsearch_index_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `qsearch_index_ibfk_2` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

--
-- Table structure for table `report_actions`
--

CREATE TABLE `report_actions` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `report_id` int unsigned NOT NULL,
  `user_id` int unsigned NOT NULL,
  `action_type` tinyint unsigned NOT NULL,
  `text` text,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `extra` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `report_id` (`report_id`),
  CONSTRAINT `report_actions_ibfk_1` FOREIGN KEY (`report_id`) REFERENCES `reports` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `reports`
--

CREATE TABLE `reports` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `reporter_id` int unsigned DEFAULT NULL,
  `target_type` tinyint unsigned NOT NULL,
  `target_id` int NOT NULL,
  `comment` text NOT NULL,
  `moderator_id` int unsigned DEFAULT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `server_domain` varchar(100) DEFAULT NULL,
  `content` json DEFAULT NULL,
  `state` tinyint unsigned NOT NULL DEFAULT '0',
  `has_file_refs` tinyint(1) NOT NULL DEFAULT '1',
  `rules` tinyblob,
  `reason` int unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `reporter_id` (`reporter_id`),
  KEY `moderator_id` (`moderator_id`),
  KEY `state` (`state`),
  KEY `target_id` (`target_id`),
  KEY `has_file_refs` (`has_file_refs`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `rules`
--

CREATE TABLE `rules` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `title` varchar(300) NOT NULL,
  `description` text NOT NULL,
  `translations` json NOT NULL,
  `priority` int NOT NULL DEFAULT '0',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `servers`
--

CREATE TABLE `servers` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `host` varchar(100) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `software` varchar(100) DEFAULT NULL,
  `version` varchar(30) DEFAULT NULL,
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_error_day` date DEFAULT NULL,
  `error_day_count` int NOT NULL DEFAULT '0',
  `is_up` tinyint unsigned NOT NULL DEFAULT '1',
  `is_restricted` tinyint unsigned NOT NULL DEFAULT '0',
  `restriction` json DEFAULT NULL,
  `features` bigint unsigned NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `host` (`host`),
  KEY `is_up` (`is_up`),
  KEY `error_day_count` (`error_day_count`),
  KEY `is_restricted` (`is_restricted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `sessions`
--

CREATE TABLE `sessions` (
  `id` binary(64) NOT NULL,
  `account_id` int unsigned NOT NULL,
  `last_active` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `ip` binary(16) NOT NULL,
  `user_agent` bigint NOT NULL,
  PRIMARY KEY (`id`),
  KEY `account_id` (`account_id`),
  CONSTRAINT `sessions_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `signup_invitations`
--

CREATE TABLE `signup_invitations` (
  `code` binary(16) NOT NULL DEFAULT '\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0',
  `owner_id` int unsigned DEFAULT NULL,
  `created` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `signups_remaining` int unsigned NOT NULL,
  `email` varchar(200) DEFAULT NULL,
  `extra` json DEFAULT NULL,
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`code`),
  UNIQUE KEY `id` (`id`),
  UNIQUE KEY `email` (`email`),
  KEY `owner_id` (`owner_id`),
  CONSTRAINT `signup_invitations_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `accounts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `signup_requests`
--

CREATE TABLE `signup_requests` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `email` varchar(200) NOT NULL,
  `first_name` varchar(100) NOT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `reason` text NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `stats_daily`
--

CREATE TABLE `stats_daily` (
  `day` date NOT NULL,
  `type` int unsigned NOT NULL,
  `object_id` int unsigned NOT NULL,
  `count` int unsigned NOT NULL,
  PRIMARY KEY (`day`,`type`,`object_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `user_action_log`
--

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `user_agents`
--

CREATE TABLE `user_agents` (
  `hash` bigint NOT NULL,
  `user_agent` text NOT NULL,
  PRIMARY KEY (`hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `user_data_exports`
--

CREATE TABLE `user_data_exports` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` int unsigned NOT NULL,
  `state` tinyint unsigned NOT NULL,
  `size` bigint unsigned NOT NULL DEFAULT '0',
  `file_id` bigint DEFAULT NULL,
  `requested_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `user_roles`
--

CREATE TABLE `user_roles` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `permissions` varbinary(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `user_staff_notes`
--

CREATE TABLE `user_staff_notes` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `target_id` int unsigned NOT NULL,
  `author_id` int unsigned NOT NULL,
  `text` text NOT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `target_id` (`target_id`),
  CONSTRAINT `user_staff_notes_ibfk_1` FOREIGN KEY (`target_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `fname` varchar(100) NOT NULL DEFAULT '',
  `lname` varchar(100) DEFAULT NULL,
  `middle_name` varchar(100) DEFAULT NULL,
  `maiden_name` varchar(100) DEFAULT NULL,
  `bdate` date DEFAULT NULL,
  `username` varchar(64) NOT NULL,
  `domain` varchar(100) NOT NULL DEFAULT '',
  `public_key` blob NOT NULL,
  `private_key` blob,
  `ap_url` varchar(300) DEFAULT NULL,
  `ap_inbox` varchar(300) DEFAULT NULL,
  `ap_shared_inbox` varchar(300) DEFAULT NULL,
  `about` text,
  `about_source` text,
  `gender` tinyint unsigned NOT NULL DEFAULT '0',
  `profile_fields` json DEFAULT NULL,
  `avatar` json DEFAULT NULL,
  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
  `last_updated` timestamp NULL DEFAULT NULL,
  `flags` bigint unsigned NOT NULL,
  `endpoints` json DEFAULT NULL,
  `privacy` json DEFAULT NULL,
  `ban_status` tinyint unsigned NOT NULL DEFAULT '0',
  `ban_info` json DEFAULT NULL,
  `presence` json DEFAULT NULL,
  `is_online` tinyint(1) GENERATED ALWAYS AS (ifnull(cast(json_extract(`presence`,_utf8mb4'$.isOnline') as unsigned),0)) VIRTUAL NOT NULL,
  `num_followers` bigint NOT NULL DEFAULT '0',
  `num_following` bigint NOT NULL DEFAULT '0',
  `num_friends` bigint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`,`domain`),
  UNIQUE KEY `ap_id` (`ap_id`),
  KEY `ban_status` (`ban_status`),
  KEY `num_followers` (`num_followers`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `wall_pinned_posts`
--

CREATE TABLE `wall_pinned_posts` (
  `owner_user_id` int unsigned NOT NULL,
  `post_id` int unsigned NOT NULL,
  `display_order` int unsigned NOT NULL,
  PRIMARY KEY (`owner_user_id`,`post_id`),
  KEY `post_id` (`post_id`),
  CONSTRAINT `wall_pinned_posts_ibfk_1` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `wall_pinned_posts_ibfk_2` FOREIGN KEY (`post_id`) REFERENCES `wall_posts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `wall_posts`
--

CREATE TABLE `wall_posts` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `author_id` int unsigned DEFAULT NULL,
  `owner_user_id` int unsigned DEFAULT NULL,
  `owner_group_id` int unsigned DEFAULT NULL,
  `text` text,
  `attachments` json DEFAULT NULL,
  `repost_of` int unsigned DEFAULT NULL,
  `ap_url` varchar(300) DEFAULT NULL,
  `ap_id` varchar(300) CHARACTER SET ascii COLLATE ascii_general_ci DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `content_warning` text,
  `updated_at` timestamp NULL DEFAULT NULL,
  `reply_key` varbinary(1024) DEFAULT NULL,
  `mentions` varbinary(1024) DEFAULT NULL,
  `reply_count` int unsigned NOT NULL DEFAULT '0',
  `ap_replies` varchar(300) DEFAULT NULL,
  `poll_id` int unsigned DEFAULT NULL,
  `federation_state` tinyint unsigned NOT NULL DEFAULT '0',
  `source` text,
  `source_format` tinyint unsigned DEFAULT NULL,
  `privacy` tinyint unsigned NOT NULL DEFAULT '0',
  `flags` bit(64) NOT NULL DEFAULT b'0',
  `action` tinyint unsigned DEFAULT NULL,
  `top_parent_is_wall_to_wall` tinyint(1) GENERATED ALWAYS AS (((`flags` & 2) = 2)) VIRTUAL,
  `extra` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ap_id` (`ap_id`),
  KEY `owner_user_id` (`owner_user_id`),
  KEY `repost_of` (`repost_of`),
  KEY `author_id` (`author_id`),
  KEY `reply_key` (`reply_key`),
  KEY `owner_group_id` (`owner_group_id`),
  KEY `poll_id` (`poll_id`),
  KEY `top_parent_is_wall_to_wall` (`top_parent_is_wall_to_wall`),
  CONSTRAINT `wall_posts_ibfk_1` FOREIGN KEY (`owner_user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  CONSTRAINT `wall_posts_ibfk_4` FOREIGN KEY (`owner_group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Table structure for table `word_filters`
--

CREATE TABLE `word_filters` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `owner_id` int unsigned NOT NULL,
  `name` varchar(300) NOT NULL,
  `words` json NOT NULL,
  `contexts` bit(32) NOT NULL,
  `expires_at` timestamp NULL DEFAULT NULL,
  `action` tinyint unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `owner_id` (`owner_id`),
  KEY `expires_at` (`expires_at`),
  CONSTRAINT `word_filters_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;

-- Dump completed on 2025-09-20  9:01:08
