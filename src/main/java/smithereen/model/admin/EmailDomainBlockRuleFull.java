package smithereen.model.admin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.storage.DatabaseUtils;

public record EmailDomainBlockRuleFull(EmailDomainBlockRule rule, Instant createdAt, String note, int creatorID){
	public static EmailDomainBlockRuleFull fromResultSet(ResultSet res) throws SQLException{
		return new EmailDomainBlockRuleFull(
				EmailDomainBlockRule.fromResultSet(res),
				DatabaseUtils.getInstant(res, "created_at"),
				res.getString("note"),
				res.getInt("creator_id")
		);
	}
}
