package smithereen.activitypub.objects;

import smithereen.ApplicationContext;
import smithereen.model.photos.Photo;

public class LocalActivityPubPhoto extends ActivityPubPhoto{
	public final Photo nativePhoto;

	public LocalActivityPubPhoto(Photo nativePhoto){
		this.nativePhoto=nativePhoto;
	}

	@Override
	public Photo asNativePhoto(ApplicationContext context){
		return nativePhoto;
	}
}
