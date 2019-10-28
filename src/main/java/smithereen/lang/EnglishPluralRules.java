package smithereen.lang;

public class EnglishPluralRules extends PluralRules{
	@Override
	public int getIndexForQuantity(int quantity){
		return quantity==1 ? 0 : 1;
	}
}
