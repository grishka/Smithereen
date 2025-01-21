package smithereen.activitypub.tasks;

import java.util.Objects;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.CollectionPage;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;

/**
 * Fetches the user's friends collection directly, assuming there is one
 */
public class FetchUserGroupsDirectlyTask extends ForwardPaginatingCollectionTask{
	private final ForeignUser user;
	private final ActivityPubWorker apw;

	public FetchUserGroupsDirectlyTask(ActivityPubWorker apw, ApplicationContext context, ForeignUser user){
		super(context, Objects.requireNonNull(user.getGroupsURL(), "user must have a groups collection"));
		this.user=user;
		maxItems=ActivityPubWorker.MAX_FRIENDS;
		this.apw=apw;
	}

	protected void doOneCollectionPage(CollectionPage page){
		apw.invokeAll(page.items.stream()
				.filter(lo->lo.link!=null && !Config.isLocal(lo.link))
				.map(lo->new FetchAndStoreOneUserFolloweeTask(context, user, lo.link, ForeignGroup.class))
				.toList());
	}
}
