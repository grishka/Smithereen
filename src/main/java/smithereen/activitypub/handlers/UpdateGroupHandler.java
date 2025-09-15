package smithereen.activitypub.handlers;

import java.sql.SQLException;

import smithereen.exceptions.BadRequestException;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Update;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.model.ForeignGroup;
import smithereen.model.User;
import smithereen.model.groups.GroupAdmin;
import smithereen.storage.GroupStorage;

public class UpdateGroupHandler extends ActivityTypeHandler<ForeignGroup, Update, ForeignGroup>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignGroup actor, Update activity, ForeignGroup object) throws SQLException{
		if(!actor.activityPubID.equals(object.activityPubID))
			throw new BadRequestException("Groups can only update themselves");
		for(GroupAdmin adm:object.adminsForActivityPub){
			try{
				adm.userID=context.appContext.getObjectLinkResolver().resolve(adm.activityPubUserID, User.class, true, true, false).id;
			}catch(ObjectNotFoundException ignore){}
		}
		object.adminsForActivityPub.removeIf(adm->adm.userID==0);
		GroupStorage.putOrUpdateForeignGroup(object);
	}
}
