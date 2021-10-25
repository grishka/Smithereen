package smithereen.lang;

public abstract class PluralRules{
	public abstract PluralCategory getCategoryForQuantity(int quantity);

	/**
	 * Must match a key in langPluralRules in JS (Helpers.ts)
	 * @return
	 */
	public abstract String getName();
}
