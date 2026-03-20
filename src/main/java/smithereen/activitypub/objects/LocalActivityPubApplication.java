package smithereen.activitypub.objects;

import smithereen.ApplicationContext;
import smithereen.model.apps.ClientApp;

public class LocalActivityPubApplication extends ActivityPubApplication{
	public final ClientApp nativeApp;

	public LocalActivityPubApplication(ClientApp nativeApp){
		this.nativeApp=nativeApp;
	}

	@Override
	public ClientApp asNativeApp(ApplicationContext ctx){
		return nativeApp;
	}
}
