package smithereen.api.model;

import smithereen.model.photos.ImageRect;

public record ApiImageRect(float x1, float y1, float x2, float y2){
	public ApiImageRect(ImageRect r){
		this(r.x1(), r.y1(), r.x2(), r.y2());
	}
}
