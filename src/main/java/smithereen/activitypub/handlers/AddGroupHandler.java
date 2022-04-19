package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Add;
import smithereen.data.ForeignGroup;
import smithereen.data.ForeignUser;
import smithereen.data.Group;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.BadRequestException;
import smithereen.storage.GroupStorage;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.UserStorage;

public class AddGroupHandler extends ActivityTypeHandler<ForeignUser, Add, Group>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Add activity, Group object) throws SQLException{
		if(activity.target==null || activity.target.link==null)
			throw new BadRequestException("activity.target is required and must be a URI");
		URI target=activity.target.link;
		if(target.equals(actor.getGroupsURL())){
			if(object instanceof ForeignGroup && object.id==0)
				GroupStorage.putOrUpdateForeignGroup((ForeignGroup) object);

			// TODO verify that this user is actually a member of this group and store the membership
			// https://socialhub.activitypub.rocks/t/querying-activitypub-collections/1866

			if(object.accessType!=Group.AccessType.PRIVATE)
				NewsfeedStorage.putEntry(actor.id, object.id, object.isEvent() ? NewsfeedEntry.Type.JOIN_EVENT : NewsfeedEntry.Type.JOIN_GROUP, null);
		}else{
			LOG.warn("Unknown Add{Group} target {}", target);
		}
	}
}
