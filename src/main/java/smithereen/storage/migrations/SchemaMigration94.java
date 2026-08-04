package smithereen.storage.migrations;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.Utils;
import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.SQLQueryBuilder;

public class SchemaMigration94 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		conn.createStatement().execute("""
			CREATE TABLE `blocks_domain` (
			  `domain` varchar(100) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
			  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
			  `moderator_id` int unsigned DEFAULT NULL,
			  `public_comment` text NOT NULL,
			  `private_comment` text NOT NULL,
			  `restriction_type` tinyint unsigned NOT NULL,
			  `flags` bit(32) NOT NULL DEFAULT b'0',
			  PRIMARY KEY (`domain`),
			  KEY `moderator_id` (`moderator_id`),
			  CONSTRAINT `blocks_domain_ibfk_1` FOREIGN KEY (`moderator_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
			) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

		ResultSet res=new SQLQueryBuilder(conn)
				.selectFrom("servers")
				.columns("host", "restriction")
				.where("restriction IS NOT NULL")
				.execute();
		try(res){
			while(res.next()){
				String domain=res.getString(1);
				FederationRestrictionV93 restriction=Utils.gson.fromJson(res.getString(2), FederationRestrictionV93.class);
				new SQLQueryBuilder(conn)
						.insertInto("blocks_domain")
						.value("domain", domain)
						.value("created_at", restriction.createdAt)
						.valueExpr("moderator_id", "IF(EXISTS(SELECT 1 FROM users WHERE id=?), ?, NULL)", restriction.moderatorId, restriction.moderatorId)
						.value("public_comment", restriction.publicComment)
						.value("private_comment", restriction.privateComment)
						.value("restriction_type", restriction.type.ordinal()-1)
						.executeNoResult();
			}
		}
		conn.createStatement().execute("ALTER TABLE servers DROP restriction, DROP is_restricted");
	}

	private static class FederationRestrictionV93{
		public RestrictionType type;
		public String publicComment, privateComment;
		public Instant createdAt;
		public int moderatorId;

		public enum RestrictionType{
			NONE,
			SUSPENSION,
		}
	}
}
