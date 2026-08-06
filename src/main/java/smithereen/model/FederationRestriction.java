package smithereen.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Arrays;

import smithereen.storage.DatabaseUtils;

public class FederationRestriction{
	public static final int FLAG_DOMAIN_OBFUSCATED=1;

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
		if(isDomainObfuscated()){
			// https://github.com/mastodon/mastodon/blob/b625f21ceab87556c990344d586a231b6c4559e3/app/models/domain_block.rb#L87
			int visibleRatio=domain.length()/4;
			char[] chars=domain.toCharArray();
			for(int i=visibleRatio;i<chars.length-visibleRatio;i++){
				if(chars[i]!='.')
					chars[i]='*';
			}
			return new String(chars);
		}
		return domain;
	}

	public boolean isDomainObfuscated(){
		return (flags & FLAG_DOMAIN_OBFUSCATED)!=0;
	}

	public enum RestrictionType{
		SUSPENSION,
	}
}
