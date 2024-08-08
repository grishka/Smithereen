package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityDeliveryRetry;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.Activity;
import smithereen.exceptions.FederationException;
import smithereen.model.Server;
import smithereen.model.User;

public class SendActivitySequenceRunnable implements Runnable{
	private static final Logger LOG=LoggerFactory.getLogger(SendActivitySequenceRunnable.class);

	private final ActivityPubWorker apw;
	private final ApplicationContext context;
	private final List<Activity> activities;
	private final URI destination;
	private final User user;
	private EnumSet<Server.Feature> requiredServerFeatures;

	public SendActivitySequenceRunnable(ActivityPubWorker apw, ApplicationContext context, List<Activity> activities, URI destination, User user){
		this.activities=activities;
		this.destination=destination;
		this.user=user;
		this.apw=apw;
		this.context=context;
	}

	public SendActivitySequenceRunnable requireFeature(Server.Feature feature){
		if(requiredServerFeatures==null)
			requiredServerFeatures=EnumSet.of(feature);
		else
			requiredServerFeatures.add(feature);
		return this;
	}

	public SendActivitySequenceRunnable requireFeatures(Server.Feature... features){
		if(requiredServerFeatures==null)
			requiredServerFeatures=EnumSet.copyOf(Arrays.asList(features));
		else
			requiredServerFeatures.addAll(Arrays.asList(features));
		return this;
	}

	@Override
	public void run(){
		for(Activity activity: activities){
			try{
				ActivityPub.postActivity(destination, activity, user, context, false, requiredServerFeatures);
			}catch(Exception x){
				LOG.error("Exception while sending activity", x);
				if(!(x instanceof FederationException)){
					ActivityDeliveryRetry retry=new ActivityDeliveryRetry(activity, destination, user, 1);
					apw.scheduleRetry(retry);
				}
			}
		}
	}
}
