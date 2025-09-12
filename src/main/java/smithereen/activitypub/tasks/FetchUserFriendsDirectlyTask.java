package smithereen.activitypub.tasks;

import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.model.ForeignUser;

/**
 * Fetches the user's friends collection directly, assuming there is one
 */
public class FetchUserFriendsDirectlyTask extends ForwardPaginatingCollectionTask{
	private final ForeignUser user;
	private final ActivityPubWorker apw;

	public FetchUserFriendsDirectlyTask(ActivityPubWorker apw, ApplicationContext context, ForeignUser user){
		super(context, Objects.requireNonNull(user.getFriendsURL(), "user must have a friends collection"));
		this.user=user;
		maxItems=ActivityPubWorker.MAX_FRIENDS;
		this.apw=apw;
	}

	protected void doOneCollectionPage(ActivityPubCollection page){
		apw.invokeAll(page.items.stream()
				.filter(lo->lo.link!=null && !Config.isLocal(lo.link))
				.map(lo->new FetchAndStoreOneUserFolloweeTask(context, user, lo.link, ForeignUser.class))
				.toList());
	}
}
