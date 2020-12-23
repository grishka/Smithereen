package smithereen.data.attachments;

import smithereen.data.SizedImage;

public class PhotoAttachment extends Attachment implements SizedAttachment{
	public SizedImage image;

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
}
