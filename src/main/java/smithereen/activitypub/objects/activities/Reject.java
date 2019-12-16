package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Reject extends Activity{
	@Override
	public String getType(){
		return "Reject";
	}
}
