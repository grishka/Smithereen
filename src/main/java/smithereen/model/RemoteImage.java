package smithereen.model;

import java.net.URI;

public abstract sealed class RemoteImage implements SizedImage permits CachedRemoteImage, NonCachedRemoteImage{
	public final URI originalURI;
	public URI photoActivityPubID;
	public String blurHash;

	protected RemoteImage(URI originalURI){
		this.originalURI=originalURI;
	}

	@Override
	public URI getOriginalURI(){
		return originalURI;
	}

	@Override
	public String getBlurHash(){
		return blurHash;
	}
}
