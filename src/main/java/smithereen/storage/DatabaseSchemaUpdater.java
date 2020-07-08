package smithereen.storage;

import java.sql.SQLException;

import smithereen.Config;

public class DatabaseSchemaUpdater{
	public static final int SCHEMA_VERSION=1;

	public static void maybeUpdate() throws SQLException{
		if(Config.dbSchemaVersion==0){
			Config.updateInDatabase("SchemaVersion", SCHEMA_VERSION+"");
		}
	}
}
