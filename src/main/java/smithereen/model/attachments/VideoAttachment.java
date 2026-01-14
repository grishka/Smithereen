package smithereen.model.attachments;

import java.net.URI;

public non-sealed class VideoAttachment extends Attachment{
	public URI url;
	public int width, height;
	public String blurHash;

	@Override
	public Type getType(){
		return Type.VIDEO;
	}
}
