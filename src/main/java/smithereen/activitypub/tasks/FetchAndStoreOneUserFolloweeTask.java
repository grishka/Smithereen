package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.Actor;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.util.NoResultCallable;

public class FetchAndStoreOneUserFolloweeTask extends NoResultCallable{
	private static final Logger LOG=LoggerFactory.getLogger(FetchAndStoreOneUserFolloweeTask.class);

	private final ForeignUser user;
	private final URI targetActorID;
	private final Class<? extends Actor> type;
	private final ApplicationContext context;

	public FetchAndStoreOneUserFolloweeTask(ApplicationContext context, ForeignUser user, URI targetActorID, Class<? extends Actor> type){
		this.user=user;
		this.targetActorID=targetActorID;
		this.type=type;
		this.context=context;
	}

	@Override
	protected void compute(){
		try{
			Actor target=context.getObjectLinkResolver().resolve(targetActorID, type, true, false, false);
			if(target instanceof ForeignUser targetUser){
				if(targetUser.getFriendsURL()!=null)
					context.getObjectLinkResolver().ensureObjectIsInCollection(targetUser, targetUser.getFriendsURL(), user.activityPubID);
				if(targetUser.id==0)
					context.getObjectLinkResolver().storeOrUpdateRemoteObject(targetUser, targetUser);
				context.getFriendsController().storeFriendship(user, targetUser);
			}else if(target instanceof ForeignGroup targetGroup){
				if(targetGroup.isEvent())
					return;
				if(targetGroup.members!=null)
					context.getObjectLinkResolver().ensureObjectIsInCollection(targetGroup, targetGroup.members, user.activityPubID);
				if(targetGroup.id==0)
					context.getObjectLinkResolver().storeOrUpdateRemoteObject(targetGroup, targetGroup);
				context.getGroupsController().joinGroup(targetGroup, user, false, true);
			}
		}catch(Exception x){
			LOG.debug("Error fetching remote actor {}", targetActorID, x);
		}
	}
}
