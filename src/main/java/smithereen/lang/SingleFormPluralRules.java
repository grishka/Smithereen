package smithereen.lang;

public class SingleFormPluralRules extends PluralRules{

	@Override
	public PluralCategory getCategoryForQuantity(int quantity){
		return PluralCategory.OTHER;
	}

	@Override
	public String getName(){
		return "single";
	}
}
