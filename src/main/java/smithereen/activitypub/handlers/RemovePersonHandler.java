package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Add;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.data.ForeignUser;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.BadRequestException;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.UserStorage;

public class RemovePersonHandler extends ActivityTypeHandler<ForeignUser, Remove, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Remove activity, User object) throws SQLException{
		if(activity.target==null || activity.target.link==null)
			throw new BadRequestException("activity.target is required and must be a URI");
		URI target=activity.target.link;
		if(target.equals(actor.getFriendsURL())){
			if(object instanceof ForeignUser && object.id==0)
				return;

			NewsfeedStorage.deleteEntry(actor.id, object.id, NewsfeedEntry.Type.ADD_FRIEND);
			UserStorage.unfriendUser(actor.id, object.id);
		}else{
			LOG.warn("Unknown Remove{Person} target {}", target);
		}
	}
}
