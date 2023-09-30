package smithereen.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import smithereen.storage.DatabaseUtils;

public record MessagesPrivacyGrant(int ownerID, int userID, Instant createdAt, int messagesRemain){
	public static MessagesPrivacyGrant fromResultSet(ResultSet res) throws SQLException{
		return new MessagesPrivacyGrant(res.getInt("owner_id"), res.getInt("user_id"), DatabaseUtils.getInstant(res, "created_at"), res.getInt("messages_remain"));
	}

	public boolean isValid(){
		return messagesRemain>0 && createdAt().isAfter(Instant.now().minus(7, ChronoUnit.DAYS));
	}
}
