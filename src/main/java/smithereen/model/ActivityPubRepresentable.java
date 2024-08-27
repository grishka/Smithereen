package smithereen.model;

import java.net.URI;

public interface ActivityPubRepresentable{
	URI getActivityPubID();

	default URI getActivityPubURL(){
		return getActivityPubID();
	}
}
