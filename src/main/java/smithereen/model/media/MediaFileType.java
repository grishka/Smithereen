package smithereen.model.media;

public enum MediaFileType{
	IMAGE_PHOTO,
	IMAGE_AVATAR,
	IMAGE_GRAFFITI,
	IMAGE_GROUP_LINK_THUMB;

	public String getFileExtension(){
		return "webp";
	}

	public String getMimeType(){
		return "image/webp";
	}
}
