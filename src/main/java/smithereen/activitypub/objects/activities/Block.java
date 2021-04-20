package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Block extends Activity{
	@Override
	public String getType(){
		return "Block";
	}
}
