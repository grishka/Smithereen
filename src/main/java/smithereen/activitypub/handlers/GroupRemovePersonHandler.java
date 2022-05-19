package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.BadRequestException;
import smithereen.storage.GroupStorage;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.UserStorage;

public class GroupRemovePersonHandler extends ActivityTypeHandler<ForeignGroup, Remove, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Remove activity, User object) throws SQLException{
		if(activity.target==null || activity.target.link==null)
			throw new BadRequestException("activity.target is required and must be a URI");
		URI target=activity.target.link;
		if(target.equals(actor.getMembersCollection()) || target.equals(actor.tentativeMembers)){
			if(object instanceof ForeignUser && object.id==0)
				return;

			GroupStorage.leaveGroup(actor, object.id, target.equals(actor.tentativeMembers), true);
		}else{
			LOG.warn("Unknown Remove{Person} target {}", target);
		}
	}
}
