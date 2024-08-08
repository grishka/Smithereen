package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPub;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.model.User;

public class ForwardOneActivityRunnable implements Runnable{
	private static final Logger LOG=LoggerFactory.getLogger(ForwardOneActivityRunnable.class);

	private final String activity;
	private final URI destination;
	private final User user;
	private final ApplicationContext context;

	public ForwardOneActivityRunnable(ApplicationContext context, String activity, URI destination, User user){
		this.activity=activity;
		this.destination=destination;
		this.user=user;
		this.context=context;
	}

	@Override
	public void run(){
		try{
			ActivityPub.forwardActivity(destination, activity, user, context);
		}catch(Exception x){
			LOG.error("Exception while forwarding activity", x);
		}
	}
}
