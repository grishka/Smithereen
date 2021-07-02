package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Add;
import smithereen.data.ForeignUser;
import smithereen.data.User;
import smithereen.data.feed.NewsfeedEntry;
import smithereen.exceptions.BadRequestException;
import smithereen.storage.NewsfeedStorage;
import smithereen.storage.UserStorage;

public class AddPersonHandler extends ActivityTypeHandler<ForeignUser, Add, User>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Add activity, User object) throws SQLException{
		if(activity.target==null || activity.target.link==null)
			throw new BadRequestException("activity.target is required and must be a URI");
		URI target=activity.target.link;
		if(target.equals(actor.getFriendsURL())){
			if(object instanceof ForeignUser && object.id==0)
				UserStorage.putOrUpdateForeignUser((ForeignUser) object);

			// TODO verify that friends collections of both users contain each other and store a mutual follow
			// https://socialhub.activitypub.rocks/t/querying-activitypub-collections/1866

			NewsfeedStorage.putEntry(actor.id, object.id, NewsfeedEntry.Type.ADD_FRIEND, null);
		}else{
			System.out.println("Unknown Add{Person} target "+target);
		}
	}
}
