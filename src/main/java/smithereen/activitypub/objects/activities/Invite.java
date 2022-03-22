package smithereen.activitypub.objects.activities;

public class Invite extends Offer{
	@Override
	public String getType(){
		return "Invite";
	}
}
