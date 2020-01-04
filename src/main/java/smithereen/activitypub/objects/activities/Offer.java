package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Offer extends Activity{
	@Override
	public String getType(){
		return "Offer";
	}
}
