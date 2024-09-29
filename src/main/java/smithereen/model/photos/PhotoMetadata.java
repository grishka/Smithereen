package smithereen.model.photos;

import com.google.gson.annotations.SerializedName;

import java.net.URI;

public class PhotoMetadata{
	@SerializedName("apUrl")
	public URI apURL;
	public URI apReplies;
	public float[] avaCropRect;
	public Rotation rotation;
	public int width, height;
	public String blurhash;

	public enum Rotation{
		@SerializedName("90")
		CW_90,
		@SerializedName("180")
		CW_180,
		@SerializedName("270")
		CW_270
	}
}
