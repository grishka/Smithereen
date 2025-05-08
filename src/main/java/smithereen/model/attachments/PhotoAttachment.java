package smithereen.model.attachments;

import smithereen.model.SizedImage;

public class PhotoAttachment extends Attachment implements SizedAttachment{
	public SizedImage image;
	public String blurHash;
	public long photoID;

	@Override
	public int getWidth(){
		return image.getOriginalDimensions().width;
	}

	@Override
	public int getHeight(){
		return image.getOriginalDimensions().height;
	}

	@Override
	public boolean isSizeKnown(){
		return image.getOriginalDimensions()!=SizedImage.Dimensions.UNKNOWN;
	}

	@Override
	public Type getType(){
		return Type.PHOTO;
	}
}
