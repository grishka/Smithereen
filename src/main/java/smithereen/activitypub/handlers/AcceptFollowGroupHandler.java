package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.NestedActivityTypeHandler;
import smithereen.activitypub.objects.activities.Accept;
import smithereen.activitypub.objects.activities.Follow;
import smithereen.data.ForeignGroup;
import smithereen.data.Group;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.storage.GroupStorage;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.UserStorage;

public class AcceptFollowGroupHandler extends NestedActivityTypeHandler<ForeignGroup, Accept, Follow, ForeignGroup>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Accept activity, Follow nested, ForeignGroup object) throws SQLException{
		User follower=UserStorage.getUserByActivityPubID(nested.actor.link);
		if(follower==null)
			throw new ObjectNotFoundException("Follower not found");
		follower.ensureLocal();
		GroupStorage.setMemberAccepted(actor, follower.id, true);
		if(object.accessType!=Group.AccessType.PRIVATE){
			context.appContext.getActivityPubWorker().sendAddToGroupsCollectionActivity(follower, actor, context.appContext.getGroupsController().getUserMembershipState(object, follower)==Group.MembershipState.TENTATIVE_MEMBER);
			NewsfeedStorage.putEntry(follower.id, object.id, object.isEvent() ? NewsfeedEntry.Type.JOIN_EVENT : NewsfeedEntry.Type.JOIN_GROUP, null);
		}
	}
}
