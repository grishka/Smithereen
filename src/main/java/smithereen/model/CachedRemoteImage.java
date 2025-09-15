package smithereen.model;

import java.net.URI;

import smithereen.Config;
import smithereen.storage.ImgProxy;
import smithereen.storage.MediaCache;

public final class CachedRemoteImage extends RemoteImage{

	private Dimensions dimensions;
	public final String cacheKey;
	private float[] cropRegion;

	public CachedRemoteImage(MediaCache.PhotoItem item, URI originalURI){
		super(originalURI);
		dimensions=new Dimensions(item.width, item.height);
		cacheKey=item.key;
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
	public URI getUriForSizeAndFormat(Type size, Format format, boolean is2x, boolean useFallback){
		ImgProxy.UrlBuilder builder=new ImgProxy.UrlBuilder("local://"+Config.imgproxyLocalMediaCache+"/"+cacheKey+".webp")
				.resize(size.getMaxWidth(), size.getMaxHeight())
				.format(format)
				.quality(90);
		if(size.getResizingType()==ImgProxy.ResizingType.FILL){
			if(cropRegion!=null){
				int x=Math.round(cropRegion[0]*dimensions.width);
				int y=Math.round(cropRegion[1]*dimensions.height);
				builder.crop(x, y, Math.round(cropRegion[2]*dimensions.width-x), Math.round(cropRegion[3]*dimensions.height-y));
			}else if(dimensions.width!=dimensions.height){
				if(dimensions.width>dimensions.height){
					int s=dimensions.height;
					builder.crop((dimensions.width-s)/2, 0, s, s);
				}else{
					int s=dimensions.width;
					builder.crop(0, 0, s, s);
				}
			}
		}
		return builder.build();
	}

	@Override
	public Dimensions getOriginalDimensions(){
		return dimensions;
	}
}
