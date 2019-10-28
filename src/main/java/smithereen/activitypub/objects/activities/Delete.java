package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Delete extends Activity{
	@Override
	public String getType(){
		return "Delete";
	}
}
