package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityDeliveryRetry;
import smithereen.activitypub.ActivityPubWorker;

public class RetryActivityRunnable implements Runnable{
	private static final Logger LOG=LoggerFactory.getLogger(RetryActivityRunnable.class);

	private final ActivityDeliveryRetry retry;
	private final ActivityPubWorker apw;
	private final ApplicationContext context;

	public RetryActivityRunnable(ActivityPubWorker apw, ApplicationContext context, ActivityDeliveryRetry retry){
		this.retry=retry;
		this.apw=apw;
		this.context=context;
	}

	@Override
	public void run(){
		LOG.debug("Retrying activity delivery to {}, attempt {}", retry.inbox(), retry.attemptNumber());
		apw.submitTask(new SendOneActivityRunnable(apw, context, retry.activity(), retry.inbox(), retry.actor(), retry.attemptNumber()));
	}
}
