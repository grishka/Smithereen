package smithereen.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import smithereen.Config;

public class DatabaseConnectionManager{
	private static ThreadLocal<ConnectionWrapper> connection=new ThreadLocal<>();

	public static Connection getConnection() throws SQLException{
		ConnectionWrapper conn=connection.get();
		if(conn!=null){
			if(System.currentTimeMillis()-conn.lastUsed<5*60000){
				conn.lastUsed=System.currentTimeMillis();
				return conn.connection;
			}else{
				conn.connection.close();
			}
		}
		System.out.println("Opening new database connection for thread "+Thread.currentThread().getName());
		if(conn==null)
			conn=new ConnectionWrapper();
//		long t=System.currentTimeMillis();
		conn.connection=DriverManager.getConnection("jdbc:mysql://"+Config.dbHost+"/"+Config.dbName+"?serverTimezone=GMT&connectionTimeZone=GMT&useUnicode=true&characterEncoding=UTF-8&forceConnectionTimeZoneToSession=true",
				Config.dbUser, Config.dbPassword);
//		System.out.println("open: "+(System.currentTimeMillis()-t));
		conn.connection.createStatement().execute("SET @@SQL_MODE = REPLACE(@@SQL_MODE, 'STRICT_TRANS_TABLES', '')");
//		System.out.println("set mode: "+(System.currentTimeMillis()-t));
//		conn.connection.createStatement().execute("SET @@session.time_zone='+00:00'");
//		System.out.println("set tz: "+(System.currentTimeMillis()-t));
		conn.lastUsed=System.currentTimeMillis();
		connection.set(conn);
		return conn.connection;
	}

	private static class ConnectionWrapper{
		public Connection connection;
		public long lastUsed;
	}
}
