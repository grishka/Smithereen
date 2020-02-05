package smithereen.activitypub.objects;

public class Document extends ActivityPubObject{

	public String localID;

	@Override
	public String getType(){
		return "Document";
	}
}
