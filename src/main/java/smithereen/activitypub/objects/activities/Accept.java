package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Accept extends Activity{
	@Override
	public String getType(){
		return "Accept";
	}
}
