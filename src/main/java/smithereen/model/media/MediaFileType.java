package smithereen.model.media;

public enum MediaFileType{
	IMAGE_PHOTO,
	IMAGE_AVATAR,
	IMAGE_GRAFFITI,
	IMAGE_GROUP_LINK_THUMB,
	USER_EXPORT_ARCHIVE;

	public String getFileExtension(){
		return switch(this){
			case USER_EXPORT_ARCHIVE -> "zip";
			default -> "webp";
		};
	}

	public String getMimeType(){
		return switch(this){
			case USER_EXPORT_ARCHIVE -> "application/zip";
			default -> "image/webp";
		};
	}
}
