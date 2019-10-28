package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Follow extends Activity{
	@Override
	public String getType(){
		return "Follow";
	}
}
