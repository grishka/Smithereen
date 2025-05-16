package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Add;
import smithereen.model.ForeignUser;
import smithereen.model.User;
import smithereen.model.feed.NewsfeedEntry;
import smithereen.exceptions.BadRequestException;
import smithereen.storage.UserStorage;
import smithereen.util.BackgroundTaskRunner;

public class PersonAddPersonHandler extends ActivityTypeHandler<ForeignUser, Add, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Add activity, User object) throws SQLException{
		if(activity.target==null || activity.target.link==null)
			throw new BadRequestException("activity.target is required and must be a URI");
		URI target=activity.target.link;
		if(target.equals(actor.getFriendsURL())){
			if(object instanceof ForeignUser && object.id==0)
				context.appContext.getObjectLinkResolver().storeOrUpdateRemoteObject(object, object);

			context.appContext.getNewsfeedController().putFriendsFeedEntry(actor, object.id, NewsfeedEntry.Type.ADD_FRIEND);

			if(object instanceof ForeignUser foreignUser && foreignUser.getFriendsURL()!=null){
				// Verify that the target user does indeed have the actor as their friend
				BackgroundTaskRunner.getInstance().submit(()->{
					try{
						context.appContext.getObjectLinkResolver().ensureObjectIsInCollection(foreignUser, foreignUser.getFriendsURL(), actor.activityPubID);
						context.appContext.getFriendsController().storeFriendship(actor, object, true);
					}catch(Exception x){
						LOG.warn("Error verifying that {} is a friend of {} or storing that relationship", actor.activityPubID, object.activityPubID);
					}
				});
			}
		}else{
			LOG.warn("Unknown Add{Person} target {}", target);
		}
	}
}
