package smithereen.activitypub.objects;

import java.net.URI;

import smithereen.BuildInfo;
import smithereen.Config;

public class ServiceActor extends Actor{

	private static ServiceActor instance;

	public static ServiceActor getInstance(){
		if(instance==null){
			instance=new ServiceActor();
		}
		return instance;
	}

	private ServiceActor(){
		privateKey=Config.serviceActorPrivateKey;
		publicKey=Config.serviceActorPublicKey;
		username="activitypub_service_actor";
		name="Smithereen "+BuildInfo.VERSION+" at "+Config.domain;
		url=Config.localURI("/system/about");
		activityPubID=Config.localURI("/activitypub/serviceActor");
		sharedInbox=Config.localURI("/activitypub/sharedInbox");
		inbox=Config.localURI("/activitypub/serviceActor/inbox");
		outbox=Config.localURI("/activitypub/serviceActor/outbox");
	}

	@Override
	public String getType(){
		return "Application";
	}

	@Override
	public int getLocalID(){
		return 0;
	}

	@Override
	public URI getWallURL(){
		return null;
	}

	@Override
	public String getTypeAndIdForURL(){
		return null;
	}

	@Override
	public String getName(){
		return name;
	}

	@Override
	protected boolean canFollowOtherActors(){
		return false;
	}

	@Override
	protected boolean canBeFollowed(){
		return false;
	}
}
