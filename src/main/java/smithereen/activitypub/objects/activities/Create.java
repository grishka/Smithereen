package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Create extends Activity{
	@Override
	public String getType(){
		return "Create";
	}
}
