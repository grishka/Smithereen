package smithereen.storage.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;

import smithereen.Config;

public class DatabaseConnectionManager{
	private static final Logger LOG=LoggerFactory.getLogger(DatabaseConnectionManager.class);
	private static final LinkedList<DatabaseConnection> pool=new LinkedList<>();
	private static final ThreadLocal<DatabaseConnection> currentThreadConnection=new ThreadLocal<>();

	public static synchronized DatabaseConnection getConnection() throws SQLException{
		DatabaseConnection conn;
		conn=currentThreadConnection.get();
		if(conn!=null){
			conn.useDepth++;
			return conn;
		}
		if(pool.isEmpty()){
			conn=new DatabaseConnection(newConnection());
		}else{
			conn=pool.removeFirst();
			try{
				validateConnection(conn.actualConnection);
				conn.lastUsed=System.nanoTime();
			}catch(SQLException x){
				LOG.debug("Failed to validate database connection, reopening");
				conn.actualConnection.close();
				conn=new DatabaseConnection(newConnection());
			}
		}

		conn.useDepth++;
		conn.ownerThread=Thread.currentThread();
		currentThreadConnection.set(conn);
		return conn;
	}

	static synchronized void reuseConnection(DatabaseConnection conn){
		if(conn.ownerThread!=Thread.currentThread())
			throw new IllegalStateException("Connections are not meant to be shared across threads");
		conn.useDepth--;
		if(conn.useDepth==0){
			conn.ownerThread=null;
			currentThreadConnection.remove();
			pool.add(conn);
			LOG.trace("Reusing database connection. Pool size is {}", pool.size());
		}
	}

	private static Connection newConnection() throws SQLException{
		LOG.trace("Opening new database connection");
		Connection conn=DriverManager.getConnection("jdbc:mysql://"+Config.dbHost+"/"+Config.dbName+"?serverTimezone=GMT&connectionTimeZone=GMT&useUnicode=true&characterEncoding=UTF-8&forceConnectionTimeZoneToSession=true&useSSL=false&allowPublicKeyRetrieval=true",
				Config.dbUser, Config.dbPassword);
		conn.createStatement().execute("SET @@SQL_MODE = REPLACE(@@SQL_MODE, 'STRICT_TRANS_TABLES', '')");
		conn.createStatement().execute("SET @@session.time_zone='+00:00'");
		return conn;
	}

	private static void validateConnection(Connection conn) throws SQLException{
		conn.createStatement().execute("/* ping */");
	}

	public static synchronized void closeUnusedConnections(){
		LOG.trace("Closing unused connections");
		int size=pool.size();
		boolean removedAny=pool.removeIf(conn->{
			if(System.nanoTime()-conn.lastUsed>5*60_000_000_000L){
				try{
					conn.actualConnection.close();
				}catch(SQLException ignore){}
				return true;
			}
			return false;
		});
		if(removedAny){
			LOG.debug("Closed {} connections, pool size is {}", size-pool.size(), pool.size());
		}
	}
}
