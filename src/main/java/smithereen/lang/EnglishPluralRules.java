package smithereen.lang;

public class EnglishPluralRules extends PluralRules{
	@Override
	public PluralCategory getCategoryForQuantity(int quantity){
		return quantity==1 ? PluralCategory.ONE : PluralCategory.OTHER;
	}

	@Override
	public String getName(){
		return "english";
	}
}
