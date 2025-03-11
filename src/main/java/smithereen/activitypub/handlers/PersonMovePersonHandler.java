package smithereen.activitypub.handlers;

import java.net.URI;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;

import smithereen.ApplicationContext;
import smithereen.activitypub.ActivityHandlerContext;
import smithereen.activitypub.ActivityTypeHandler;
import smithereen.activitypub.objects.activities.Move;
import smithereen.exceptions.BadRequestException;
import smithereen.exceptions.ObjectNotFoundException;
import smithereen.exceptions.UserErrorException;
import smithereen.model.ForeignUser;
import smithereen.model.User;
import smithereen.model.friends.FollowRelationship;
import smithereen.storage.UserStorage;
import smithereen.util.BackgroundTaskRunner;

public class PersonMovePersonHandler extends ActivityTypeHandler<ForeignUser, Move, ForeignUser>{

	@Override
	public void handle(ActivityHandlerContext context, ForeignUser actor, Move activity, ForeignUser object) throws SQLException{
		if(actor.id!=object.id)
			throw new BadRequestException("Actor and object IDs don't match");
		if(activity.target==null || activity.target.link==null)
			throw new BadRequestException("Move{Person} must have a `target` pointing to the new account");
		URI target=activity.target.link;

		User newUser;
		try{
			newUser=context.appContext.getObjectLinkResolver().resolve(target, User.class, true, true, true);
		}catch(ObjectNotFoundException x){
			throw new BadRequestException("Failed to fetch the target account from "+target, x);
		}
		if(!newUser.alsoKnownAs.contains(actor.activityPubID))
			throw new BadRequestException("New actor does not contain old actor's ID in `alsoKnownAs`");

		if(actor.movedAt!=null){
			LOG.debug("Not moving {} to {} because this user has already moved accounts", actor.activityPubID, target);
			return;
		}

		context.appContext.getUsersController().transferUserFollowers(actor, newUser);
	}
}
