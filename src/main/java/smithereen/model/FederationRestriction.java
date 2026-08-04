package smithereen.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

import smithereen.storage.DatabaseUtils;

public class FederationRestriction{
	public String domain;
	public RestrictionType type;
	public String publicComment, privateComment;
	public Instant createdAt;
	public int moderatorId;
	public int flags;

	public static FederationRestriction fromResultSet(ResultSet res) throws SQLException{
		FederationRestriction r=new FederationRestriction();
		r.domain=res.getString("domain");
		r.createdAt=DatabaseUtils.getInstant(res, "created_at");
		r.moderatorId=res.getInt("moderator_id");
		r.publicComment=res.getString("public_comment");
		r.privateComment=res.getString("private_comment");
		r.type=RestrictionType.values()[res.getInt("restriction_type")];
		r.flags=res.getInt("flags");
		return r;
	}

	public String getDisplayDomain(){
		return domain;
	}

	public enum RestrictionType{
		SUSPENSION,
	}
}
