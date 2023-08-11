package smithereen.util;

import org.jsoup.safety.Safelist;

public class Whitelist extends Safelist{
	public Whitelist(){
		super();
	}

	public Whitelist(Safelist copy){
		super(copy);
	}
}
