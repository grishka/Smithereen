package smithereen.model.media;

public class UserDataArchiveMetadata implements MediaFileMetadata{
	@Override
	public int width(){
		return 0;
	}

	@Override
	public int height(){
		return 0;
	}

	@Override
	public String blurhash(){
		return "";
	}

	@Override
	public float[] cropRegion(){
		return null;
	}

	@Override
	public int duration(){
		return 0;
	}
}
