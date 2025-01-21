package smithereen.model.photos;

public record ImageRect(float x1, float y1, float x2, float y2){
	public AbsoluteImageRect makeAbsolute(int width, int height){
		return new AbsoluteImageRect(Math.round(width*x1), Math.round(height*y1), Math.round(width*x2), Math.round(height*y2), width, height);
	}

	public int getWidth(){
		return (int)(x2-x1);
	}

	public int getHeight(){
		return (int)(y2-y1);
	}

	// TODO rotation
}
