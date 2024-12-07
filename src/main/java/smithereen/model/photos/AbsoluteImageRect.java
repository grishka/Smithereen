package smithereen.model.photos;

import smithereen.model.SizedImage;

public record AbsoluteImageRect(int x1, int y1, int x2, int y2, int fullWidth, int fullHeight){
	public int getWidth(){
		return x2-x1;
	}

	public int getHeight(){
		return y2-y1;
	}

	public ImageRect makeRelative(){
		return new ImageRect(x1/(float)fullWidth, y1/(float)fullHeight, x2/(float)fullWidth, y2/(float)fullHeight);
	}

	public AbsoluteImageRect rotate(SizedImage.Rotation rotation){
		return switch(rotation){
			case _0 -> this;
			case _90 -> new AbsoluteImageRect(fullHeight-y2, x1, fullHeight-y1, x2, fullHeight, fullWidth);
			case _180 -> new AbsoluteImageRect(fullWidth-x2, fullHeight-y2, fullWidth-x1, fullHeight-y1, fullWidth, fullHeight);
			case _270 -> new AbsoluteImageRect(y1, fullWidth-x2, y2, fullWidth-x1, fullHeight, fullWidth);
		};
	}
}
