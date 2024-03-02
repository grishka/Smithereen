package smithereen.storage.sql;

import java.sql.Connection;

public class DebugDatabaseConnection extends DatabaseConnection{
	Throwable throwableForStack;

	public DebugDatabaseConnection(Connection actualConnection){
		super(actualConnection);
	}
}
