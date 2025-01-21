package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.Actor;
import smithereen.exceptions.FederationException;
import smithereen.model.ForeignUser;
import smithereen.util.NoResultCallable;

/**
 * Fetches the user's friends and groups by intersecting followers and following
 */
public class FetchUserFriendsAndGroupsViaFollowsTask extends NoResultCallable{
	private static final Logger LOG=LoggerFactory.getLogger(FetchUserFriendsAndGroupsViaFollowsTask.class);

	private final ForeignUser user;
	private final ActivityPubWorker apw;
	private final ApplicationContext context;

	public FetchUserFriendsAndGroupsViaFollowsTask(ActivityPubWorker apw, ApplicationContext context, ForeignUser user){
		this.user=user;
		this.apw=apw;
		this.context=context;
	}

	@Override
	protected void compute() throws Exception{
		if(user.followers==null || user.following==null)
			throw new FederationException("The user must have followers and following collections");

		ActivityPubCollection followers=context.getObjectLinkResolver().resolve(user.followers, ActivityPubCollection.class, true, false, false);
		ActivityPubCollection following=context.getObjectLinkResolver().resolve(user.following, ActivityPubCollection.class, true, false, false);
		LOG.trace("Fetch followers/following: collection sizes: {} followers, {} following", followers.totalItems, following.totalItems);

		if(followers.totalItems<=0 || following.totalItems<=0){
			LOG.debug("Can't proceed because collection sizes are not known");
			return;
		}
		if(followers.totalItems>ActivityPubWorker.MAX_FRIENDS && following.totalItems>ActivityPubWorker.MAX_FRIENDS){
			LOG.debug("Can't proceed because both followers and following exceed the limit of {}", ActivityPubWorker.MAX_FRIENDS);
			return;
		}

		Set<URI> first=new HashSet<>();
		apw.submitTask(new FetchCollectionIntoSetTask(context, followers.totalItems>following.totalItems ? following : followers, first, ActivityPubWorker.MAX_FRIENDS)).get();
		Set<URI> mutualFollows=new HashSet<>();
		apw.submitTask(new FilterCollectionAgainstSetTask(context, followers.totalItems>following.totalItems ? followers : following, first, mutualFollows)).get();
		List<Callable<?>> tasks=new ArrayList<>();
		for(URI uri: mutualFollows){
			if(!Config.isLocal(uri)){
				tasks.add(new FetchAndStoreOneUserFolloweeTask(context, user, uri, Actor.class));
			}
			if(tasks.size()==ActivityPubWorker.ACTORS_BATCH_SIZE){
				apw.invokeAll(tasks);
				tasks.clear();
			}
		}
		if(!tasks.isEmpty())
			apw.invokeAll(tasks);
	}
}
