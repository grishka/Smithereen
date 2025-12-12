package smithereen.api.model;

public record ApiCropPhoto(ApiPhoto photo, ApiImageRect crop, ApiImageRect squareCrop){
}
