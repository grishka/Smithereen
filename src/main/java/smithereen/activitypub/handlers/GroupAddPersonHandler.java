package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Add;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.BadRequestException;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.UserStorage;
import smithereen.util.BackgroundTaskRunner;

public class GroupAddPersonHandler extends ActivityTypeHandler<ForeignGroup, Add, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Add activity, User object) throws SQLException{
		if(activity.target==null || activity.target.link==null)
			throw new BadRequestException("activity.target is required and must be a URI");
		URI target=activity.target.link;
		if(target.equals(actor.getMembersCollection()) || target.equals(actor.tentativeMembers)){
			if(object instanceof ForeignUser && object.id==0)
				UserStorage.putOrUpdateForeignUser((ForeignUser) object);

			if(object instanceof ForeignUser foreignUser && foreignUser.getGroupsURL()!=null){
				BackgroundTaskRunner.getInstance().submit(()->{
					try{
						context.appContext.getObjectLinkResolver().ensureObjectIsInCollection(foreignUser, foreignUser.getGroupsURL(), actor.activityPubID);
						context.appContext.getGroupsController().joinGroup(actor, object, target.equals(actor.tentativeMembers), true);
					}catch(Exception x){
						LOG.warn("Error verifying that {} is a member of {} or storing that relationship", actor.activityPubID, object.activityPubID);
					}
				});
			}
		}else{
			LOG.warn("Unknown Add{Person} target {}", target);
		}
	}
}
