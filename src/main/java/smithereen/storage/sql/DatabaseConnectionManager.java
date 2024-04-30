package smithereen.storage.sql;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import smithereen.Config;

public class DatabaseConnectionManager{
	private static final Logger LOG=LoggerFactory.getLogger(DatabaseConnectionManager.class);
	private static final ArrayList<DatabaseConnection> pool=new ArrayList<>();
	private static final ThreadLocal<DatabaseConnection> currentThreadConnection=new ThreadLocal<>();
	private static final ArrayList<DatabaseConnection> connectionsInUse=new ArrayList<>();
	private static final boolean DEBUG_CONNECTION_LEAKS=System.getProperty("smithereen.debugDatabaseConnections")!=null;
	private static final Semaphore semaphore=new Semaphore(Config.dbMaxConnections);

	public static synchronized DatabaseConnection getConnection() throws SQLException{
		DatabaseConnection conn;
		conn=currentThreadConnection.get();
		if(conn!=null){
			conn.useDepth++;
			return conn;
		}
		if(pool.isEmpty()){
			if(DEBUG_CONNECTION_LEAKS)
				conn=new DebugDatabaseConnection(newConnection());
			else
				conn=new DatabaseConnection(newConnection());
		}else{
			conn=pool.removeLast();
			try{
				validateConnection(conn.actualConnection);
				conn.lastUsed=System.nanoTime();
			}catch(SQLException x){
				LOG.debug("Failed to validate database connection, reopening");
				closeConnection(conn);
				conn=new DatabaseConnection(newConnection());
			}
		}

		conn.useDepth++;
		conn.ownerThread=Thread.currentThread();
		currentThreadConnection.set(conn);
		connectionsInUse.add(conn);
		if(DEBUG_CONNECTION_LEAKS && conn instanceof DebugDatabaseConnection ddc)
			ddc.throwableForStack=new Exception().fillInStackTrace();
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
			connectionsInUse.remove(conn);
			LOG.trace("Reusing database connection. Pool size is {}", pool.size());
		}
	}

	private static Connection newConnection() throws SQLException{
		try{
			semaphore.acquire();
		}catch(InterruptedException x){
			throw new RuntimeException(x);
		}
		LOG.trace("Opening new database connection");
		Connection conn=DriverManager.getConnection("jdbc:mysql://"+Config.dbHost+"/"+Config.dbName+"?serverTimezone=GMT&connectionTimeZone=GMT&useUnicode=true&characterEncoding=UTF-8&forceConnectionTimeZoneToSession=true&useSSL=false&allowPublicKeyRetrieval=true",
				Config.dbUser, Config.dbPassword);
		conn.createStatement().execute("SET @@SQL_MODE = REPLACE(@@SQL_MODE, 'STRICT_TRANS_TABLES', '')");
		conn.createStatement().execute("SET @@session.time_zone='+00:00'");
		return conn;
	}

	private static void closeConnection(DatabaseConnection conn){
		try{
			conn.actualConnection.close();
		}catch(SQLException ignore){}
		semaphore.release();
	}

	private static void validateConnection(Connection conn) throws SQLException{
		conn.createStatement().execute("/* ping */");
	}

	public static synchronized void closeUnusedConnections(){
		LOG.trace("Closing unused connections");
		int size=pool.size();
		boolean removedAny=pool.removeIf(conn->{
			if(System.nanoTime()-conn.lastUsed>5*60_000_000_000L){
				if(conn.useDepth!=0)
					throw new IllegalStateException("Connection use depth is "+conn.useDepth+", expected 0");
				closeConnection(conn);
				return true;
			}
			return false;
		});
		if(removedAny){
			LOG.debug("Closed {} connections, pool size is {}", size-pool.size(), pool.size());
		}
		for(DatabaseConnection conn:connectionsInUse){
			if(System.nanoTime()-conn.lastUsed>60_000_000_000L){
				LOG.warn("Database connection {} was not closed! Owner: {}", conn, conn.ownerThread);
				if(conn instanceof DebugDatabaseConnection ddc)
					LOG.warn("Last opened at:", ddc.throwableForStack);
			}
		}
	}
}
