package smithereen.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.LruCache;
import smithereen.data.NodeInfo;

public class NodeInfoStorage{

	private static LruCache<String, NodeInfo> cache=new LruCache<>(500);

	public static void put(NodeInfo info) throws SQLException{
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT count(*) FROM `servers` WHERE `host`=?");
		stmt.setString(1, info.host);
		try(ResultSet res=stmt.executeQuery()){
			res.first();
			boolean exists=res.getInt(1)==1;
			if(exists){
				stmt=conn.prepareStatement("UPDATE `servers` SET `software`=?, `version`=?, `capabilities`=?, `last_updated`=CURRENT_TIMESTAMP()");
				stmt.setString(1, info.softwareName);
				stmt.setString(2, info.softwareVersion);
				stmt.setLong(3, info.capabilities);
			}else{
				stmt=conn.prepareStatement("INSERT INTO `servers` (`host`, `software`, `version`, `capabilities`) VALUES (?, ?, ?, ?)");
				stmt.setString(1, info.host);
				stmt.setString(2, info.softwareName);
				stmt.setString(3, info.softwareVersion);
				stmt.setLong(4, info.capabilities);
			}
			stmt.execute();
		}
		cache.put(info.host, info);
	}

	public static NodeInfo get(String host) throws SQLException{
		NodeInfo info=cache.get(host);
		if(info!=null)
			return info;
		Connection conn=DatabaseConnectionManager.getConnection();
		PreparedStatement stmt=conn.prepareStatement("SELECT * FROM `servers` WHERE `host`=?");
		stmt.setString(1, host);
		try(ResultSet res=stmt.executeQuery()){
			if(res.first()){
				info=new NodeInfo(res);
				cache.put(info.host, info);
				return info;
			}
		}
		return null;
	}
}
