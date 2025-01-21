package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.EnumSet;
import java.util.Set;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.objects.Actor;
import smithereen.model.Server;
import smithereen.model.User;

public class ForwardOneActivityRunnable implements Runnable{
	private static final Logger LOG=LoggerFactory.getLogger(ForwardOneActivityRunnable.class);

	private final String activity;
	private final URI destination;
	private final Actor signer;
	private final ApplicationContext context;
	private final EnumSet<Server.Feature> requiredServerFeatures;

	public ForwardOneActivityRunnable(ApplicationContext context, String activity, URI destination, Actor signer, EnumSet<Server.Feature> requiredServerFeatures){
		this.activity=activity;
		this.destination=destination;
		this.signer=signer;
		this.context=context;
		this.requiredServerFeatures=requiredServerFeatures;
	}

	@Override
	public void run(){
		try{
			ActivityPub.forwardActivity(destination, activity, signer, context, requiredServerFeatures);
		}catch(Exception x){
			LOG.error("Exception while forwarding activity", x);
		}
	}
}
