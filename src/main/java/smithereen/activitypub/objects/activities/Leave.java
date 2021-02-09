package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Leave extends Activity{
	@Override
	public String getType(){
		return "Leave";
	}
}
