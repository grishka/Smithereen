package smithereen.lang;

public class SlavicPluralRules extends PluralRules{
	@Override
	public PluralCategory getCategoryForQuantity(int quantity){
		if((quantity/10)%10==1)
			return PluralCategory.MANY;
		int r=quantity%10;
		if(r==1) // 1 хрень
			return PluralCategory.ONE;
		if(r>1 && r<5) // 3 хрени
			return PluralCategory.FEW;
		return PluralCategory.MANY; // 6 хреней
	}

	@Override
	public String getName(){
		return "slavic";
	}
}
