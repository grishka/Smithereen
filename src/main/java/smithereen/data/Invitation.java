package smithereen.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;

public class Invitation{
	public String code;
	public Instant createdAt;

	public static Invitation fromResultSet(ResultSet res) throws SQLException{
		Invitation inv=new Invitation();
		inv.code=Utils.byteArrayToHexString(res.getBytes("code"));
		inv.createdAt=DatabaseUtils.getInstant(res, "created");
		return inv;
	}
}
