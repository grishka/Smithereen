package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.BadRequestException;
import smithereen.storage.NewsfeedStorage;

public class RemoveGroupHandler extends ActivityTypeHandler<ForeignUser, Remove, Group>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Remove activity, Group object) throws SQLException{
		if(activity.target==null || activity.target.link==null)
			throw new BadRequestException("activity.target is required and must be a URI");
		URI target=activity.target.link;
		if(target.equals(actor.getFriendsURL())){
			NewsfeedStorage.deleteEntry(actor.id, object.id, NewsfeedEntry.Type.JOIN_GROUP);
		}else{
			System.out.println("Unknown Remove{Group} target "+target);
		}
	}
}
