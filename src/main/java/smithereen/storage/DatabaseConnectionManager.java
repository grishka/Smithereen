package smithereen.storage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import smithereen.Config;

public class DatabaseConnectionManager{
	private static ThreadLocal<Connection> connection=new ThreadLocal<>();

	public static Connection getConnection() throws SQLException{
		Connection conn=connection.get();
		if(conn!=null && !conn.isClosed())
			return conn;
		System.out.println("Opening new database connection for thread "+Thread.currentThread().getName());
		conn=DriverManager.getConnection("jdbc:mysql://"+Config.dbHost+"/"+Config.dbName+"?user="+Config.dbUser+"&password="+Config.dbPassword+"&useGmtMillisForDatetimes=true&serverTimezone=GMT&useUnicode=true&characterEncoding=UTF-8&connectTimeout=0&socketTimeout=0&autoReconnect=true");
		conn.createStatement().execute("SET @@SQL_MODE = REPLACE(@@SQL_MODE, 'STRICT_TRANS_TABLES', '')");
		conn.createStatement().execute("SET @@session.time_zone='+00:00'");
		connection.set(conn);
		return conn;
	}
}
