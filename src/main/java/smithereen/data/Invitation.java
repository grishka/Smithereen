package smithereen.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import smithereen.Utils;

public class Invitation{
	public String code;
	public Timestamp createdAt;

	public static Invitation fromResultSet(ResultSet res) throws SQLException{
		Invitation inv=new Invitation();
		inv.code=Utils.byteArrayToHexString(res.getBytes("code"));
		inv.createdAt=res.getTimestamp("created");
		return inv;
	}
}
