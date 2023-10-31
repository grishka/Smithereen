package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Move extends Activity{
	@Override
	public String getType(){
		return "Move";
	}
}
