package smithereen.model.attachments;

import java.net.URI;

public non-sealed class AudioAttachment extends Attachment{
	public URI url;

	@Override
	public Type getType(){
		return Type.AUDIO;
	}
}
