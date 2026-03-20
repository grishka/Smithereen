package smithereen.model.apps;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Base64;
import java.util.EnumSet;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;

public record AppAccessToken(byte[] id, int accountID, long appID, Instant expiresAt, EnumSet<ClientAppPermission> permissions){
	public static AppAccessToken fromResultSet(ResultSet res) throws SQLException{
		return new AppAccessToken(
				res.getBytes("id"),
				res.getInt("account_id"),
				res.getLong("app_id"),
				DatabaseUtils.getInstant(res, "expires_at"),
				Utils.deserializeEnumSet(ClientAppPermission.class, res.getLong("permissions"))
		);
	}

	public String getEncodedID(){
		return Base64.getUrlEncoder().withoutPadding().encodeToString(id);
	}
}
