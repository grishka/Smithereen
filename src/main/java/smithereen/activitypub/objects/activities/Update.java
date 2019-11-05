package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Update extends Activity{
	@Override
	public String getType(){
		return "Update";
	}
}
