package smithereen.model;

import java.time.Instant;

public class FederationRestriction{
	public RestrictionType type;
	public String publicComment, privateComment;
	public Instant createdAt;
	public int moderatorId;

	public enum RestrictionType{
		NONE,
		SUSPENSION,
	}
}
