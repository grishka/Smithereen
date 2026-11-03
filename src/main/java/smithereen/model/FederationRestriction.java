package smithereen.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.BreakIterator;
import java.time.Instant;

import smithereen.storage.DatabaseUtils;
import smithereen.util.UriRenderer;

public class FederationRestriction{
	public static final int FLAG_DOMAIN_OBFUSCATED=1;

	/**
	 * <p>The ASCII-only representation of the restriction's domain (<a href="https://en.wikipedia.org/wiki/Punycode">Punycode</a>-encoded if necessary).</p>
	 * <p><b>Not suitable for displaying in the UI!</b> Use {@link getHumanReadableDomain} for that.</p>
	 */
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

	/**
	 * <p>The representation of the restriction's domain suitable for displaying in the UI.</p>
	 * <p><b>Do not use this in business logic!</b> Use {@link domain} instead.</p>
	 */
	public String getHumanReadableDomain(){
		return UriRenderer.renderDomain(domain);
	}

	public String getDisplayDomain(){
		String domain=getHumanReadableDomain();
		if(isDomainObfuscated()){
			// https://github.com/mastodon/mastodon/blob/b625f21ceab87556c990344d586a231b6c4559e3/app/models/domain_block.rb#L87
			// Iterate over extended grapheme clusters in case of a non-ASCII domain name
			BreakIterator it=BreakIterator.getCharacterInstance();
			it.setText(domain);

			int domainLength=0;
			while(it.next()!=BreakIterator.DONE) ++domainLength;
			it.setText(domain); // Reset the iterator

			int visibleRatio=domainLength/4;

			StringBuilder chars=new StringBuilder(domain.length());
			for(int start=it.first(), end=it.next();end!=BreakIterator.DONE;start=end, end=it.next(), --visibleRatio){
				String grapheme=domain.substring(start, end);
				if(grapheme.equals(".") || visibleRatio<=0){
					chars.append(grapheme);
				}else{
					chars.append("*");
				}
			}

			return chars.toString();
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
