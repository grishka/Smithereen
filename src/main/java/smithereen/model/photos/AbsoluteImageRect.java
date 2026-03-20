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

	public int getCenterX(){
		return x1+(x2-x1)/2;
	}

	public int getCenterY(){
		return y1+(y2-y1)/2;
	}

	public AbsoluteImageRect withX(int x1, int x2){
		return new AbsoluteImageRect(x1, y1, x2, y2, fullWidth, fullHeight);
	}

	public AbsoluteImageRect withY(int y1, int y2){
		return new AbsoluteImageRect(x1, y1, x2, y2, fullWidth, fullHeight);
	}

	public AbsoluteImageRect offset(int dx, int dy){
		return new AbsoluteImageRect(x1+dx, y1+dy, x2+dx, y2+dy, fullWidth, fullHeight);
	}

	public boolean fitsInsideFullSize(){
		return x1>=0 && y1>=0 && x2<fullWidth && y2<fullHeight;
	}
}
