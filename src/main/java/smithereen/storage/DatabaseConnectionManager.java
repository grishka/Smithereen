package smithereen.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import smithereen.Config;

public class DatabaseConnectionManager{
	private static final ThreadLocal<ConnectionWrapper> connection=new ThreadLocal<>();
	private static final Logger LOG=LoggerFactory.getLogger(DatabaseConnectionManager.class);

	public static Connection getConnection() throws SQLException{
		ConnectionWrapper conn=connection.get();
		if(conn!=null){
			if(System.currentTimeMillis()-conn.lastUsed<5*60000){
				conn.lastUsed=System.currentTimeMillis();
				return conn.connection;
			}else{
				LOG.debug("Closing possibly stale database connection");
				conn.connection.close();
			}
		}
		LOG.debug("Opening new database connection");
		if(conn==null)
			conn=new ConnectionWrapper();
		conn.connection=DriverManager.getConnection("jdbc:mysql://"+Config.dbHost+"/"+Config.dbName+"?serverTimezone=GMT&connectionTimeZone=GMT&useUnicode=true&characterEncoding=UTF-8&forceConnectionTimeZoneToSession=true&useSSL=false&allowPublicKeyRetrieval=true",
				Config.dbUser, Config.dbPassword);
		conn.connection.createStatement().execute("SET @@SQL_MODE = REPLACE(@@SQL_MODE, 'STRICT_TRANS_TABLES', '')");
		conn.connection.createStatement().execute("SET @@session.time_zone='+00:00'");
		conn.lastUsed=System.currentTimeMillis();
		connection.set(conn);
		return conn.connection;
	}

	private static class ConnectionWrapper{
		public Connection connection;
		public long lastUsed;
	}
}
