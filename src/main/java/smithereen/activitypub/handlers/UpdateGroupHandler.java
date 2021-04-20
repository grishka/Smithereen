package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.exceptions.BadRequestException;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Update;
import smithereen.data.ForeignGroup;
import smithereen.storage.GroupStorage;

public class UpdateGroupHandler extends ActivityTypeHandler<ForeignGroup, Update, ForeignGroup>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Update activity, ForeignGroup object) throws SQLException{
		if(!actor.activityPubID.equals(object.activityPubID))
			throw new BadRequestException("Groups can only update themselves");
		GroupStorage.putOrUpdateForeignGroup(object);
	}
}
