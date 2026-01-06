package smithereen.model.groups;

public enum GroupFeatureState{
	ENABLED_OPEN,
	ENABLED_RESTRICTED,
	ENABLED_CLOSED,
	DISABLED;

	public String asActivityPubValue(){
		return switch(this){
			case ENABLED_OPEN -> "sm:Open";
			case ENABLED_RESTRICTED -> "sm:Restricted";
			case ENABLED_CLOSED -> "sm:Closed";
			case DISABLED -> "sm:Disabled";
		};
	}

	public static GroupFeatureState fromActivityPubValue(String v, GroupFeatureState def){
		return switch(v){
			case "sm:Open" -> ENABLED_OPEN;
			case "sm:Restricted" -> ENABLED_RESTRICTED;
			case "sm:Closed" -> ENABLED_CLOSED;
			case "sm:Disabled" -> DISABLED;
			case null, default -> def;
		};
	}

	public String asApiValue(){
		return switch(this){
			case ENABLED_OPEN -> "open";
			case ENABLED_RESTRICTED -> "restricted";
			case ENABLED_CLOSED -> "closed";
			case DISABLED -> "disabled";
		};
	}
}
