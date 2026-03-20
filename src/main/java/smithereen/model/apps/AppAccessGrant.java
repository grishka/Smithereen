package smithereen.model.apps;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.EnumSet;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;

public record AppAccessGrant(int accountID, long appID, Instant grantedAt, EnumSet<ClientAppPermission> permissions){
	public static AppAccessGrant fromResultSet(ResultSet res) throws SQLException{
		return new AppAccessGrant(
				res.getInt("account_id"),
				res.getLong("app_id"),
				DatabaseUtils.getInstant(res, "granted_at"),
				Utils.deserializeEnumSet(ClientAppPermission.class, res.getLong("permissions"))
		);
	}
}
