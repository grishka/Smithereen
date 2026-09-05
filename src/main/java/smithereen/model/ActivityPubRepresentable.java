package smithereen.model;

import java.net.URI;

import org.jetbrains.annotations.NotNull;

import smithereen.util.UriRenderer;

public interface ActivityPubRepresentable{
	URI getActivityPubID();

	default URI getActivityPubURL(){
		return getActivityPubID();
	}

	@NotNull
	default String getHumanReadableActivityPubURL(){
		return UriRenderer.DEFAULT.render(getActivityPubURL());
	}

	@NotNull
	default String getHumanReadableDomain(){
		return UriRenderer.HOST_ONLY.render(getActivityPubURL());
	}
}
