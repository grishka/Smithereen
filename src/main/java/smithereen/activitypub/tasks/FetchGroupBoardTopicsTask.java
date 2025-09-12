package smithereen.activitypub.tasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityPubWorker;
import smithereen.activitypub.objects.ActivityPubBoardTopic;
import smithereen.activitypub.objects.ActivityPubCollection;
import smithereen.activitypub.objects.LinkOrObject;
import smithereen.model.ForeignGroup;
import smithereen.model.board.BoardTopic;

public class FetchGroupBoardTopicsTask extends ForwardPaginatingCollectionTask{
	private static final Logger LOG=LoggerFactory.getLogger(FetchGroupBoardTopicsTask.class);

	private final ActivityPubWorker apw;
	private final ApplicationContext context;
	private final ForeignGroup group;
	private final Set<Long> seenTopics=new HashSet<>();

	public FetchGroupBoardTopicsTask(ActivityPubWorker apw, ApplicationContext context, ForeignGroup group){
		super(context, group.getBoardTopicsURL());
		this.apw=apw;
		this.context=context;
		this.group=group;
	}

	@Override
	protected void doOneCollectionPage(ActivityPubCollection page){
		try{
			int i=0;
			for(LinkOrObject lo:page.items){
				if(lo.object instanceof ActivityPubBoardTopic topic){
					BoardTopic nativeTopic=topic.asNativeTopic(context);
					context.getObjectLinkResolver().storeOrUpdateRemoteObject(nativeTopic, topic);
					seenTopics.add(nativeTopic.id);
					apw.fetchBoardTopicComments(topic, nativeTopic).get();
					i++;
					if(i==maxItems)
						break;
				}
			}
		}catch(Exception x){
			LOG.warn("Error fetching actor photo albums", x);
		}
	}
}
