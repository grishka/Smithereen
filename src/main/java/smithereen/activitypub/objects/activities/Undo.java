package smithereen.activitypub.objects.activities;

import smithereen.activitypub.objects.Activity;

public class Undo extends Activity{
	@Override
	public String getType(){
		return "Undo";
	}
}
