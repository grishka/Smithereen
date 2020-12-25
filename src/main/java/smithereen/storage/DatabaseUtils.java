package smithereen.storage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatabaseUtils{
	public static List<Integer> intResultSetToList(ResultSet res) throws SQLException{
		res.beforeFirst();
		ArrayList<Integer> list=new ArrayList<>();
		while(res.next()){
			list.add(res.getInt(1));
		}
		return list;
	}
}
