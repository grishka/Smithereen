package smithereen.storage.migrations;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.stream.Collectors;

import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.SQLQueryBuilder;

class SchemaMigration28 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
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
		for(String domain: servers){
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
}
