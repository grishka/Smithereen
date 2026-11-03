package smithereen.storage.migrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import smithereen.storage.sql.DatabaseConnection;

public abstract class DatabaseSchemaMigration{
	protected static final Logger LOG=LoggerFactory.getLogger(DatabaseSchemaMigration.class);

	public abstract void doMigration(DatabaseConnection conn) throws SQLException;

	public static DatabaseSchemaMigration get(int version){
		try{
			Class<?> migrationClass=Class.forName("smithereen.storage.migrations.SchemaMigration"+version);
			if(migrationClass.getDeclaredConstructor().newInstance() instanceof DatabaseSchemaMigration dsm){
				return dsm;
			}else{
				throw new IllegalStateException("Class "+migrationClass+" does not extend DatabaseSchemaMigration");
			}
		}catch(ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException x){
			throw new IllegalStateException("Failed to find and instantiate database schema migration for version "+version, x);
		}
	}
}
