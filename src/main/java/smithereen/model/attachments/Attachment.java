package smithereen.model.attachments;

import org.jetbrains.annotations.Nullable;

import smithereen.util.TranslatableEnum;

public abstract sealed class Attachment permits AudioAttachment, PhotoAttachment, VideoAttachment{

	@Nullable
	public String description;

	public abstract Type getType();

	public enum Type implements TranslatableEnum<Type>{
		PHOTO,
		VIDEO,
		AUDIO;

		@Override
		public String getLangKey(){
			return switch(this){
				case PHOTO -> "content_type_photo";
				case VIDEO -> "content_type_video";
				case AUDIO -> "content_type_audio";
			};
		}
	}
}
