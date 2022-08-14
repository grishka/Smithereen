package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Add;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.GroupMembership;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.BadRequestException;
import smithereen.storage.GroupStorage;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.UserStorage;
import smithereen.util.BackgroundTaskRunner;

public class AddGroupHandler extends ActivityTypeHandler<ForeignUser, Add, Group>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Add activity, Group object) throws SQLException{
		if(activity.target==null || activity.target.link==null)
			throw new BadRequestException("activity.target is required and must be a URI");
		URI target=activity.target.link;
		if(target.equals(actor.getGroupsURL())){
			if(object instanceof ForeignGroup foreignGroup && object.id==0)
				GroupStorage.putOrUpdateForeignGroup(foreignGroup);

			if(object.accessType!=Group.AccessType.PRIVATE)
				context.appContext.getNewsfeedController().putFriendsFeedEntry(actor, object.id, object.isEvent() ? NewsfeedEntry.Type.JOIN_EVENT : NewsfeedEntry.Type.JOIN_GROUP);

			if(object instanceof ForeignGroup foreignGroup){
				// Verify that the group in question does indeed have this user as its member
				BackgroundTaskRunner.getInstance().submit(()->{
					boolean tentative=foreignGroup.isEvent() && foreignGroup.tentativeMembers!=null && activity.tentative;
					Group.MembershipState state=context.appContext.getGroupsController().getUserMembershipState(foreignGroup, actor);
					if((state==Group.MembershipState.TENTATIVE_MEMBER && tentative) || (state==Group.MembershipState.MEMBER && !tentative))
						return;
					try{
						context.appContext.getObjectLinkResolver().ensureObjectIsInCollection(object, tentative ? foreignGroup.tentativeMembers : foreignGroup.getMembersCollection(), actor.activityPubID);
						context.appContext.getGroupsController().joinGroup(foreignGroup, actor, tentative, true);
					}catch(Exception x){
						LOG.warn("Error verifying that object {} belongs to collection {} or saving membership", object.activityPubID, target, x);
					}
				});
			}

		}else{
			LOG.warn("Unknown Add{Group} target {}", target);
		}
	}
}
