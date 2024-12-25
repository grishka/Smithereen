package smithereen.model.photos;

public class TaggedPhotosPseudoAlbum extends PhotoAlbum{
	@Override
	public String getURL(){
		return "/users/"+ownerID+"/tagged";
	}
}
