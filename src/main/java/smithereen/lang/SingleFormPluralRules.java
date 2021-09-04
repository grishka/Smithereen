package smithereen.lang;

public class SingleFormPluralRules extends PluralRules{

	@Override
	public int getIndexForQuantity(int quantity){
		return 0;
	}

	@Override
	public String getName(){
		return "single";
	}
}
