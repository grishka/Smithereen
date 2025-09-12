package smithereen.activitypub.tasks;

import java.sql.SQLException;

import smithereen.ApplicationContext;
import smithereen.Config;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.exceptions.InternalServerErrorException;
import smithereen.model.ForeignGroup;
import smithereen.storage.GroupStorage;

public class FetchGroupMembersTask extends ForwardPaginatingCollectionTask{
	private final ForeignGroup group;
	private final boolean tentative;
	private final ActivityPubWorker apw;

	public FetchGroupMembersTask(ActivityPubWorker apw, ApplicationContext context, ForeignGroup group, boolean tentative){
		super(context, tentative ? group.tentativeMembers : group.members);
		this.group=group;
		this.tentative=tentative;
		this.apw=apw;
	}

	@Override
	protected void doOneCollectionPage(ActivityPubCollection page){
		apw.invokeAll(page.items.stream()
				.filter(lo->lo.link!=null && !Config.isLocal(lo.link))
				.map(lo->new FetchAndStoreOneGroupMemberTask(context, group, lo.link, tentative))
				.toList());
	}

	@Override
	protected void onCollectionLoaded(){
		try{
			GroupStorage.setMemberCount(group, (int)totalItems, tentative);
		}catch(SQLException x){
			throw new InternalServerErrorException(x);
		}
	}
}
