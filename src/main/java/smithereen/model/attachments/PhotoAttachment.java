package smithereen.model.attachments;

import org.jetbrains.annotations.Nullable;

import smithereen.model.BlurHashable;
import smithereen.model.SizedImage;

public non-sealed class PhotoAttachment extends Attachment implements SizedAttachment, BlurHashable{
	public SizedImage image;
	private String blurHash;
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

	@Override
	@Nullable
	public String getBlurHash(){
		return blurHash;
	}

	@Override
	public void setBlurHash(@Nullable String blurHash){
		this.blurHash=blurHash;
	}
}
