package smithereen.model.apps;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumSet;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;

public record AppAuthCode(int accountID, long appID, EnumSet<ClientAppPermission> permissions, Instant expiresAt, String redirectURI, String s256CodeChallenge){
	public static AppAuthCode fromResultSet(ResultSet res) throws SQLException{
		JsonObject extra=JsonParser.parseString(res.getString("extra")).getAsJsonObject();
		return new AppAuthCode(
				res.getInt("account_id"),
				res.getLong("app_id"),
				Utils.deserializeEnumSet(ClientAppPermission.class, res.getLong("permissions")),
				DatabaseUtils.getInstant(res, "expires_at"),
				extra.get("redirectURI").getAsString(),
				extra.has("codeChallenge") ? extra.get("codeChallenge").getAsString() : null
		);
	}
}
