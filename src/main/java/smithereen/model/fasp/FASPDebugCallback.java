package smithereen.model.fasp;

import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;

public record FASPDebugCallback(int id, InetAddress ip, String body, Instant receivedAt){
	public static FASPDebugCallback fromResultSet(ResultSet res) throws SQLException{
		return new FASPDebugCallback(
				res.getInt("id"),
				Utils.deserializeInetAddress(res.getBytes("ip")),
				res.getString("body"),
				DatabaseUtils.getInstant(res, "received_at")
		);
	}
}
