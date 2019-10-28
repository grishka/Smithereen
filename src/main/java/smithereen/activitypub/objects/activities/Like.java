package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Like extends Activity{
	@Override
	public String getType(){
		return "Like";
	}
}
