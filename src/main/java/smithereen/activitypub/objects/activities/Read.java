package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Read extends Activity{
	@Override
	public String getType(){
		return "Read";
	}
}
