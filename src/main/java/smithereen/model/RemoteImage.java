package smithereen.model;

import java.net.URI;

public abstract sealed class RemoteImage implements SizedImage permits CachedRemoteImage, NonCachedRemoteImage{
	public final URI originalURI;
	public URI photoActivityPubID;

	protected RemoteImage(URI originalURI){
		this.originalURI=originalURI;
	}

	@Override
	public URI getOriginalURI(){
		return originalURI;
	}
}
