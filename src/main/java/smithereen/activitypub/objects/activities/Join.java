package smithereen.activitypub.objects.activities;

public class Join extends Follow{
	@Override
	public String getType(){
		return "Join";
	}
}
