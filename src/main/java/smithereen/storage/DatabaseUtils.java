package smithereen.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import smithereen.Utils;

public class DatabaseUtils{

	private static final Object UNIQUE_USERNAME_LOCK=new Object();

	public static List<Integer> intResultSetToList(ResultSet res) throws SQLException{
		res.beforeFirst();
		ArrayList<Integer> list=new ArrayList<>();
		while(res.next()){
			list.add(res.getInt(1));
		}
		res.close();
		return list;
	}

	public static int oneFieldToInt(final ResultSet res) throws SQLException{
		try(res){
			return res.first() ? res.getInt(1) : -1;
		}
	}

	public static boolean runWithUniqueUsername(String username, DatabaseRunnable action) throws SQLException{
		if(!Utils.isValidUsername(username))
			return false;
		if(Utils.isReservedUsername(username))
			return false;
		synchronized(UNIQUE_USERNAME_LOCK){
			Connection conn=DatabaseConnectionManager.getConnection();
			PreparedStatement stmt=conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username=? AND domain=''");
			stmt.setString(1, username);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				if(res.getInt(1)>0)
					return false;
			}
			stmt=conn.prepareStatement("SELECT COUNT(*) FROM groups WHERE username=? AND domain=''");
			stmt.setString(1, username);
			try(ResultSet res=stmt.executeQuery()){
				res.first();
				if(res.getInt(1)>0)
					return false;
			}
			action.run();
			return true;
		}
	}

	public static int insertAndGetID(PreparedStatement stmt) throws SQLException{
		stmt.execute();
		try(ResultSet keys=stmt.getGeneratedKeys()){
			keys.first();
			return keys.getInt(1);
		}
	}
}
