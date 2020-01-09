package smithereen.lang;

public class RussianPluralRules extends PluralRules{
	@Override
	public int getIndexForQuantity(int quantity){
		if((quantity/10)%10==1)
			return 2;
		int r=quantity%10;
		if(r==1) // 1 хрень
			return 0;
		if(r>1 && r<5) // 3 хрени
			return 1;
		return 2; // 6 хреней
	}
}
