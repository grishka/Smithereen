package smithereen.data;

import smithereen.activitypub.objects.ActivityPubObject;

public class PollVote extends ActivityPubObject{
	@Override
	public String getType(){
		return "Note";
	}
}
