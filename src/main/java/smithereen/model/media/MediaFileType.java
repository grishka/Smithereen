package smithereen.model.media;

public enum MediaFileType{
	IMAGE_PHOTO,
	IMAGE_AVATAR,
	IMAGE_GRAFFITI;

	public String getFileExtension(){
		return "webp";
	}

	public String getMimeType(){
		return "image/webp";
	}
}
