package smithereen.data;

import java.net.URI;

import smithereen.Config;
import smithereen.storage.ImgProxy;
import smithereen.storage.MediaCache;

public class CachedRemoteImage implements SizedImage{

	private Dimensions dimensions;
	private String cacheKey;
	private float[] cropRegion;

	public CachedRemoteImage(MediaCache.PhotoItem item){
		dimensions=new Dimensions(item.width, item.height);
		cacheKey=item.key;
	}

	public CachedRemoteImage(MediaCache.PhotoItem item, float[] cropRegion){
		this(item);
		setCropRegion(cropRegion);
	}

	public void setCropRegion(float[] cropRegion){
		if(cropRegion!=null && cropRegion.length!=4)
			throw new IllegalArgumentException("Crop region must be 4 floats");
		this.cropRegion=cropRegion;
	}

	@Override
	public URI getUriForSizeAndFormat(Type size, Format format){
		ImgProxy.UrlBuilder builder=new ImgProxy.UrlBuilder("local://"+Config.mediaCacheURLPath+"/"+cacheKey+".webp")
				.resize(size.getMaxWidth(), size.getMaxHeight())
				.format(format)
				.quality(90);
		if(cropRegion!=null && size.getResizingType()==ImgProxy.ResizingType.FILL){
			int x=Math.round(cropRegion[0]*dimensions.width);
			int y=Math.round(cropRegion[1]*dimensions.height);
			builder.crop(x, y, Math.round(cropRegion[2]*dimensions.width-x), Math.round(cropRegion[3]*dimensions.height-y));
		}
		return builder.build();
	}

	@Override
	public Dimensions getOriginalDimensions(){
		return dimensions;
	}
}
