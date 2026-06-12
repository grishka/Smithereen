package smithereen.storage.migrations;

import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;
import smithereen.storage.sql.SQLQueryBuilder;
import smithereen.util.Passwords;

class SchemaMigration53 extends DatabaseSchemaMigration{
	@Override
	public void doMigration(DatabaseConnection conn) throws SQLException{
		migratePasswordsToSaltedHashes(conn);
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
}
