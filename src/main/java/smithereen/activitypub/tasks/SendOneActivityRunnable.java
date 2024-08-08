package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityDeliveryRetry;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.Activity;
import smithereen.activitypub.objects.Actor;
import smithereen.exceptions.FederationException;
import smithereen.model.Server;

public class SendOneActivityRunnable implements Runnable{
	private static final Logger LOG=LoggerFactory.getLogger(SendOneActivityRunnable.class);

	private final ActivityPubWorker apw;
	private final ApplicationContext context;
	private final Activity activity;
	private final URI destination;
	private final Actor actor;
	private int retryAttempt;
	private EnumSet<Server.Feature> requiredServerFeatures;

	public SendOneActivityRunnable(ActivityPubWorker apw, ApplicationContext context, Activity activity, URI destination, Actor actor){
		this.activity=activity;
		this.destination=destination;
		this.actor=actor;
		this.apw=apw;
		this.context=context;
	}

	public SendOneActivityRunnable(ActivityPubWorker apw, ApplicationContext context, Activity activity, URI destination, Actor actor, int retryAttempt){
		this(apw, context, activity, destination, actor);
		this.retryAttempt=retryAttempt;
	}

	public SendOneActivityRunnable requireFeature(Server.Feature feature){
		if(requiredServerFeatures==null)
			requiredServerFeatures=EnumSet.of(feature);
		else
			requiredServerFeatures.add(feature);
		return this;
	}

	public SendOneActivityRunnable requireFeatures(Server.Feature... features){
		if(requiredServerFeatures==null)
			requiredServerFeatures=EnumSet.copyOf(Arrays.asList(features));
		else
			requiredServerFeatures.addAll(Arrays.asList(features));
		return this;
	}

	@Override
	public void run(){
		try{
			ActivityPub.postActivity(destination, activity, actor, context, retryAttempt>0, requiredServerFeatures);
		}catch(Exception x){
			LOG.error("Exception while sending activity", x);
			if(!(x instanceof FederationException)){
				ActivityDeliveryRetry retry=new ActivityDeliveryRetry(activity, destination, actor, retryAttempt+1);
				apw.scheduleRetry(retry);
			}
		}
	}
}
