package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.Actor;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.util.NoResultCallable;

public class FetchActorRelationshipCollectionsTask extends NoResultCallable{
	private static final Logger LOG=LoggerFactory.getLogger(FetchActorRelationshipCollectionsTask.class);

	private final Actor actor;
	private final ActivityPubWorker apw;
	private final ApplicationContext context;
	private final HashSet<URI> fetchingRelationshipCollectionsActors;

	public FetchActorRelationshipCollectionsTask(ActivityPubWorker apw, ApplicationContext context, HashSet<URI> fetchingRelationshipCollectionsActors, Actor actor){
		this.actor=actor;
		this.apw=apw;
		this.context=context;
		this.fetchingRelationshipCollectionsActors=fetchingRelationshipCollectionsActors;
	}

	@Override
	protected void compute(){
		List<Callable<Void>> tasks=new ArrayList<>();
		// TODO also sync removed items
		if(actor instanceof ForeignUser user){
			if(user.getFriendsURL()!=null){
				tasks.add(new FetchUserFriendsDirectlyTask(apw, context, user));
				if(user.getGroupsURL()!=null){
					tasks.add(new FetchUserGroupsDirectlyTask(apw, context, user));
				}
			}else{
				tasks.add(new FetchUserFriendsAndGroupsViaFollowsTask(apw, context, user));
			}
		}else if(actor instanceof ForeignGroup group){
			tasks.add(new FetchGroupMembersTask(apw, context, group, false));
			if(group.isEvent() && group.tentativeMembers!=null)
				tasks.add(new FetchGroupMembersTask(apw, context, group, true));
		}
		try{
			apw.invokeAll(tasks);
		}catch(Throwable x){
			LOG.warn("Error fetching relationship collections for {}", actor.activityPubID, x);
		}
		synchronized(apw){
			fetchingRelationshipCollectionsActors.remove(actor.activityPubID);
		}
		LOG.debug("Done fetching relationship collections for {}", actor.activityPubID);
	}
}
