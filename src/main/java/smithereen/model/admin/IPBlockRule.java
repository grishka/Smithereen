package smithereen.model.admin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.Utils;
import smithereen.storage.DatabaseUtils;
import smithereen.util.InetAddressRange;
import smithereen.util.TranslatableEnum;

public record IPBlockRule(int id, InetAddressRange ipRange, Action action, Instant expiresAt){

	public static IPBlockRule fromResultSet(ResultSet res) throws SQLException{
		return new IPBlockRule(
				res.getInt("id"),
				new InetAddressRange(Utils.deserializeInetAddress(res.getBytes("address")), res.getInt("prefix_length")),
				Action.values()[res.getInt("action")],
				DatabaseUtils.getInstant(res, "expires_at")
		);
	}

	public enum Action implements TranslatableEnum<Action>{
		MANUAL_REVIEW_SIGNUPS,
		BLOCK_SIGNUPS;

		@Override
		public String getLangKey(){
			return switch(this){
				case MANUAL_REVIEW_SIGNUPS -> "admin_email_rule_review";
				case BLOCK_SIGNUPS -> "admin_email_rule_reject";
			};
		}
	}
}
