package smithereen.model.attachments;

import org.jetbrains.annotations.Nullable;

import java.net.URI;

public non-sealed class AudioAttachment extends Attachment{
	public URI url;

	@Nullable
	public String artist;

	@Nullable
	public String title;

	/**
	 * Duration in milliseconds.
	 */
	public long duration;

	@Override
	public Type getType(){
		return Type.AUDIO;
	}
}
