package smithereen.model.admin;

import org.jetbrains.annotations.NotNull;

import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.util.TranslatableEnum;
import smithereen.util.UriRenderer;

/**
 * @param domain The ASCII-only representation of the rule's domain (<a href="https://en.wikipedia.org/wiki/Punycode">Punycode</a>-encoded if necessary).
 * <b>Not suitable for displaying in the UI!</b> Use {@link getHumanReadableDomain} for that.
 */
public record EmailDomainBlockRule(@NotNull String domain, Action action){

	public static EmailDomainBlockRule fromResultSet(ResultSet res) throws SQLException{
		return new EmailDomainBlockRule(
				res.getString("domain"),
				Action.values()[res.getInt("action")]
		);
	}

	public boolean matches(String domain){
		return this.domain.equalsIgnoreCase(domain);
	}

	/**
	 * <p>The representation of the rule's domain suitable for displaying in the UI.</p>
	 * <p><b>Do not use this in business logic!</b> Use {@link domain} instead.</p>
	 */
	@NotNull
	public String getHumanReadableDomain(){
		return UriRenderer.renderDomain(domain);
	}

	public enum Action implements TranslatableEnum<Action>{
		MANUAL_REVIEW,
		BLOCK;

		@Override
		public String getLangKey(){
			return switch(this){
				case MANUAL_REVIEW -> "admin_email_rule_review";
				case BLOCK -> "admin_email_rule_reject";
			};
		}
	}
}
