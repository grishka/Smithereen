package smithereen.model.photos;

import com.google.gson.annotations.SerializedName;

import java.net.URI;

import smithereen.model.SizedImage;

public class PhotoMetadata{
	@SerializedName("apUrl")
	public URI apURL;
	public URI apReplies;
	public float[] avaCropRect;
	public SizedImage.Rotation rotation;
	public int width, height;
	public String blurhash;
}
