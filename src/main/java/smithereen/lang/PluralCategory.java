package smithereen.lang;

public enum PluralCategory{
	ZERO("zero"),
	ONE("one"),
	TWO("two"),
	FEW("few"),
	MANY("many"),
	OTHER("other");

	public final String value;

	PluralCategory(String value){
		this.value=value;
	}

	public static PluralCategory fromString(String s){
		return switch(s){
			case "zero" -> ZERO;
			case "one" -> ONE;
			case "two" -> TWO;
			case "few" -> FEW;
			case "many" -> MANY;
			case "other" -> OTHER;
			default -> throw new IllegalArgumentException("invalid value "+s);
		};
	}
}
