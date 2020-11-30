package smithereen.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import smithereen.Config;

public class DatabaseSchemaUpdater{
	public static final int SCHEMA_VERSION=3;

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
		}
	}
}
