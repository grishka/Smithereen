package smithereen.model;

import java.net.URI;

import smithereen.Config;
import smithereen.storage.ImgProxy;
import smithereen.storage.MediaCache;

public class CachedRemoteImage implements SizedImage{

	private Dimensions dimensions;
	private String cacheKey;
	private float[] cropRegion;
	private URI originalURI;

	public CachedRemoteImage(MediaCache.PhotoItem item, URI originalURI){
		dimensions=new Dimensions(item.width, item.height);
		cacheKey=item.key;
		this.originalURI=originalURI;
	}

	public CachedRemoteImage(MediaCache.PhotoItem item, float[] cropRegion, URI originalURI){
		this(item, originalURI);
		setCropRegion(cropRegion);
	}

	public void setCropRegion(float[] cropRegion){
		if(cropRegion!=null && cropRegion.length!=4)
			throw new IllegalArgumentException("Crop region must be 4 floats");
		this.cropRegion=cropRegion;
	}

	@Override
	public URI getUriForSizeAndFormat(Type size, Format format){
		ImgProxy.UrlBuilder builder=new ImgProxy.UrlBuilder("local://"+Config.imgproxyLocalMediaCache+"/"+cacheKey+".webp")
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

	@Override
	public URI getOriginalURI(){
		return originalURI;
	}
}
