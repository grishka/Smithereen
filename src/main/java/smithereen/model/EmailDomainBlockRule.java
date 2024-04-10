package smithereen.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import smithereen.util.TranslatableEnum;

public record EmailDomainBlockRule(String domain, Action action){

	public static EmailDomainBlockRule fromResultSet(ResultSet res) throws SQLException{
		return new EmailDomainBlockRule(
				res.getString("domain"),
				Action.values()[res.getInt("action")]
		);
	}

	public boolean matches(String domain){
		return this.domain.equalsIgnoreCase(domain);
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
