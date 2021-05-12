package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Add extends Activity{
	@Override
	public String getType(){
		return "Add";
	}
}
