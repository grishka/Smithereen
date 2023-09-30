package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.exceptions.BadRequestException;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Update;
import smithereen.model.ForeignUser;
import smithereen.storage.UserStorage;

public class UpdatePersonHandler extends ActivityTypeHandler<ForeignUser, Update, ForeignUser>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Update activity, ForeignUser object) throws SQLException{
		if(!actor.activityPubID.equals(object.activityPubID))
			throw new BadRequestException("Users can only update themselves");
		UserStorage.putOrUpdateForeignUser(object);
	}
}
