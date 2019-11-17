package smithereen.activitypub.objects;

public class Mention extends ActivityPubLink{
	@Override
	public String getType(){
		return "Mention";
	}
}
