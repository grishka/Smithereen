package smithereen.model;

import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;
import smithereen.util.uaparser.BrowserInfo;
import smithereen.util.uaparser.UserAgentParser;

public record OtherSession(int id, InetAddress ip, Instant lastActive, String userAgent, BrowserInfo browserInfo){
	public static OtherSession fromResultSet(ResultSet res) throws SQLException{
		byte[] id=res.getBytes("id");
		String ua=res.getString("user_agent_str");
		return new OtherSession((int)Utils.unpackLong(id), Utils.deserializeInetAddress(res.getBytes("ip")), DatabaseUtils.getInstant(res, "last_active"), ua, UserAgentParser.parse(ua));
	}
}
