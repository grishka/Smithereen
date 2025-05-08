package smithereen.model.attachments;

import java.net.URI;

public class VideoAttachment extends Attachment{
	public URI url;

	@Override
	public Type getType(){
		return Type.VIDEO;
	}
}
