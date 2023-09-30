package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;

import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Remove;
import smithereen.model.ForeignUser;
import smithereen.model.Group;
import smithereen.exceptions.BadRequestException;

public class RemoveGroupHandler extends ActivityTypeHandler<ForeignUser, Remove, Group>{
	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Remove activity, Group object) throws SQLException{
		if(activity.target==null || activity.target.link==null)
			throw new BadRequestException("activity.target is required and must be a URI");
		URI target=activity.target.link;
		if(target.equals(actor.getGroupsURL())){
			context.appContext.getGroupsController().leaveGroup(object, actor);
		}else{
			LOG.warn("Unknown Remove{Group} target {}", target);
		}
	}
}
