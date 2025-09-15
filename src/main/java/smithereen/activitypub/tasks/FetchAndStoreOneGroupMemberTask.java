package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

import smithereen.ApplicationContext;
import smithereen.Utils;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.model.ForeignGroup;
import smithereen.model.ForeignUser;
import smithereen.util.NoResultCallable;

public class FetchAndStoreOneGroupMemberTask extends NoResultCallable{
	private static final Logger LOG=LoggerFactory.getLogger(FetchAndStoreOneGroupMemberTask.class);

	private final ForeignGroup group;
	private final URI userID;
	private final boolean tentative;
	private final ApplicationContext context;

	public FetchAndStoreOneGroupMemberTask(ApplicationContext context, ForeignGroup group, URI userID, boolean tentative){
		this.group=group;
		this.userID=userID;
		this.tentative=tentative;
		this.context=context;
	}

	@Override
	protected void compute(){
		try{
			ForeignUser user=context.getObjectLinkResolver().resolve(userID, ForeignUser.class, true, false, false);
			if(user.getGroupsURL()!=null && !Utils.uriHostMatches(group.activityPubID, user.getGroupsURL()))
				context.getObjectLinkResolver().ensureObjectIsInCollection(user, user.getGroupsURL(), group.activityPubID);
			if(user.id==0)
				context.getObjectLinkResolver().storeOrUpdateRemoteObject(user, user);
			context.getGroupsController().joinGroup(group, user, tentative, true);
		}catch(Exception x){
			LOG.debug("Error fetching remote user {}", userID, x);
		}
	}
}
