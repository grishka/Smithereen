package smithereen.lang;

public abstract class PluralRules{
	public abstract int getIndexForQuantity(int quantity);

	/**
	 * Must match a key in langPluralRules in JS (Helpers.ts)
	 * @return
	 */
	public abstract String getName();
}
